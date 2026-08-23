import { useEffect, useRef } from 'react'
import { Loader2, ExternalLink, GitBranch, GitCommitHorizontal } from 'lucide-react'

// Shown while a build we triggered is still running. Tails the real
// GitHub Actions job log (polled server-side via /api/pipelines/live) so
// the user can watch the build stage as it happens, instead of waiting in
// the dark until the finish webhook lands. Once the run completes, the
// dashboard swaps back to the normal builds table automatically.
export default function LiveBuildLogsPanel({ status, waiting }) {
  const logRef = useRef(null)

  useEffect(() => {
    if (logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight
    }
  }, [status?.logs])

  const headline = waiting
    ? 'Waiting for GitHub Actions to pick up the build…'
    : (status?.currentStepName || 'Build running')

  return (
    <div className="panel live-log-panel">
      <div className="live-log-header">
        <div className="live-log-title">
          <Loader2 size={16} className="live-log-spinner" />
          <span>{headline}</span>
        </div>
        <div className="live-log-meta">
          {status?.branch && (
            <span className="live-log-meta-item"><GitBranch size={12} /> {status.branch}</span>
          )}
          {status?.commitHash && (
            <span className="live-log-meta-item pipeline-commit-inner">
              <GitCommitHorizontal size={12} /> {status.commitHash.substring(0, 7)}
            </span>
          )}
          {status?.htmlUrl && (
            <a href={status.htmlUrl} target="_blank" rel="noreferrer" className="live-log-link">
              <ExternalLink size={12} /> View on GitHub
            </a>
          )}
        </div>
      </div>

      <div className="live-log-body" ref={logRef}>
        {status?.logs ? (
          <pre className="live-log-pre">
            {status.truncated ? `… showing the most recent output …\n\n${status.logs}` : status.logs}
          </pre>
        ) : (
          <p className="live-log-empty">
            {waiting
              ? 'Build dispatched — logs will start streaming once GitHub Actions starts the job.'
              : 'No output yet.'}
          </p>
        )}
      </div>
    </div>
  )
}
