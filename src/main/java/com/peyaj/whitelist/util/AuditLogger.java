package com.peyaj.whitelist.util;

import com.peyaj.whitelist.PeyajWhitelist;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class AuditLogger {

    private final PeyajWhitelist plugin;
    private final File logFile;
    private final ExecutorService executor;
    private final SimpleDateFormat dateFormat;

    public AuditLogger(PeyajWhitelist plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "audit.log");
        this.executor = Executors.newSingleThreadExecutor();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Ensure plugin directory exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }

    /**
     * Appends an audit message asynchronously to the audit.log.
     *
     * @param action      The action type (e.g. ADDED, REMOVED, MAINTENANCE, KICK)
     * @param description Details of the action
     */
    public void log(String action, String description) {
        final String timestamp = dateFormat.format(new Date());
        final String logLine = String.format("[%s] [%s] %s\n", timestamp, action.toUpperCase(), description);

        executor.submit(() -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile, true), StandardCharsets.UTF_8))) {
                writer.write(logLine);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to write to audit.log", e);
            }
        });
    }

    /**
     * Reads recent logs, optionally filtering by player name/details.
     *
     * @param maxLines     Maximum entries to retrieve
     * @param filterPlayer Optional name filter (case-insensitive)
     * @return List of matching log lines
     */
    public List<String> getRecentLogs(int maxLines, String filterPlayer) {
        List<String> matching = new ArrayList<>();
        if (!logFile.exists()) {
            return matching;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            String line;
            String filterLower = filterPlayer != null ? filterPlayer.toLowerCase() : null;

            while ((line = reader.readLine()) != null) {
                if (filterLower == null || line.toLowerCase().contains(filterLower)) {
                    matching.add(line);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read audit.log", e);
        }

        // Slice to get only the last 'maxLines' entries
        if (matching.size() > maxLines) {
            return matching.subList(matching.size() - maxLines, matching.size());
        }
        return matching;
    }

    /**
     * Safely shut down the single-thread writer executor.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
