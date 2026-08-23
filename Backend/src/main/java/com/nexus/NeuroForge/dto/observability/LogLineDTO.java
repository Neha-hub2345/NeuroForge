// LogLineDTO.java — [M4][Jashanpreet]
// One row for the Releases & Monitoring "recent logs" panel.
package com.nexus.NeuroForge.dto.observability;

public class LogLineDTO {
    private final String timestamp;
    private final String level;
    private final String message;

    public LogLineDTO(String timestamp, String level, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }

    public String getTimestamp() { return timestamp; }
    public String getLevel() { return level; }
    public String getMessage() { return message; }
}
