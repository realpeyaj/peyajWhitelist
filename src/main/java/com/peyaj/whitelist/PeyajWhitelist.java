package com.peyaj.whitelist;

import com.peyaj.whitelist.command.WhitelistCommand;
import com.peyaj.whitelist.hook.ActiveFloodgateHook;
import com.peyaj.whitelist.hook.DummyFloodgateHook;
import com.peyaj.whitelist.hook.IFloodgateHook;
import com.peyaj.whitelist.listener.PlayerLoginListener;
import com.peyaj.whitelist.manager.WhitelistManager;
import com.peyaj.whitelist.model.PendingRequest;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PeyajWhitelist extends JavaPlugin {

    private WhitelistManager whitelistManager;
    private IFloodgateHook floodgateHook;
    private volatile boolean whitelistEnabled;

    @Override
    public void onEnable() {
        // Save default config.yml if it doesn't exist
        saveDefaultConfig();

        // Load whitelist enabled state
        this.whitelistEnabled = getConfig().getBoolean("enabled", true);

        // Detect and hook into Floodgate (soft-dependency)
        if (getServer().getPluginManager().isPluginEnabled("Floodgate")) {
            this.floodgateHook = new ActiveFloodgateHook();
            getLogger().info("Successfully hooked into Floodgate API. Bedrock crossplay support is ACTIVE.");
        } else {
            this.floodgateHook = new DummyFloodgateHook();
            getLogger().info("Floodgate was not detected. Running in standard Java-only mode.");
        }

        // Initialize Whitelist Manager
        this.whitelistManager = new WhitelistManager(this);

        // Register listener
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(this), this);

        // Register command
        WhitelistCommand cmd = new WhitelistCommand(this);
        if (getCommand("pwhitelist") != null) {
            getCommand("pwhitelist").setExecutor(cmd);
            getCommand("pwhitelist").setTabCompleter(cmd);
        }

        // Print minimalist startup banner
        printBanner();
    }

    @Override
    public void onDisable() {
        getLogger().info("PeyajWhitelist has been disabled.");
    }

    /**
     * Prints a minimalist console banner on startup.
     */
    private void printBanner() {
        boolean floodgateActive = getServer().getPluginManager().isPluginEnabled("Floodgate");
        getLogger().info("▲ PeyajWhitelist v1.0.0");
        getLogger().info("▪ Whitelist: " + (whitelistEnabled ? "§aEnabled" : "§cDisabled"));
        getLogger().info("▪ Crossplay: " + (floodgateActive ? "§aActive (Geyser/Floodgate)" : "§7Inactive (Java-only)"));
    }

    /**
     * Reloads configuration files and refreshes the cache.
     */
    public void reloadPluginConfig() {
        reloadConfig();
        this.whitelistEnabled = getConfig().getBoolean("enabled", true);
        if (whitelistManager != null) {
            whitelistManager.loadWhitelist();
        }
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public void setWhitelistEnabled(boolean enabled) {
        this.whitelistEnabled = enabled;
        getConfig().set("enabled", enabled);
        saveConfig();
    }

    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public IFloodgateHook getFloodgateHook() {
        return floodgateHook;
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
        com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
        com.google.gson.JsonObject embed = new com.google.gson.JsonObject();

        embed.addProperty("title", isReject ? "❌ Whitelist Rejection" : "✅ Player Whitelisted");
        embed.addProperty("color", isReject ? 16724530 : 3916595); // Red (#ff3332) or Green (#3bc033)

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
        footer.addProperty("text", "PeyajWhitelist v1.0.0");
        embed.add("footer", footer);

        embeds.add(embed);
        payload.add("embeds", embeds);

        com.peyaj.whitelist.util.DiscordWebhook.sendWebhook(getLogger(), url, payload);
    }
}
