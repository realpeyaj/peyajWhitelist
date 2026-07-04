package com.peyaj.whitelist.manager;

import com.peyaj.whitelist.PeyajWhitelist;
import com.peyaj.whitelist.hook.IFloodgateHook;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class WhitelistManager {

    private final PeyajWhitelist plugin;
    private final File whitelistFile;
    private FileConfiguration whitelistConfig;

    private final Set<String> whitelistedNames = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<UUID> whitelistedUuids = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<String> whitelistedXuids = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public WhitelistManager(PeyajWhitelist plugin) {
        this.plugin = plugin;
        this.whitelistFile = new File(plugin.getDataFolder(), "whitelist.yml");
        loadWhitelist();
    }

    /**
     * Loads the whitelist data from whitelist.yml into memory cache.
     */
    public synchronized void loadWhitelist() {
        if (!whitelistFile.exists()) {
            plugin.saveResource("whitelist.yml", false);
        }

        try {
            whitelistConfig = YamlConfiguration.loadConfiguration(whitelistFile);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to parse whitelist.yml! The file may be corrupted.", t);
            File backup = new File(plugin.getDataFolder(), "whitelist.yml.corrupted_" + System.currentTimeMillis());
            if (whitelistFile.renameTo(backup)) {
                plugin.getLogger().severe("Corrupted whitelist.yml has been renamed to " + backup.getName());
            }
            plugin.saveResource("whitelist.yml", true);
            whitelistConfig = YamlConfiguration.loadConfiguration(whitelistFile);
        }

        whitelistedNames.clear();
        whitelistedUuids.clear();
        whitelistedXuids.clear();

        List<String> names = whitelistConfig.getStringList("names");
        for (String name : names) {
            if (name != null && !name.trim().isEmpty()) {
                whitelistedNames.add(name.trim().toLowerCase());
            }
        }

        List<String> uuids = whitelistConfig.getStringList("uuids");
        for (String uuidStr : uuids) {
            if (uuidStr != null && !uuidStr.trim().isEmpty()) {
                try {
                    whitelistedUuids.add(UUID.fromString(uuidStr.trim()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID found in whitelist.yml: " + uuidStr);
                }
            }
        }

        List<String> xuids = whitelistConfig.getStringList("xuids");
        for (String xuid : xuids) {
            if (xuid != null && !xuid.trim().isEmpty()) {
                whitelistedXuids.add(xuid.trim());
            }
        }

        if (plugin.isVerbose()) {
            plugin.getLogger().info(String.format("Loaded %d names, %d UUIDs, and %d XUIDs into whitelist cache.",
                    whitelistedNames.size(), whitelistedUuids.size(), whitelistedXuids.size()));
        }
    }

    /**
     * Saves the memory cache back to whitelist.yml.
     */
    private synchronized void saveWhitelist() {
        try {
            List<String> namesList = new java.util.ArrayList<>(whitelistedNames);
            List<String> uuidsList = new java.util.ArrayList<>();
            for (UUID uuid : whitelistedUuids) {
                uuidsList.add(uuid.toString());
            }
            List<String> xuidsList = new java.util.ArrayList<>(whitelistedXuids);

            whitelistConfig.set("names", namesList);
            whitelistConfig.set("uuids", uuidsList);
            whitelistConfig.set("xuids", xuidsList);

            whitelistConfig.save(whitelistFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save whitelist.yml", e);
        }
    }

    /**
     * Checks if a player is whitelisted based on their UUID and Username.
     * Performs multi-factor matching for Bedrock players.
     */
    public boolean isWhitelisted(UUID uuid, String name) {
        if (!plugin.isWhitelistEnabled()) {
            return true;
        }

        // 1. Check direct UUID match
        if (whitelistedUuids.contains(uuid)) {
            if (plugin.isVerbose()) {
                plugin.getLogger().info("[PeyajWhitelist Debug] Matched " + name + " by UUID: " + uuid);
            }
            return true;
        }

        // 2. Check direct case-insensitive name match
        String lowercaseName = name.toLowerCase();
        if (whitelistedNames.contains(lowercaseName)) {
            if (plugin.isVerbose()) {
                plugin.getLogger().info("[PeyajWhitelist Debug] Matched " + name + " by exact username.");
            }
            return true;
        }

        // 3. Floodgate integration checks
        IFloodgateHook floodgate = plugin.getFloodgateHook();
        if (floodgate.isBedrockPlayer(uuid)) {
            // A. Check XUID
            String xuid = floodgate.getXuid(uuid);
            if (xuid != null && whitelistedXuids.contains(xuid)) {
                if (plugin.isVerbose()) {
                    plugin.getLogger().info("[PeyajWhitelist Debug] Matched Bedrock player " + name + " by XUID: " + xuid);
                }
                return true;
            }

            // B. Check original Bedrock gamertag (no prefix, spaces intact)
            String rawUsername = floodgate.getRawUsername(uuid);
            if (rawUsername != null) {
                String rawLower = rawUsername.toLowerCase();
                if (whitelistedNames.contains(rawLower)) {
                    if (plugin.isVerbose()) {
                        plugin.getLogger().info("[PeyajWhitelist Debug] Matched Bedrock player by raw username: " + rawUsername);
                    }
                    return true;
                }
                // Also check if they whitelisted with space replaced by underscore (e.g. John_Doe instead of John Doe)
                if (whitelistedNames.contains(rawLower.replace(' ', '_'))) {
                    if (plugin.isVerbose()) {
                        plugin.getLogger().info("[PeyajWhitelist Debug] Matched Bedrock player by raw username with space replaced.");
                    }
                    return true;
                }
            }

            // C. Check java-mapped username (with prefix, e.g. .John_Doe)
            String javaUsername = floodgate.getJavaUsername(uuid);
            if (javaUsername != null && whitelistedNames.contains(javaUsername.toLowerCase())) {
                if (plugin.isVerbose()) {
                    plugin.getLogger().info("[PeyajWhitelist Debug] Matched Bedrock player by java username: " + javaUsername);
                }
                return true;
            }

            // D. Check correct username
            String correctUsername = floodgate.getCorrectUsername(uuid);
            if (correctUsername != null && whitelistedNames.contains(correctUsername.toLowerCase())) {
                if (plugin.isVerbose()) {
                    plugin.getLogger().info("[PeyajWhitelist Debug] Matched Bedrock player by correct username: " + correctUsername);
                }
                return true;
            }
        }

        // 4. Auto-detect Bedrock prefix fallback (if Floodgate configuration prefix is different or not fully resolved)
        if (plugin.getConfig().getBoolean("auto-detect-bedrock-prefix", true)) {
            if (name.length() > 1 && !Character.isLetterOrDigit(name.charAt(0))) {
                // Strip the prefix character and check
                String stripped = name.substring(1).toLowerCase();
                if (whitelistedNames.contains(stripped)) {
                    if (plugin.isVerbose()) {
                        plugin.getLogger().info("[PeyajWhitelist Debug] Matched player by prefix-stripped username: " + name.substring(1));
                    }
                    return true;
                }
                // Check if spaces replaced by underscores (e.g., .John_Doe -> John Doe)
                if (whitelistedNames.contains(stripped.replace('_', ' '))) {
                    if (plugin.isVerbose()) {
                        plugin.getLogger().info("[PeyajWhitelist Debug] Matched player by prefix-stripped username with space replacing underscores.");
                    }
                    return true;
                }
                if (whitelistedNames.contains(stripped.replace(' ', '_'))) {
                    if (plugin.isVerbose()) {
                        plugin.getLogger().info("[PeyajWhitelist Debug] Matched player by prefix-stripped username with underscores replacing spaces.");
                    }
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Adds an entry to the whitelist.
     * Automatically detects if the input is UUID, XUID, or Name.
     *
     * @param input The identifier (name, UUID, or XUID)
     * @return Result message
     */
    public synchronized String addPlayer(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "§cInvalid player identifier.";
        }

        input = input.trim();

        // Check if UUID
        try {
            UUID uuid = UUID.fromString(input);
            if (whitelistedUuids.add(uuid)) {
                saveWhitelist();
                return "§aSuccessfully whitelisted UUID: §e" + uuid;
            }
            return "§eUUID " + uuid + " is already whitelisted.";
        } catch (IllegalArgumentException e) {
            // Not a UUID, continue
        }

        // Check if XUID (Numeric, length between 10 and 20)
        if (input.matches("\\d{10,20}")) {
            if (whitelistedXuids.add(input)) {
                saveWhitelist();
                return "§aSuccessfully whitelisted Bedrock XUID: §e" + input;
            }
            return "§eXUID " + input + " is already whitelisted.";
        }

        // Treat as Username
        String lowercaseName = input.toLowerCase();
        if (whitelistedNames.add(lowercaseName)) {
            saveWhitelist();
            return "§aSuccessfully whitelisted username: §e" + input;
        }
        return "§eUsername " + input + " is already whitelisted.";
    }

    /**
     * Removes an entry from the whitelist.
     * Automatically detects if the input is UUID, XUID, or Name.
     *
     * @param input The identifier (name, UUID, or XUID)
     * @return Result message
     */
    public synchronized String removePlayer(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "§cInvalid player identifier.";
        }

        input = input.trim();

        // Check if UUID
        try {
            UUID uuid = UUID.fromString(input);
            if (whitelistedUuids.remove(uuid)) {
                saveWhitelist();
                return "§aSuccessfully removed UUID §e" + uuid + " §afrom whitelist.";
            }
            return "§cUUID " + uuid + " is not whitelisted.";
        } catch (IllegalArgumentException e) {
            // Not a UUID, continue
        }

        // Check if XUID
        if (input.matches("\\d{10,20}")) {
            if (whitelistedXuids.remove(input)) {
                saveWhitelist();
                return "§aSuccessfully removed Bedrock XUID §e" + input + " §afrom whitelist.";
            }
            return "§cXUID " + input + " is not whitelisted.";
        }

        // Treat as Username
        String lowercaseName = input.toLowerCase();
        if (whitelistedNames.remove(lowercaseName)) {
            saveWhitelist();
            return "§aSuccessfully removed username §e" + input + " §afrom whitelist.";
        }
        return "§cUsername " + input + " is not whitelisted.";
    }

    /**
     * Clear all whitelist entries.
     */
    public synchronized void clearWhitelist() {
        whitelistedNames.clear();
        whitelistedUuids.clear();
        whitelistedXuids.clear();
        saveWhitelist();
    }

    public Set<String> getWhitelistedNames() {
        return new HashSet<>(whitelistedNames);
    }

    public Set<UUID> getWhitelistedUuids() {
        return new HashSet<>(whitelistedUuids);
    }

    public Set<String> getWhitelistedXuids() {
        return new HashSet<>(whitelistedXuids);
    }
}
