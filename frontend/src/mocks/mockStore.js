// ---------------------------------------------------------------------------
// Milestone 2 — Mock Data Layer
// ---------------------------------------------------------------------------
// WHY THIS FILE EXISTS
// The Milestone 2 backend (Task entity, Kafka events, Blocker tracking) is
// being built by teammates in parallel. Per the project's master prompt, the
// frontend must NOT call those APIs yet. Instead we generate mock objects
// that are shaped EXACTLY like the real backend contract so that swapping
// `services/taskService.js` (etc.) for a real axios call later is a one-line
// change — no component will need to change.
//
// Real backend shape (confirmed from Backend/.../models/Task.java + TaskRequest.java):
//   Task response : { id, title, points, status, assigneeId }
//   TaskRequest   : { title, points, sprintId, assigneeId, status }
// Note: `sprint` is @JsonIgnore on the backend, so a real Task response will
// NOT include sprintId. We keep tasks scoped by sprintId only on the client
// (the same way the real GET /api/tasks/sprint/{sprintId} endpoint scopes them).
//
// Blocker + Notification entities do not exist in the backend yet (Milestone 2
// validation checklist calls for "Blocker management" but no DTO has been
// published for it). The shapes below are a reasonable forward guess and are
// clearly marked — flag these to the backend team before wiring the real API.
// ---------------------------------------------------------------------------

const STATUSES = ['TODO', 'IN_PROGRESS', 'DONE']

// Deterministic pseudo-random generator so the same sprint always renders the
// same mock board (nice for demos), without needing a real persistence layer.
function seededRandom(seed) {
  let s = seed % 2147483647
  if (s <= 0) s += 2147483646
  return () => {
    s = (s * 16807) % 2147483647
    return (s - 1) / 2147483646
  }
}

function hashSeed(value) {
  const str = String(value)
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash) || 1
}

const TASK_TITLES = [
  'Design database schema', 'Implement REST endpoint', 'Fix CORS issue',
  'Write unit tests', 'Set up CI pipeline', 'Review pull request',
  'Refactor auth middleware', 'Add validation rules', 'Optimize query performance',
  'Update API documentation', 'Investigate flaky test', 'Build UI component',
  'Wire up Kafka consumer', 'Add error boundary', 'Migrate legacy endpoint',
  'Improve loading states', 'Add pagination', 'Set up monitoring alert',
  'Patch security vulnerability', 'Sync with design team'
]

// taskStore: sprintId -> Task[]
const taskStore = new Map()
// blockerStore: sprintId -> Blocker[]
const blockerStore = new Map()
let nextTaskId = 5000
let nextBlockerId = 9000

function generateTasksForSprint(sprintId) {
  const rng = seededRandom(hashSeed(sprintId))
  const count = 6 + Math.floor(rng() * 6) // 6-11 tasks
  const tasks = []
  for (let i = 0; i < count; i++) {
    const statusRoll = rng()
    const status = statusRoll < 0.35 ? 'DONE' : statusRoll < 0.65 ? 'IN_PROGRESS' : 'TODO'
    tasks.push({
      id: hashSeed(`${sprintId}-${i}`) % 100000,
      title: TASK_TITLES[Math.floor(rng() * TASK_TITLES.length)],
      points: [1, 2, 3, 5, 8][Math.floor(rng() * 5)],
      status,
      assigneeId: null // resolved against real users at render time by caller
    })
  }
  return tasks
}

function generateBlockersForSprint(sprintId, tasks) {
  const rng = seededRandom(hashSeed(`blockers-${sprintId}`))
  const blockedCandidates = tasks.filter((t) => t.status !== 'DONE')
  const blockerCount = Math.min(blockedCandidates.length, Math.floor(rng() * 2))
  const reasons = [
    'Waiting on backend API contract',
    'Blocked by third-party dependency',
    'Needs design sign-off',
    'Environment/config issue',
    'Waiting on code review'
  ]
  const blockers = []
  for (let i = 0; i < blockerCount; i++) {
    const task = blockedCandidates[i]
    blockers.push({
      id: nextBlockerId++,
      taskId: task.id,
      taskTitle: task.title,
      reason: reasons[Math.floor(rng() * reasons.length)],
      resolved: false,
      raisedAt: new Date(Date.now() - Math.floor(rng() * 5) * 86400000).toISOString()
    })
  }
  return blockers
}

export function getTasksForSprint(sprintId) {
  const key = String(sprintId)
  if (!taskStore.has(key)) {
    const tasks = generateTasksForSprint(key)
    taskStore.set(key, tasks)
    blockerStore.set(key, generateBlockersForSprint(key, tasks))
  }
  return taskStore.get(key)
}

export function getBlockersForSprint(sprintId) {
  getTasksForSprint(sprintId) // ensure seeded
  return blockerStore.get(String(sprintId))
}

export function createTask(sprintId, { title, points, assigneeId, status }) {
  const key = String(sprintId)
  const tasks = getTasksForSprint(key)
  const task = { id: nextTaskId++, title, points: Number(points) || 1, status: status || 'TODO', assigneeId: assigneeId || null }
  tasks.unshift(task)
  return task
}

export function updateTaskStatus(sprintId, taskId, status) {
  const tasks = getTasksForSprint(sprintId)
  const task = tasks.find((t) => t.id === taskId)
  if (task) task.status = status
  return task
}

export function assignUserToTask(sprintId, taskId, assigneeId) {
  const tasks = getTasksForSprint(sprintId)
  const task = tasks.find((t) => t.id === taskId)
  if (task) task.assigneeId = assigneeId
  return task
}

export function createBlocker(sprintId, { taskId, taskTitle, reason }) {
  const key = String(sprintId)
  getTasksForSprint(key)
  const blockers = blockerStore.get(key)
  const blocker = { id: nextBlockerId++, taskId, taskTitle, reason, resolved: false, raisedAt: new Date().toISOString() }
  blockers.unshift(blocker)
  return blocker
}

export function resolveBlocker(sprintId, blockerId) {
  const blockers = blockerStore.get(String(sprintId))
  const blocker = blockers?.find((b) => b.id === blockerId)
  if (blocker) blocker.resolved = true
  return blocker
}

export const TASK_STATUSES = STATUSES
