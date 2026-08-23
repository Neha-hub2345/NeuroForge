// LiveHealthDTO.java — [M4][Jashanpreet]
// Values here come straight from a live Prometheus query at request time —
// NOT from ReleaseService's DB-computed, 60s-cached KPIs (see ReleaseKpiDTO).
// The two should roughly agree; if they drift apart it usually means the
// scrape target is stale or ProjectGaugeRegistrar hasn't ticked recently.
package com.nexus.NeuroForge.dto.observability;

public class LiveHealthDTO {
    private boolean scrapeUp;
    private Double uptimePercent;
    private Double mttrMinutes;
    private Double releasesThisMonth;
    private Double rolledBackTotal;
    private long asOfEpochMs;

    public LiveHealthDTO() {}

    public LiveHealthDTO(boolean scrapeUp, Double uptimePercent, Double mttrMinutes,
                          Double releasesThisMonth, Double rolledBackTotal, long asOfEpochMs) {
        this.scrapeUp = scrapeUp;
        this.uptimePercent = uptimePercent;
        this.mttrMinutes = mttrMinutes;
        this.releasesThisMonth = releasesThisMonth;
        this.rolledBackTotal = rolledBackTotal;
        this.asOfEpochMs = asOfEpochMs;
    }

    public boolean isScrapeUp() { return scrapeUp; }
    public void setScrapeUp(boolean scrapeUp) { this.scrapeUp = scrapeUp; }

    public Double getUptimePercent() { return uptimePercent; }
    public void setUptimePercent(Double uptimePercent) { this.uptimePercent = uptimePercent; }

    public Double getMttrMinutes() { return mttrMinutes; }
    public void setMttrMinutes(Double mttrMinutes) { this.mttrMinutes = mttrMinutes; }

    public Double getReleasesThisMonth() { return releasesThisMonth; }
    public void setReleasesThisMonth(Double releasesThisMonth) { this.releasesThisMonth = releasesThisMonth; }

    public Double getRolledBackTotal() { return rolledBackTotal; }
    public void setRolledBackTotal(Double rolledBackTotal) { this.rolledBackTotal = rolledBackTotal; }

    public long getAsOfEpochMs() { return asOfEpochMs; }
    public void setAsOfEpochMs(long asOfEpochMs) { this.asOfEpochMs = asOfEpochMs; }
}
