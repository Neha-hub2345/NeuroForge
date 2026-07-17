import { useEffect, useState } from 'react'
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { useProjectSprints } from '../hooks/useProjectSprints'
import { analyticsService } from '../services/analyticsService'
import SprintSelector from '../components/SprintSelector'
import { Alert, EmptyState } from '../components/ui'

export default function SprintProgress() {
  const {
    projects, sprints, projectId, setProjectId, sprintId, setSprintId,
    selectedSprint, loadingSprints, error: pickerError
  } = useProjectSprints()

  const [burndown, setBurndown] = useState(null)
  const [velocity, setVelocity] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!selectedSprint) {
      setBurndown(null)
      return
    }
    setLoading(true)
    analyticsService
      .getBurndown(selectedSprint)
      .then(setBurndown)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [selectedSprint])

  useEffect(() => {
    if (!sprints || sprints.length === 0) {
      setVelocity([])
      return
    }
    analyticsService.getVelocity(sprints).then(setVelocity).catch((err) => setError(err.message))
  }, [sprints])

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Sprint Progress</h1>
          <p className="page-subtitle">
            Burndown &amp; velocity tracking · <span className="mock-pill">Mock data — Milestone 2</span>
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

      {!selectedSprint ? (
        <EmptyState title="No sprint selected" subtitle="Create a project and sprint in Milestone 1 first." />
      ) : (
        <>
          <div className="panel">
            <div className="panel-header">
              <h2>Burndown — {selectedSprint.goal}</h2>
              {burndown && (
                <span className="page-subtitle-inline">
                  {burndown.remainingNow} / {burndown.totalPoints} points remaining
                </span>
              )}
            </div>
            {loading || !burndown ? (
              <div className="empty-sub">Loading…</div>
            ) : (
              <div className="chart-wrap">
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={burndown.series} margin={{ top: 8, right: 24, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--chart-grid)" />
                    <XAxis dataKey="day" stroke="var(--chart-axis)" fontSize={12} />
                    <YAxis stroke="var(--chart-axis)" fontSize={12} />
                    <Tooltip contentStyle={{ background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 8 }} />
                    <Legend />
                    <Line type="monotone" dataKey="ideal" name="Ideal" stroke="var(--ink-soft)" strokeDasharray="5 5" dot={false} />
                    <Line type="monotone" dataKey="actual" name="Actual" stroke="var(--accent)" strokeWidth={2.5} dot />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>

          <div className="panel">
            <div className="panel-header">
              <h2>Velocity by sprint</h2>
            </div>
            {velocity.length === 0 ? (
              <EmptyState title="No sprint history yet" />
            ) : (
              <div className="chart-wrap">
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={velocity} margin={{ top: 8, right: 24, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--chart-grid)" />
                    <XAxis dataKey="sprint" stroke="var(--chart-axis)" fontSize={12} />
                    <YAxis stroke="var(--chart-axis)" fontSize={12} />
                    <Tooltip contentStyle={{ background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 8 }} />
                    <Legend />
                    <Bar dataKey="committed" name="Committed" fill="var(--accent-soft)" radius={[6, 6, 0, 0]} />
                    <Bar dataKey="completed" name="Completed" fill="var(--accent)" radius={[6, 6, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}
