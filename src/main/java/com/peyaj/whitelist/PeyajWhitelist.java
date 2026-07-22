package com.peyaj.whitelist;

import com.peyaj.whitelist.command.WhitelistCommand;
import com.peyaj.whitelist.hook.ActiveFloodgateHook;
import com.peyaj.whitelist.hook.ActiveLuckPermsHook;
import com.peyaj.whitelist.hook.DummyFloodgateHook;
import com.peyaj.whitelist.hook.DummyLuckPermsHook;
import com.peyaj.whitelist.hook.IFloodgateHook;
import com.peyaj.whitelist.hook.ILuckPermsHook;
import com.peyaj.whitelist.listener.PlayerLoginListener;
import com.peyaj.whitelist.manager.WhitelistManager;
import com.peyaj.whitelist.model.PendingRequest;
import com.peyaj.whitelist.placeholder.PeyajWhitelistExpansion;
import com.peyaj.whitelist.util.AuditLogger;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PeyajWhitelist extends JavaPlugin {

    private WhitelistManager whitelistManager;
    private IFloodgateHook floodgateHook;
    private ILuckPermsHook luckPermsHook;
    private AuditLogger auditLogger;
    private volatile boolean whitelistEnabled;
    private volatile boolean maintenanceMode;

    @Override
    public void onEnable() {
        // Initialize Audit Logger
        this.auditLogger = new AuditLogger(this);

        // Load whitelist enabled state
        this.whitelistEnabled = getConfig().getBoolean("enabled", true);
        this.maintenanceMode = getConfig().getBoolean("maintenance", false);

        // Detect and hook into Floodgate (soft-dependency)
        if (getServer().getPluginManager().isPluginEnabled("Floodgate")) {
            this.floodgateHook = new ActiveFloodgateHook();
            getLogger().info("Successfully hooked into Floodgate API. Bedrock crossplay support is ACTIVE.");
        } else {
            this.floodgateHook = new DummyFloodgateHook();
            getLogger().info("Floodgate was not detected. Running in standard Java-only mode.");
        }

        // Detect and hook into LuckPerms (soft-dependency)
        if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            this.luckPermsHook = new ActiveLuckPermsHook();
            getLogger().info("Successfully hooked into LuckPerms API. Group/Permission bypass is ACTIVE.");
        } else {
            this.luckPermsHook = new DummyLuckPermsHook();
            getLogger().info("LuckPerms was not detected. Group bypass is INACTIVE.");
        }

        // Initialize Whitelist Manager
        this.whitelistManager = new WhitelistManager(this);

        // Register listener
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(this), this);

        // Register PlaceholdersAPI expansion
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PeyajWhitelistExpansion(this).register();
            getLogger().info("Successfully registered PlaceholdersAPI expansion.");
        }

        // Disable vanilla whitelist to prevent dual checks and conflicts
        getServer().setWhitelist(false);

        // Register commands
        WhitelistCommand cmd = new WhitelistCommand(this);
        if (getCommand("pwhitelist") != null) {
            getCommand("pwhitelist").setExecutor(cmd);
            getCommand("pwhitelist").setTabCompleter(cmd);
        }
        if (getCommand("whitelist") != null) {
            getCommand("whitelist").setExecutor(cmd);
            getCommand("whitelist").setTabCompleter(cmd);
        }

        // Initialize bStats Metrics
        int pluginId = 32816;
        new Metrics(this, pluginId);

        // Log and print minimalist startup banner
        auditLogger.log("SYSTEM", "PeyajWhitelist has been enabled.");
        printBanner();
    }

    @Override
    public void onDisable() {
        if (auditLogger != null) {
            auditLogger.log("SYSTEM", "PeyajWhitelist has been disabled.");
            auditLogger.shutdown();
        }
        getLogger().info("PeyajWhitelist has been disabled.");
    }

    /**
     * Prints a minimalist console banner on startup.
     */
    private void printBanner() {
        boolean floodgateActive = getServer().getPluginManager().isPluginEnabled("Floodgate");
        boolean luckpermsActive = getServer().getPluginManager().isPluginEnabled("LuckPerms");
        getLogger().info("▲ PeyajWhitelist v4.2");
        getLogger().info("▪ Whitelist: " + (whitelistEnabled ? "§aEnabled" : "§cDisabled"));
        getLogger().info("▪ Maintenance: " + (maintenanceMode ? "§cEnabled" : "§aDisabled"));
        getLogger().info("▪ Crossplay: " + (floodgateActive ? "§aActive (Geyser/Floodgate)" : "§7Inactive"));
        getLogger().info("▪ LuckPerms: " + (luckpermsActive ? "§aHooked (Bypasses active)" : "§7Inactive"));
    }

    /**
     * Reloads configuration files and refreshes the cache.
     */
    public void reloadPluginConfig() {
        reloadConfig();
        this.whitelistEnabled = getConfig().getBoolean("enabled", true);
        this.maintenanceMode = getConfig().getBoolean("maintenance", false);
        if (whitelistManager != null) {
            whitelistManager.loadWhitelist();
        }
        if (auditLogger != null) {
            auditLogger.log("SYSTEM", "Plugin configuration reloaded.");
        }
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public void setWhitelistEnabled(boolean enabled) {
        this.whitelistEnabled = enabled;
        getConfig().set("enabled", enabled);
        saveConfig();
        if (auditLogger != null) {
            auditLogger.log("SYSTEM", "Whitelist toggled to " + (enabled ? "ENABLED" : "DISABLED"));
        }
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean active) {
        this.maintenanceMode = active;
        getConfig().set("maintenance", active);
        saveConfig();
        if (auditLogger != null) {
            auditLogger.log("MAINTENANCE", "Maintenance mode set to " + (active ? "ENABLED" : "DISABLED"));
        }
    }

    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public IFloodgateHook getFloodgateHook() {
        return floodgateHook;
    }

    public ILuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    public AuditLogger getAuditLogger() {
        return auditLogger;
    }

    public boolean isVerbose() {
        return getConfig().getBoolean("debug", false);
    }

    private final List<PendingRequest> pendingRequests = Collections.synchronizedList(new ArrayList<>());

    public List<PendingRequest> getPendingRequests() {
        synchronized (pendingRequests) {
            return new ArrayList<>(pendingRequests);
        }
    }

    public void addPendingRequest(PendingRequest request) {
        synchronized (pendingRequests) {
            pendingRequests.removeIf(r -> r.getUuid().equals(request.getUuid()) || r.getName().equalsIgnoreCase(request.getName()));
            pendingRequests.add(0, request);
            if (pendingRequests.size() > 20) {
                pendingRequests.remove(pendingRequests.size() - 1);
            }
        }
    }

    public PendingRequest popPendingRequest(int index) {
        synchronized (pendingRequests) {
            if (index >= 0 && index < pendingRequests.size()) {
                return pendingRequests.remove(index);
            }
            return null;
        }
    }

    public PendingRequest popPendingRequest(String nameOrUuid) {
        synchronized (pendingRequests) {
            for (int i = 0; i < pendingRequests.size(); i++) {
                PendingRequest r = pendingRequests.get(i);
                if (r.getName().equalsIgnoreCase(nameOrUuid) || r.getUuid().toString().equalsIgnoreCase(nameOrUuid)) {
                    return pendingRequests.remove(i);
                }
            }
            return null;
        }
    }

    /**
     * Sends a rich notification embed to Discord when a connection attempt is rejected or whitelisted.
     */
    public void fireWebhook(String type, String playerName, String uuid, String xuid, String platform, String actor) {
        if (!getConfig().getBoolean("discord-webhook.enabled", false)) return;
        String url = getConfig().getString("discord-webhook.url");
        if (url == null || url.trim().isEmpty() || url.contains("YOUR_DISCORD_WEBHOOK_URL_HERE")) return;

        boolean isReject = type.equalsIgnoreCase("reject");
        if (isReject && !getConfig().getBoolean("discord-webhook.notify-on-reject", true)) return;
        if (!isReject && !getConfig().getBoolean("discord-webhook.notify-on-whitelist", true)) return;

        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        
        String webhookName = getConfig().getString("discord-webhook.username", "PeyajWhitelist");
        String avatarUrl = getConfig().getString("discord-webhook.avatar-url", "");

        payload.addProperty("username", webhookName);
        if (avatarUrl != null && !avatarUrl.trim().isEmpty() && !avatarUrl.contains("YOUR_AVATAR_IMAGE_URL_HERE")) {
            payload.addProperty("avatar_url", avatarUrl);
        }

        com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
        com.google.gson.JsonObject embed = new com.google.gson.JsonObject();

        String stylePath = isReject ? "discord-webhook.styling.reject" : "discord-webhook.styling.whitelist";
        String defaultTitle = isReject ? "❌ Whitelist Rejection" : "✅ Player Whitelisted";
        String defaultColor = isReject ? "#ff3332" : "#3bc033";

        String title = getConfig().getString(stylePath + ".title", defaultTitle);
        String colorHex = getConfig().getString(stylePath + ".color", defaultColor);

        int colorInt;
        try {
            colorInt = Integer.parseInt(colorHex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            colorInt = isReject ? 16724530 : 3916595;
        }

        embed.addProperty("title", title);
        embed.addProperty("color", colorInt);

        StringBuilder desc = new StringBuilder();
        if (isReject) {
            desc.append(String.format("Player **%s** attempted to connect but was rejected.", playerName));
        } else {
            desc.append(String.format("Player **%s** has been whitelisted.", playerName));
            if (actor != null) {
                desc.append(String.format("\n**Approved by:** %s", actor));
            }
        }
        embed.addProperty("description", desc.toString());

        com.google.gson.JsonArray fields = new com.google.gson.JsonArray();

        com.google.gson.JsonObject f1 = new com.google.gson.JsonObject();
        f1.addProperty("name", "Platform");
        f1.addProperty("value", platform);
        f1.addProperty("inline", true);
        fields.add(f1);

        if (uuid != null) {
            com.google.gson.JsonObject f2 = new com.google.gson.JsonObject();
            f2.addProperty("name", "UUID");
            f2.addProperty("value", "`" + uuid + "`");
            f2.addProperty("inline", false);
            fields.add(f2);
        }

        if (xuid != null) {
            com.google.gson.JsonObject f3 = new com.google.gson.JsonObject();
            f3.addProperty("name", "Xbox XUID");
            f3.addProperty("value", "`" + xuid + "`");
            f3.addProperty("inline", true);
            fields.add(f3);
        }

        embed.add("fields", fields);

        // Thumbnail (head avatar)
        if (uuid != null) {
            com.google.gson.JsonObject thumbnail = new com.google.gson.JsonObject();
            thumbnail.addProperty("url", "https://mc-heads.net/avatar/" + uuid + "/64");
            embed.add("thumbnail", thumbnail);
        }

        com.google.gson.JsonObject footer = new com.google.gson.JsonObject();
        footer.addProperty("text", "PeyajWhitelist v4.2");
        embed.add("footer", footer);

        embeds.add(embed);
        payload.add("embeds", embeds);

        com.peyaj.whitelist.util.DiscordWebhook.sendWebhook(getLogger(), url, payload);
    }
}
