// PrometheusStatusBadge.jsx
// Small status indicator only — NOT a second copy of the KPI numbers.
// Polls the same backend-proxied endpoint LiveHealthWidget used
// (GET /api/observability/live, backend talks to Prometheus internally —
// the browser never calls Prometheus/Grafana directly), but only surfaces
// the connection status + last-checked time. The actual KPI values live
// in ReleaseKpiStats so we don't show "Uptime 99% / Uptime 98.2%" twice.
import { useState, useEffect, useCallback } from "react";
import { observabilityService } from "../../services/observabilityService";

export default function PrometheusStatusBadge({ projectId }) {
  const [live, setLive] = useState(null);
  const [error, setError] = useState(false);

  const poll = useCallback(async () => {
    if (!projectId) return;
    try {
      const data = await observabilityService.getLive(projectId);
      setLive(data);
      setError(false);
    } catch {
      setError(true);
    }
  }, [projectId]);

  useEffect(() => {
    poll();
    const interval = setInterval(poll, 15000);
    return () => clearInterval(interval);
  }, [poll]);

  if (!live && !error) return null;

  const up = live?.scrapeUp;

  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        fontSize: 12,
        color: "var(--ink-faint)",
      }}
      title="Backend-proxied Prometheus scrape status (GET /api/observability/live)"
    >
      <span
        style={{
          width: 7,
          height: 7,
          borderRadius: "50%",
          display: "inline-block",
          background: up ? "var(--success)" : "var(--danger)",
        }}
      />
      {up
        ? `Prometheus connected · checked ${new Date(live.asOfEpochMs).toLocaleTimeString()}`
        : "Not connected to Prometheus"}
    </span>
  );
}
