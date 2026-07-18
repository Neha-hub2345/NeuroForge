import { useEffect, useState } from 'react'
import { Plus, AlertTriangle, User } from 'lucide-react'
import { useProjectSprints } from '../hooks/useProjectSprints'
import { usersApi } from '../api/users'
import { taskService } from '../services/taskService'
import { blockerService } from '../services/blockerService'
import SprintSelector from '../components/SprintSelector'
import { Alert, Modal, EmptyState } from '../components/ui'

const COLUMNS = [
  { key: 'TODO', label: 'To Do' },
  { key: 'IN_PROGRESS', label: 'In Progress' },
  { key: 'DONE', label: 'Done' }
]

export default function KanbanBoard() {
  const {
    projects, sprints, projectId, setProjectId, sprintId, setSprintId,
    selectedSprint, loadingSprints, error: pickerError
  } = useProjectSprints()

  const [tasks, setTasks] = useState([])
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [blockingTask, setBlockingTask] = useState(null)
  const [dragTaskId, setDragTaskId] = useState(null)

  useEffect(() => {
    usersApi.getAll().then(setUsers).catch(() => {})
  }, [])

  useEffect(() => {
    if (!sprintId) {
      setTasks([])
      return
    }
    let mounted = true
    setLoading(true)
    taskService
      .getTasksForSprint(sprintId)
      .then((data) => mounted && setTasks(data))
      .catch((err) => mounted && setError(err.message))
      .finally(() => mounted && setLoading(false))
    return () => {
      mounted = false
    }
  }, [sprintId])

  const userName = (id) => users.find((u) => u.id === id)?.username

  const moveTask = async (taskId, status) => {
    const updated = await taskService.updateStatus(sprintId, taskId, status)
    setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)))
  }

  const handleDrop = (status) => (e) => {
    e.preventDefault()
    if (dragTaskId != null) moveTask(dragTaskId, status)
    setDragTaskId(null)
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Kanban Board</h1>
          <p className="page-subtitle">
            Task management for the active sprint · <span className="mock-pill">Mock data — Milestone 2</span>
          </p>
        </div>
        <button className="btn-primary" onClick={() => setShowCreate(true)} disabled={!sprintId}>
          <Plus size={16} /> New Task
        </button>
      </div>

      <Alert onClose={() => setError('')}>{error || pickerError}</Alert>

      <div className="panel panel-tight">
        <SprintSelector
          projects={projects} projectId={projectId} setProjectId={setProjectId}
          sprints={sprints} sprintId={sprintId} setSprintId={setSprintId} loadingSprints={loadingSprints}
        />
      </div>

      {!sprintId ? (
        <EmptyState title="No sprint selected" subtitle="Create a project and sprint in Milestone 1 first." />
      ) : (
        <div className="kanban-board">
          {COLUMNS.map((col) => {
            const colTasks = tasks.filter((t) => t.status === col.key)
            return (
              <div
                key={col.key}
                className="kanban-column"
                onDragOver={(e) => e.preventDefault()}
                onDrop={handleDrop(col.key)}
              >
                <div className="kanban-column-header">
                  <span>{col.label}</span>
                  <span className="kanban-count">{colTasks.length}</span>
                </div>
                <div className="kanban-column-body">
                  {loading && <div className="empty-sub">Loading…</div>}
                  {!loading && colTasks.length === 0 && <div className="kanban-empty">Drop tasks here</div>}
                  {colTasks.map((task) => (
                    <div
                      key={task.id}
                      className="kanban-card"
                      draggable
                      onDragStart={() => setDragTaskId(task.id)}
                    >
                      <div className="kanban-card-title">{task.title}</div>
                      <div className="kanban-card-meta">
                        <span className="points-badge">{task.points} pts</span>
                        {task.assigneeId && (
                          <span className="assignee-chip">
                            <User size={12} /> {userName(task.assigneeId) || `#${task.assigneeId}`}
                          </span>
                        )}
                      </div>
                      <div className="kanban-card-actions">
                        <select
                          className="inline-select inline-select-sm"
                          value={task.status}
                          onChange={(e) => moveTask(task.id, e.target.value)}
                        >
                          {COLUMNS.map((c) => (
                            <option key={c.key} value={c.key}>{c.label}</option>
                          ))}
                        </select>
                        <button className="btn-ghost-sm" onClick={() => setBlockingTask(task)} title="Flag as blocked">
                          <AlertTriangle size={14} />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {showCreate && (
        <CreateTaskModal
          users={users}
          onClose={() => setShowCreate(false)}
          onCreated={(task) => {
            setTasks((prev) => [task, ...prev])
            setShowCreate(false)
          }}
          sprintId={sprintId}
        />
      )}

      {blockingTask && (
        <FlagBlockerModal
          task={blockingTask}
          sprintId={sprintId}
          onClose={() => setBlockingTask(null)}
          onFlagged={() => setBlockingTask(null)}
        />
      )}
    </div>
  )
}

function CreateTaskModal({ users, onClose, onCreated, sprintId }) {
  const [title, setTitle] = useState('')
  const [points, setPoints] = useState(3)
  const [assigneeId, setAssigneeId] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const task = await taskService.createTask(sprintId, {
        title: title.trim(),
        points: Number(points),
        assigneeId: assigneeId ? Number(assigneeId) : null,
        status: 'TODO'
      })
      onCreated(task)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal title="New task" onClose={onClose}>
      <Alert onClose={() => setError('')}>{error}</Alert>
      <form onSubmit={handleSubmit} className="modal-form">
        <label className="field">
          <span>Title</span>
          <input value={title} onChange={(e) => setTitle(e.target.value)} required autoFocus />
        </label>
        <label className="field">
          <span>Story points</span>
          <select value={points} onChange={(e) => setPoints(e.target.value)}>
            {[1, 2, 3, 5, 8, 13].map((p) => (
              <option key={p} value={p}>{p}</option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Assignee</span>
          <select value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)}>
            <option value="">Unassigned</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>{u.username}</option>
            ))}
          </select>
        </label>
        <button className="btn-primary btn-block" type="submit" disabled={submitting}>
          {submitting ? 'Creating…' : 'Create task'}
        </button>
      </form>
    </Modal>
  )
}

function FlagBlockerModal({ task, sprintId, onClose, onFlagged }) {
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      await blockerService.raiseBlocker(sprintId, { taskId: task.id, taskTitle: task.title, reason: reason.trim() })
      onFlagged()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal title={`Flag "${task.title}" as blocked`} onClose={onClose}>
      <Alert onClose={() => setError('')}>{error}</Alert>
      <form onSubmit={handleSubmit} className="modal-form">
        <label className="field">
          <span>Why is it blocked?</span>
          <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={3} required autoFocus />
        </label>
        <button className="btn-primary btn-block" type="submit" disabled={submitting}>
          {submitting ? 'Flagging…' : 'Flag as blocked'}
        </button>
      </form>
    </Modal>
  )
}
