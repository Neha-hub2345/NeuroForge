import { useEffect, useState } from 'react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { useProjectSprints } from '../hooks/useProjectSprints'
import { taskService } from '../services/taskService'
import { blockerService } from '../services/blockerService'
import { analyticsService } from '../services/analyticsService'
import SprintSelector from '../components/SprintSelector'
import { Alert, EmptyState } from '../components/ui'

export default function Analytics() {
  const {
    projects, sprints, projectId, setProjectId, sprintId, setSprintId,
    loadingSprints, error: pickerError
  } = useProjectSprints()

  const [stats, setStats] = useState(null)
  const [velocity, setVelocity] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!sprints || sprints.length === 0) {
      setStats(null)
      setVelocity([])
      return
    }
    let mounted = true
    setLoading(true)

    const run = async () => {
      let totalTasks = 0, totalPoints = 0, completedPoints = 0, openBlockers = 0
      for (const sprint of sprints) {
        const tasks = await taskService.getTasksForSprint(sprint.id)
        const blockers = await blockerService.getBlockersForSprint(sprint.id)
        totalTasks += tasks.length
        totalPoints += tasks.reduce((s, t) => s + t.points, 0)
        completedPoints += tasks.filter((t) => t.status === 'DONE').reduce((s, t) => s + t.points, 0)
        openBlockers += blockers.filter((b) => !b.resolved).length
      }
      if (!mounted) return
      setStats({
        totalTasks,
        totalPoints,
        completedPoints,
        completionRate: totalPoints === 0 ? 0 : Math.round((completedPoints / totalPoints) * 100),
        openBlockers,
        sprintCount: sprints.length
      })
      const v = await analyticsService.getVelocity(sprints)
      if (mounted) setVelocity(v)
    }

    run().catch((err) => mounted && setError(err.message)).finally(() => mounted && setLoading(false))
    return () => { mounted = false }
  }, [sprints])

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Analytics</h1>
          <p className="page-subtitle">
            Cross-sprint overview · <span className="mock-pill">Mock data — Milestone 2</span>
          </p>
        </div>
      </div>

      <Alert onClose={() => setError('')}>{error || pickerError}</Alert>

      <div className="panel panel-tight">
        <SprintSelector
          projects={projects} projectId={projectId} setProjectId={setProjectId}
          sprints={sprints} sprintId={sprintId} setSprintId={setSprintId} loadingSprints={loadingSprints}
        />
      </div>

      {loading || !stats ? (
        <EmptyState title={loading ? 'Crunching numbers…' : 'No sprint data yet'} />
      ) : (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-label">Sprints tracked</div>
              <div className="stat-value">{stats.sprintCount}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Total tasks</div>
              <div className="stat-value">{stats.totalTasks}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Completion rate</div>
              <div className="stat-value">{stats.completionRate}%</div>
              <div className="stat-foot">{stats.completedPoints} / {stats.totalPoints} pts</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Open blockers</div>
              <div className="stat-value">{stats.openBlockers}</div>
            </div>
          </div>

          <div className="panel">
            <div className="panel-header">
              <h2>Points committed vs completed</h2>
            </div>
            <div className="chart-wrap">
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={velocity} margin={{ top: 8, right: 24, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--chart-grid)" />
                  <XAxis dataKey="sprint" stroke="var(--chart-axis)" fontSize={12} />
                  <YAxis stroke="var(--chart-axis)" fontSize={12} />
                  <Tooltip contentStyle={{ background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 8 }} />
                  <Bar dataKey="committed" name="Committed" fill="var(--accent-soft)" radius={[6, 6, 0, 0]} />
                  <Bar dataKey="completed" name="Completed" fill="var(--accent)" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
