import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { kpiSummary, serviceHealth, usageOverTime, recentReleases } from '../data/observabilityMockData'

// -----------------------------------------------------------------------
// DevOps Observability dashboard (Milestone 4).
// Built against mock JSON data per the M4 brief — swap `data/observabilityMockData.js`
// for a real `services/observabilityService.js` once Jashanpreet's Actuator /
// Prometheus-backed endpoints are ready. Component structure below should not
// need to change, only the data source.
// -----------------------------------------------------------------------

export default function Observability() {
  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>DevOps Observability</h1>
          <p className="page-sub">Real-time service health, resource usage, and release KPIs.</p>
        </div>
      </div>

      {/* KPI cards */}
      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-label">Uptime</div>
          <div className="stat-value stat-value-success">{kpiSummary.uptimePercent}%</div>
          <div className="stat-foot">last 30 days</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">MTTR</div>
          <div className="stat-value">{kpiSummary.mttrMinutes}<span className="stat-value-unit">min</span></div>
          <div className="stat-foot">mean time to recovery</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Releases</div>
          <div className="stat-value">{kpiSummary.releasesPerMonth}</div>
          <div className="stat-foot">per month</div>
        </div>
      </div>

      {/* Service health table */}
      <div className="panel">
        <div className="panel-header">
          <h2>Service Health</h2>
        </div>
        <table className="table">
          <thead>
            <tr>
              <th>Service</th>
              <th>Status</th>
              <th>CPU</th>
              <th>Memory</th>
            </tr>
          </thead>
          <tbody>
            {serviceHealth.map((svc) => (
              <tr key={svc.name}>
                <td>{svc.name}</td>
                <td>
                  <span className={svc.status === 'healthy' ? 'badge badge-success' : 'badge badge-blocked'}>
                    {svc.status}
                  </span>
                </td>
                <td>{svc.cpu}%</td>
                <td>{svc.memory}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* CPU / Memory usage over time */}
      <div className="panel">
        <div className="panel-header">
          <h2>CPU &amp; Memory Usage</h2>
        </div>
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={usageOverTime} margin={{ top: 8, right: 24, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--chart-grid)" />
            <XAxis dataKey="time" stroke="var(--chart-axis)" fontSize={12} />
            <YAxis stroke="var(--chart-axis)" fontSize={12} unit="%" />
            <Tooltip contentStyle={{ background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 8 }} />
            <Legend />
            <Line type="monotone" dataKey="cpu" name="CPU" stroke="var(--chart-committed)" strokeWidth={2.5} dot={false} />
            <Line type="monotone" dataKey="memory" name="Memory" stroke="var(--chart-completed)" strokeWidth={2.5} dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Recent releases */}
      <div className="panel">
        <div className="panel-header">
          <h2>Recent Releases</h2>
        </div>
        <table className="table">
          <thead>
            <tr>
              <th>Version</th>
              <th>Date</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {recentReleases.map((r) => (
              <tr key={r.relId}>
                <td>{r.version}</td>
                <td>{new Date(r.date).toLocaleString()}</td>
                <td>
                  <span className={r.status === 'success' ? 'badge badge-success' : 'badge badge-blocked'}>
                    {r.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}