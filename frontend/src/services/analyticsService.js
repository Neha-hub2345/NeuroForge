// ---------------------------------------------------------------------------
// AnalyticsService — derives chart-ready data from the Milestone 2 mock store
// ---------------------------------------------------------------------------
// Velocity is computed for real from the mock tasks generated per sprint
// (sum of story points on DONE tasks). Burndown needs day-by-day progress,
// which the mock layer can't know (no timestamps on task completion yet), so
// the daily curve is a smoothed simulation anchored to today's real totals —
// clearly cosmetic, and swappable once the backend tracks status-change
// timestamps (Kafka event log would be the natural source for this later).
// ---------------------------------------------------------------------------
import { taskService } from './taskService'

export const analyticsService = {
  async getVelocity(sprints) {
    const results = []
    for (const sprint of sprints) {
      const tasks = await taskService.getTasksForSprint(sprint.id)
      const committed = tasks.reduce((sum, t) => sum + t.points, 0)
      const completed = tasks.filter((t) => t.status === 'DONE').reduce((sum, t) => sum + t.points, 0)
      results.push({ sprint: sprint.goal || `Sprint ${sprint.id}`, committed, completed })
    }
    return results
  },

  async getBurndown(sprint) {
    const tasks = await taskService.getTasksForSprint(sprint.id)
    const totalPoints = tasks.reduce((sum, t) => sum + t.points, 0)
    const remainingNow = tasks.filter((t) => t.status !== 'DONE').reduce((sum, t) => sum + t.points, 0)
    const days = 10
    const completedFraction = totalPoints === 0 ? 0 : 1 - remainingNow / totalPoints
    const dayIndex = Math.max(1, Math.round(completedFraction * days))

    const series = []
    for (let d = 0; d <= days; d++) {
      const ideal = Math.round(totalPoints - (totalPoints / days) * d)
      let actual = null
      if (d <= dayIndex) {
        const progress = dayIndex === 0 ? 0 : d / dayIndex
        actual = Math.round(totalPoints - (totalPoints - remainingNow) * progress)
      }
      series.push({ day: `Day ${d}`, ideal, actual })
    }
    return { series, totalPoints, remainingNow }
  }
}
