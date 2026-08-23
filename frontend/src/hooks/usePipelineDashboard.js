import { useEffect, useRef, useState } from 'react'
import { pipelineService } from '../services/pipelineService'

const LIVE_POLL_MS = 3000
// ~10 minutes of polling before we give up waiting on a run that never
// seems to finish (misconfigured workflow, GitHub API hiccup, etc.) —
// keeps this from polling forever if something upstream goes wrong.
const LIVE_POLL_MAX_ATTEMPTS = 200
// How often to check for a build that's running but wasn't dispatched
// from this dashboard (e.g. a plain `git push`, or someone else clicking
// Trigger). Slower than the fast poll since most of the time nothing's
// running — this is just a "is anything happening?" check.
const BACKGROUND_POLL_MS = 15000

export function usePipelineDashboard(projectId) {
  const [kpis, setKpis] = useState(null)
  const [builds, setBuilds] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [selectedBuildId, setSelectedBuildId] = useState(null)
  const [buildDetails, setBuildDetails] = useState(null)
  const [loadingDetails, setLoadingDetails] = useState(false)
  
  const [triggering, setTriggering] = useState(false)
  const [rollingBack, setRollingBack] = useState(false)

  // Live build log tracking — active from the moment a build is
  // dispatched until GitHub Actions reports the run as completed.
  const [liveStatus, setLiveStatus] = useState(null)
  const [liveTracking, setLiveTracking] = useState(false)
  const liveBaselineRunId = useRef(null)
  const livePollAttempts = useRef(0)

  // Standard function defined in the hook scope
  const loadDashboard = async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      const [k, b] = await Promise.all([
        pipelineService.getKpis(projectId),
        pipelineService.getHistory(projectId)
      ])
      setKpis(k)
      setBuilds([...b].sort((a, c) => new Date(c.startedAt) - new Date(a.startedAt)))
    } catch (err) {
      setError(err.message)
    } finally {
      if (!silent) setLoading(false)
    }
  }

  // Runs loadDashboard once when the component mounts
 useEffect(() => {
    if (projectId) loadDashboard()
  }, [projectId])

  // Fetch build details when selectedBuildId changes
  useEffect(() => {
    if (!selectedBuildId) {
      setBuildDetails(null)
      return
    }
    setLoadingDetails(true)
    pipelineService.getDetail(selectedBuildId)
      .then(setBuildDetails)
      .catch((err) => setError(err.message))
      .finally(() => setLoadingDetails(false))
  }, [selectedBuildId])

  // Lock background scroll while the modal is open
  useEffect(() => {
    if (!selectedBuildId) return
    const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth
    const prevOverflow = document.body.style.overflow
    const prevPaddingRight = document.body.style.paddingRight
    document.body.style.overflow = 'hidden'
    if (scrollbarWidth > 0) {
      document.body.style.paddingRight = `${scrollbarWidth}px`
    }
    return () => {
      document.body.style.overflow = prevOverflow
      document.body.style.paddingRight = prevPaddingRight
    }
  }, [selectedBuildId])

  // Background watcher — catches builds that started outside this
  // dashboard (a `git push`, someone else clicking Trigger, a manual
  // workflow_dispatch from GitHub itself) so the live panel isn't only
  // available for builds *we* dispatched. Polls slowly while idle so it
  // doesn't burn GitHub API quota, and hands off to the fast poller below
  // the moment it actually finds a run in progress.
  useEffect(() => {
    if (!projectId || liveTracking) return

    let cancelled = false
    const check = async () => {
      try {
        const status = await pipelineService.getLiveStatus(projectId)
        if (cancelled || !status.active) return
        // Any active run counts as "new" here — we're not tracking a
        // baseline because we didn't dispatch this one ourselves.
        liveBaselineRunId.current = null
        livePollAttempts.current = 0
        setLiveStatus(status)
        setLiveTracking(true)
      } catch {
        // No GitHub integration configured, or a transient API error —
        // this is just a background check, so fail silently.
      }
    }

    check()
    const interval = setInterval(check, BACKGROUND_POLL_MS)
    return () => {
      cancelled = true
      clearInterval(interval)
    }
  }, [projectId, liveTracking])

  // Polls /pipelines/live while liveTracking is true. A workflow_dispatch
  // trigger doesn't return a run id, and GitHub can take a few seconds to
  // register the new run — during that gap /live would still report the
  // *previous* run as "active: false". So we snapshot the previous run id
  // as a baseline before dispatching (see triggerBuild) and ignore
  // responses until a genuinely new run id shows up.
  useEffect(() => {
    if (!liveTracking || !projectId) return

    let cancelled = false

    const poll = async () => {
      livePollAttempts.current += 1
      if (livePollAttempts.current > LIVE_POLL_MAX_ATTEMPTS) {
        if (!cancelled) setLiveTracking(false)
        return
      }

      try {
        const status = await pipelineService.getLiveStatus(projectId)
        if (cancelled) return

        const isNewRun = status.runId && status.runId !== liveBaselineRunId.current
        if (!isNewRun) return // still the old run (or nothing yet) — keep waiting

        setLiveStatus(status)
        if (!status.active) {
          setLiveTracking(false)
          // The finish webhook should have landed the real Pipeline row
          // by now — refresh so it shows up in the table/KPIs.
          await loadDashboard(true)
        }
      } catch (err) {
        if (!cancelled) {
          setLiveTracking(false)
          setError(err.message)
        }
      }
    }

    poll()
    const interval = setInterval(poll, LIVE_POLL_MS)
    return () => {
      cancelled = true
      clearInterval(interval)
    }
  }, [liveTracking, projectId])

  const triggerBuild = async (projectId) => {
    setError('')
    setTriggering(true)
    try {
      // Snapshot the current latest run id so the poller above can tell a
      // genuinely new run apart from GitHub still reporting the old one.
      let baselineRunId = null
      try {
        const current = await pipelineService.getLiveStatus(projectId)
        baselineRunId = current.runId || null
      } catch {
        // No integration yet, or nothing has ever run — fine, baseline stays null.
      }

      await pipelineService.triggerBuild(projectId)
      liveBaselineRunId.current = baselineRunId
      livePollAttempts.current = 0
      setLiveStatus(null)
      setLiveTracking(true)
      return true
    } catch (err) {
      setError(err.message)
      return false
    } finally {
      setTriggering(false)
    }
  }

  const rollbackBuild = async (pipelineId) => {
    setError('')
    setRollingBack(true)
    try {
      await pipelineService.rollbackBuild(pipelineId)
      console.log('Rollback successful, refreshing dashboard...')
      setSelectedBuildId(null)
      // Calls loadDashboard silently to refresh the table/KPIs after rollback
      await loadDashboard(true)
      return true
    } catch (err) {
      setError(err.message)
      return false
    } finally {
      setRollingBack(false)
    }
  }

  return {
    kpis, builds, loading, error, setError,
    selectedBuildId, setSelectedBuildId, buildDetails, loadingDetails,
    triggering, rollingBack, triggerBuild, rollbackBuild,
    liveStatus, liveTracking
  }
}