// RecentLogsPanel.jsx — [M4][Jashanpreet]
// Shows the most recent log lines for this project, pulled through the
// backend's /api/observability/logs endpoint (which queries Elasticsearch
// directly — the same store Kibana reads from). Nothing here talks to
// Kibana or Elasticsearch from the browser.
import { useState, useEffect, useCallback } from 'react'
import { ExternalLink, RefreshCw } from 'lucide-react'
import { observabilityService } from '../../services/observabilityService'
import { EmptyState } from '../ui'
import { formatIST } from '../pipeline/pipelineConstants'

const KIBANA_URL = import.meta.env.VITE_KIBANA_URL || 'http://localhost:5601'

// Deep link only — not embedded. Kibana's Discover doesn't have a
// one-click embed panel the way Grafana does, so this just opens Discover
// pre-filtered to the project in a new tab, for anyone who wants to dig
// further than the last handful of lines shown here.
function kibanaDiscoverUrl(projectId) {
  const query = encodeURIComponent(`projectId:"${projectId}"`)
  return `${KIBANA_URL}/app/discover#/?_a=(query:(language:kuery,query:'${query}'))`
}

const LEVEL_COLORS = {
  ERROR: 'var(--danger)',
  WARN: 'var(--warning)',
  INFO: 'var(--ink-faint)',
  DEBUG: 'var(--ink-faint)'
}

export default function RecentLogsPanel({ projectId }) {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const poll = useCallback(async () => {
    if (!projectId) return
    try {
      const data = await observabilityService.getRecentLogs(projectId, 20)
      setLogs(data)
      setError(false)
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [projectId])

  useEffect(() => {
    poll()
    const interval = setInterval(poll, 20000)
    return () => clearInterval(interval)
  }, [poll])

  return (
    <div className="panel">
      <div className="panel-header">
        <h2>Recent Logs</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button className="btn-ghost" onClick={poll} title="Refresh now">
            <RefreshCw size={14} />
          </button>
          {projectId && (
            <a
              href={kibanaDiscoverUrl(projectId)}
              target="_blank"
              rel="noreferrer"
              className="btn-ghost"
              style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
            >
              <ExternalLink size={14} /> Open full trail in Kibana
            </a>
          )}
        </div>
      </div>

      {loading ? (
        <div className="log-list-loading">Loading…</div>
      ) : error ? (
        <EmptyState title="Couldn't reach the log store" subtitle="It may still be starting up." />
      ) : logs.length === 0 ? (
        <EmptyState
          title="No log lines for this project yet"
          subtitle="Try cutting a release or waiting for the next scheduled check."
        />
      ) : (
        <div className="log-list">
          {logs.map((line, i) => (
            <div className="log-line" key={i}>
              <span className="log-line-time">{formatIST(line.timestamp)}</span>
              <span
                className="log-line-level"
                style={{ color: LEVEL_COLORS[line.level] || 'var(--ink-faint)' }}
              >
                {line.level}
              </span>
              <span className="log-line-message">{line.message}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
