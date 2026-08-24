// ---------------------------------------------------------------------------
// Mock data for the DevOps Observability dashboard (Milestone 4).
// Per the Milestone 4 brief: "start by building this with mock JSON metrics
// and logs while the backend monitoring APIs are being finalized."
// Once Jashanpreet's real Actuator/metrics endpoints are ready, this file's
// shape is what a real observabilityService.js should return — swapping the
// data source shouldn't require changing the dashboard components.
// ---------------------------------------------------------------------------

export const kpiSummary = {
  uptimePercent: 99.99,
  mttrMinutes: 12,
  releasesPerMonth: 47
}

// Service health — one row per microservice
export const serviceHealth = [
  { name: 'Backend API', status: 'healthy', cpu: 34, memory: 61 },
  { name: 'Postgres', status: 'healthy', cpu: 22, memory: 48 },
  { name: 'Keycloak', status: 'healthy', cpu: 18, memory: 39 },
  { name: 'Kafka', status: 'degraded', cpu: 71, memory: 82 },
  { name: 'Elasticsearch', status: 'healthy', cpu: 45, memory: 67 }
]

// CPU/Memory usage over the last 12 sampling points (e.g. last hour, 5 min apart)
export const usageOverTime = [
  { time: '10:00', cpu: 28, memory: 52 },
  { time: '10:05', cpu: 31, memory: 54 },
  { time: '10:10', cpu: 35, memory: 55 },
  { time: '10:15', cpu: 33, memory: 58 },
  { time: '10:20', cpu: 40, memory: 60 },
  { time: '10:25', cpu: 46, memory: 63 },
  { time: '10:30', cpu: 44, memory: 65 },
  { time: '10:35', cpu: 38, memory: 62 },
  { time: '10:40', cpu: 41, memory: 64 },
  { time: '10:45', cpu: 37, memory: 61 },
  { time: '10:50', cpu: 34, memory: 59 },
  { time: '10:55', cpu: 36, memory: 60 }
]

// Recent releases — matches the Release entity shape from the M4 doc
// (relId, version, date) plus a status field for the UI
export const recentReleases = [
  { relId: 'a1f2c3', version: 'v2.4.1', date: '2026-08-15T09:12:00Z', status: 'success' },
  { relId: 'b7e9d4', version: 'v2.4.0', date: '2026-08-13T14:30:00Z', status: 'success' },
  { relId: 'c3a8f1', version: 'v2.3.9', date: '2026-08-11T11:05:00Z', status: 'rolled-back' },
  { relId: 'd5b6e2', version: 'v2.3.8', date: '2026-08-09T16:47:00Z', status: 'success' },
  { relId: 'e9c1a7', version: 'v2.3.7', date: '2026-08-07T08:20:00Z', status: 'success' }
]