// observabilityService.js — [M4][Jashanpreet]
// GET /api/observability/live -> LiveHealthDTO, sourced live from
// Prometheus at request time (not the DB-cached KPIs from ReleaseService).
// GET /api/observability/logs -> recent log lines for a project, sourced
// live from Elasticsearch (the same store Kibana reads from).
import client from '../api/client'

export const observabilityService = {
  getLive: (projectId) =>
    client.get('/observability/live', { params: { projectId } }).then((r) => r.data),
  getRecentLogs: (projectId, limit = 20) =>
    client.get('/observability/logs', { params: { projectId, limit } }).then((r) => r.data)
}
