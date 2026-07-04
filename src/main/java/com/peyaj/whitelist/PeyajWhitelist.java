package com.peyaj.whitelist;

import com.peyaj.whitelist.command.WhitelistCommand;
import com.peyaj.whitelist.hook.ActiveFloodgateHook;
import com.peyaj.whitelist.hook.DummyFloodgateHook;
import com.peyaj.whitelist.hook.IFloodgateHook;
import com.peyaj.whitelist.listener.PlayerLoginListener;
import com.peyaj.whitelist.manager.WhitelistManager;
import org.bukkit.plugin.java.JavaPlugin;

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
}
