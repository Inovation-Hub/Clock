package main;

public class TimeRecord {
    public final String timestamp;
    public final String duration;
    public final long   totalSeconds;
    public final String description;

    public TimeRecord(String timestamp, String duration, long totalSeconds, String description) {
        this.timestamp    = timestamp;
        this.duration     = duration;
        this.totalSeconds = totalSeconds;
        this.description  = description;
    }
}
