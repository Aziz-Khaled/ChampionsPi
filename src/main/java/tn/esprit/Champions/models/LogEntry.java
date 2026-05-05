package tn.esprit.Champions.models;

public class LogEntry {
    private String time, action, details;
    public LogEntry(String time, String action, String details) {
        this.time = time; this.action = action; this.details = details;
    }
    public String getTime() { return time; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
}