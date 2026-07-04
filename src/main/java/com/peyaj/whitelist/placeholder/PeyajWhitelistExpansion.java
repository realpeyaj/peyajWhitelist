package com.peyaj.whitelist.placeholder;

import com.peyaj.whitelist.PeyajWhitelist;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PeyajWhitelistExpansion extends PlaceholderExpansion {

    private final PeyajWhitelist plugin;

    public PeyajWhitelistExpansion(PeyajWhitelist plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "peyajwhitelist";
    }

    @Override
    public @NotNull String getAuthor() {
        return "realpeyaj";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // Keep registered on reload
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params.toLowerCase()) {
            case "status":
                return plugin.isWhitelistEnabled() ? "Enabled" : "Disabled";

            case "count":
                if (plugin.getWhitelistManager() == null) {
                    return "0";
                }
                int count = plugin.getWhitelistManager().getWhitelistedNames().size()
                        + plugin.getWhitelistManager().getWhitelistedUuids().size()
                        + plugin.getWhitelistManager().getWhitelistedXuids().size();
                return String.valueOf(count);

            case "pending_count":
                return String.valueOf(plugin.getPendingRequests().size());

            case "maintenance":
                return plugin.isMaintenanceMode() ? "Enabled" : "Disabled";

            default:
                return null; // Unknown placeholder
        }
    }
}
