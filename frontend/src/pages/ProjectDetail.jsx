import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { projectsApi } from '../api/projects'
import { sprintsApi } from '../api/sprints'
import { milestonesApi } from '../api/milestones'
import { useAuth } from '../context/AuthContext'
import { Alert, StatusBadge, EmptyState } from '../components/ui'
import { canManage } from '../utils/roles'

export default function ProjectDetail() {
  const { id } = useParams()
  
  // 1. Destructure 'roles' instead of 'user'
  const { roles } = useAuth()
  
  const [project, setProject] = useState(null)
  const [sprints, setSprints] = useState([])
  const [milestones, setMilestones] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const [sprintForm, setSprintForm] = useState({ goal: '', dates: '' })
  const [milestoneForm, setMilestoneForm] = useState({ title: '', targetDate: '' })
  const [savingSprint, setSavingSprint] = useState(false)
  const [savingMilestone, setSavingMilestone] = useState(false)

  // 2. Pass the first role from the Keycloak array into your existing utility function
  const canEdit = canManage(roles?.[0])

  const load = async () => {
    setLoading(true)
    try {
      const [proj, sp, ms] = await Promise.all([
        projectsApi.getById(id),
        sprintsApi.getByProject(id),
        milestonesApi.getByProject(id)
      ])
      setProject(proj)
      setSprints(sp)
      setMilestones(ms)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  const handleAddSprint = async (e) => {
    e.preventDefault()
    setError('')
    setSavingSprint(true)
    try {
      const sprint = await sprintsApi.create({
        goal: sprintForm.goal.trim(),
        dates: sprintForm.dates.trim(),
        projectId: Number(id)
      })
      setSprints((prev) => [sprint, ...prev])
      setSprintForm({ goal: '', dates: '' })
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingSprint(false)
    }
  }

  const handleAddMilestone = async (e) => {
    e.preventDefault()
    setError('')
    setSavingMilestone(true)
    try {
      const milestone = await milestonesApi.create({
        title: milestoneForm.title.trim(),
        targetDate: milestoneForm.targetDate,
        projectId: Number(id)
      })
      setMilestones((prev) => [milestone, ...prev])
      setMilestoneForm({ title: '', targetDate: '' })
    } catch (err) {
      setError(err.message)
    } finally {
      setSavingMilestone(false)
    }
  }

  if (loading) return <div className="page">Loading…</div>

  if (!project) {
    return (
      <div className="page">
        <Alert>{error || 'Project not found'}</Alert>
        <Link className="link" to="/projects">
          ← Back to projects
        </Link>
      </div>
    )
  }

  return (
    <div className="page">
      <Link className="link back-link" to="/projects">
        ← Back to projects
      </Link>

      <div className="page-header">
        <div>
          <h1>{project.name}</h1>
          <p className="page-subtitle">
            <StatusBadge status={project.status} /> · Team: {project.teamName} · Manager:{' '}
            {project.managerUsername || '—'} · Created {project.createdAt}
          </p>
        </div>
      </div>

      <Alert onClose={() => setError('')}>{error}</Alert>

      <div className="two-col">
        <div className="panel">
          <div className="panel-header">
            <h2>Sprints</h2>
          </div>

          {canEdit && (
            <form onSubmit={handleAddSprint} className="inline-form">
              <input
                placeholder="Sprint goal"
                value={sprintForm.goal}
                onChange={(e) => setSprintForm((f) => ({ ...f, goal: e.target.value }))}
                required
              />
              <input
                placeholder="Dates (e.g. Jul 1 - Jul 14)"
                value={sprintForm.dates}
                onChange={(e) => setSprintForm((f) => ({ ...f, dates: e.target.value }))}
                required
              />
              <button className="btn-primary" type="submit" disabled={savingSprint}>
                {savingSprint ? 'Adding…' : 'Add sprint'}
              </button>
            </form>
          )}

          {sprints.length === 0 ? (
            <EmptyState title="No sprints yet" />
          ) : (
            <ul className="list">
              {sprints.map((s) => (
                <li key={s.id} className="list-item">
                  <div className="list-item-title">{s.goal}</div>
                  <div className="list-item-sub">{s.dates}</div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="panel">
          <div className="panel-header">
            <h2>Milestones</h2>
          </div>

          {canEdit && (
            <form onSubmit={handleAddMilestone} className="inline-form">
              <input
                placeholder="Milestone title"
                value={milestoneForm.title}
                onChange={(e) => setMilestoneForm((f) => ({ ...f, title: e.target.value }))}
                required
              />
              <input
                type="date"
                value={milestoneForm.targetDate}
                onChange={(e) => setMilestoneForm((f) => ({ ...f, targetDate: e.target.value }))}
                required
              />
              <button className="btn-primary" type="submit" disabled={savingMilestone}>
                {savingMilestone ? 'Adding…' : 'Add milestone'}
              </button>
            </form>
          )}

          {milestones.length === 0 ? (
            <EmptyState title="No milestones yet" />
          ) : (
            <ul className="list">
              {milestones.map((m) => (
                <li key={m.id} className="list-item">
                  <div className="list-item-title">{m.title}</div>
                  <div className="list-item-sub">Due {m.targetDate}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}