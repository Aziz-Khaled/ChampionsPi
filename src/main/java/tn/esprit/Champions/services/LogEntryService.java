package tn.esprit.Champions.services;

import tn.esprit.Champions.models.LogEntry;
import tn.esprit.Champions.utils.DbConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogEntryService {

    public void saveLog(String action, String details) {
        String sql = "INSERT INTO system_logs (action, details) VALUES (?, ?)";
        try (PreparedStatement pstmt = DbConnection.getInstance().getCnx().prepareStatement(sql)) {
            pstmt.setString(1, action);
            pstmt.setString(2, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving log: " + e.getMessage());
        }
    }

    public List<LogEntry> getAllLogs() {
        List<LogEntry> logs = new ArrayList<>();
        String sql = "SELECT * FROM system_logs ORDER BY timestamp DESC";
        try (Statement stmt = DbConnection.getInstance().getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                logs.add(new LogEntry(
                        rs.getTimestamp("timestamp").toString(),
                        rs.getString("action"),
                        rs.getString("details")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching logs: " + e.getMessage());
        }
        return logs;
    }

    public void clearAllLogs() {
        String sql = "DELETE FROM system_logs";
        try (Statement stmt = DbConnection.getInstance().getCnx().createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Error clearing logs: " + e.getMessage());
        }
    }
}