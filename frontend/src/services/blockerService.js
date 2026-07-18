// ---------------------------------------------------------------------------
// BlockerService — MOCK IMPLEMENTATION (Milestone 2)
// ---------------------------------------------------------------------------
// FLAG FOR BACKEND TEAM: the Milestone 2 brief requires "Blocker management"
// but no Blocker entity/DTO has been published yet (it's absent from both
// the API reference PDF and the Task model). The shape below is a proposed
// contract: { id, taskId, taskTitle, reason, resolved, raisedAt }.
// Confirm/adjust field names with the backend before wiring the real API —
// only this file and mocks/mockStore.js would need to change.
// ---------------------------------------------------------------------------
import * as store from '../mocks/mockStore'

const LATENCY = 200
function delay(value) {
  return new Promise((resolve) => setTimeout(() => resolve(value), LATENCY))
}

export const blockerService = {
  getBlockersForSprint: (sprintId) => delay(store.getBlockersForSprint(sprintId)),
  raiseBlocker: (sprintId, payload) => delay(store.createBlocker(sprintId, payload)),
  resolveBlocker: (sprintId, blockerId) => delay(store.resolveBlocker(sprintId, blockerId))
}
