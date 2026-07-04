package com.peyaj.whitelist.listener;

import com.peyaj.whitelist.PeyajWhitelist;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerLoginListener implements Listener {

    private final PeyajWhitelist plugin;
    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public PlayerLoginListener(PeyajWhitelist plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();

        if (plugin.isVerbose()) {
            plugin.getLogger().info(String.format("Processing connection request: %s (%s)", name, uuid));
        }

        // 1. Maintenance Mode Gate Check
        if (plugin.isMaintenanceMode()) {
            boolean isOp = org.bukkit.Bukkit.getOfflinePlayer(uuid).isOp();
            boolean hasBypass = plugin.getLuckPermsHook().hasBypassPermission(uuid) || isOp;
            if (!hasBypass) {
                String kickMsg = plugin.getConfig().getString("kick-message-maintenance", 
                        "&cThe server is currently under maintenance!");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, translateColors(kickMsg));
                plugin.getAuditLogger().log("KICK", String.format("Denied %s (%s) - Server is in maintenance mode.", name, uuid));
                return;
            }
        }

        // 2. Check if player has LuckPerms whitelist bypass permission
        if (plugin.getConfig().getBoolean("luckperms-bypass", true) && plugin.getLuckPermsHook().hasBypassPermission(uuid)) {
            if (plugin.isVerbose()) {
                plugin.getLogger().info("[PeyajWhitelist Debug] Allowed connection bypass for: " + name + " (has bypass permission)");
            }
            return;
        }

        // 3. Check if player is whitelisted
        if (!plugin.getWhitelistManager().isWhitelisted(uuid, name)) {
            // Gather details for pending request queue and webhook
            boolean isBedrock = plugin.getFloodgateHook().isBedrockPlayer(uuid);
            String platform = isBedrock ? "Bedrock" : "Java";
            String xuid = isBedrock ? plugin.getFloodgateHook().getXuid(uuid) : null;

            // Register rejected request
            plugin.addPendingRequest(new com.peyaj.whitelist.model.PendingRequest(name, uuid, xuid, platform));

            // Log rejection to audit file
            plugin.getAuditLogger().log("KICK", String.format("Denied %s (%s) - Not whitelisted (Platform: %s).", name, uuid, platform));

            // Trigger Discord Webhook
            plugin.fireWebhook("reject", name, uuid.toString(), xuid, platform, null);

            // Determine kick message based on platform
            String kickKey = isBedrock ? "kick-message-bedrock" : "kick-message";
            String kickMessage = plugin.getConfig().getString(kickKey);

            if (isBedrock && (kickMessage == null || kickMessage.trim().isEmpty())) {
                kickMessage = plugin.getConfig().getString("kick-message", "&cYou are not whitelisted on this server!");
            }
            if (kickMessage == null) {
                kickMessage = "&cYou are not whitelisted on this server!";
            }
            
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, translateColors(kickMessage));
            
            if (plugin.isVerbose() || plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format("Denied connection for non-whitelisted player: %s (%s)", name, uuid));
            }
        } else {
            if (plugin.isVerbose()) {
                plugin.getLogger().info(String.format("Allowed connection for whitelisted player: %s (%s)", name, uuid));
            }
        }
    }

    /**
     * Translates hex and legacy color codes in the message.
     */
    private String translateColors(String message) {
        if (message == null) {
            return "";
        }

        // 1. Translate hex colors (e.g. &#ff0000 -> chat color equivalent)
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        String hexTranslated = sb.toString();

        // 2. Translate legacy color codes (e.g. &c -> red)
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', hexTranslated);
    }
}
