package com.peyaj.whitelist.command;

import com.peyaj.whitelist.PeyajWhitelist;
import com.peyaj.whitelist.manager.WhitelistManager;
import com.peyaj.whitelist.hook.IFloodgateHook;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;

public class WhitelistCommand implements CommandExecutor, TabCompleter {

    private final PeyajWhitelist plugin;
    private static final String PREFIX = "§8[§d§lPeyajWhitelist§8] §r";

    public WhitelistCommand(PeyajWhitelist plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("peyajwhitelist.admin")) {
            sender.sendMessage(PREFIX + "§cYou do not have permission to execute this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        WhitelistManager manager = plugin.getWhitelistManager();

        switch (subCommand) {
            case "on":
                plugin.setWhitelistEnabled(true);
                sender.sendMessage(PREFIX + "§aWhitelist has been enabled.");
                break;

            case "off":
                plugin.setWhitelistEnabled(false);
                sender.sendMessage(PREFIX + "§cWhitelist has been disabled.");
                break;

            case "add":
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + "§cUsage: /" + label + " add <name|uuid|xuid>");
                    return true;
                }
                String addTarget = args[1];
                sender.sendMessage(PREFIX + "§eAdding §6" + addTarget + " §eto the whitelist...");
                // Asynchronous/Safe add
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    String result = manager.addPlayer(addTarget);
                    plugin.getAuditLogger().log("ADDED", String.format("%s by %s", addTarget, sender.getName()));
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(PREFIX + result);
                        if (result.contains("Successfully")) {
                            sendAdminNotification(sender, addTarget);
                        }
                    });
                });
                break;

            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + "§cUsage: /" + label + " remove <name|uuid|xuid>");
                    return true;
                }
                String removeTarget = args[1];
                sender.sendMessage(PREFIX + "§eRemoving §6" + removeTarget + " §efrom the whitelist...");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    String result = manager.removePlayer(removeTarget);
                    plugin.getAuditLogger().log("REMOVED", String.format("%s by %s", removeTarget, sender.getName()));
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(PREFIX + result);
                        
                        // Instant Kick Logic:
                        final String finalRemoveTarget = removeTarget.trim().toLowerCase();
                        List<Player> playersToKick = new ArrayList<>();
                        IFloodgateHook floodgate = plugin.getFloodgateHook();

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            // Match by UUID
                            if (player.getUniqueId().toString().equalsIgnoreCase(finalRemoveTarget)) {
                                playersToKick.add(player);
                                continue;
                            }
                            // Match by Name
                            if (player.getName().equalsIgnoreCase(finalRemoveTarget)) {
                                playersToKick.add(player);
                                continue;
                            }
                            // Floodgate integration checks if Bedrock player
                            if (floodgate.isBedrockPlayer(player.getUniqueId())) {
                                String xuid = floodgate.getXuid(player.getUniqueId());
                                if (xuid != null && xuid.equalsIgnoreCase(finalRemoveTarget)) {
                                    playersToKick.add(player);
                                    continue;
                                }
                                String rawUsername = floodgate.getRawUsername(player.getUniqueId());
                                if (rawUsername != null && rawUsername.equalsIgnoreCase(finalRemoveTarget)) {
                                    playersToKick.add(player);
                                    continue;
                                }
                                String javaUsername = floodgate.getJavaUsername(player.getUniqueId());
                                if (javaUsername != null && javaUsername.equalsIgnoreCase(finalRemoveTarget)) {
                                    playersToKick.add(player);
                                    continue;
                                }
                            }
                            // Auto-detect Bedrock prefix check
                            if (plugin.getConfig().getBoolean("auto-detect-bedrock-prefix", true)) {
                                String name = player.getName();
                                if (name.length() > 1 && (name.startsWith(".") || name.startsWith("*"))) {
                                    String stripped = name.substring(1);
                                    if (stripped.equalsIgnoreCase(finalRemoveTarget)) {
                                        playersToKick.add(player);
                                        continue;
                                    }
                                }
                            }
                        }

                        if (!playersToKick.isEmpty()) {
                            String rawMessage = plugin.getConfig().getString("kick-message-removal", 
                                    "&cYou have been removed from the whitelist.");
                            String coloredMessage = translateColors(rawMessage);
                            for (Player p : playersToKick) {
                                p.kickPlayer(coloredMessage);
                                plugin.getAuditLogger().log("KICK", String.format("Instantly kicked %s - Removed from whitelist by %s", p.getName(), sender.getName()));
                            }
                        }
                    });
                });
                break;

            case "list":
                sender.sendMessage(PREFIX + "§7--- §dWhitelisted Entries §7---");
                Set<String> names = manager.getWhitelistedNames();
                Set<UUID> uuids = manager.getWhitelistedUuids();
                Set<String> xuids = manager.getWhitelistedXuids();

                sender.sendMessage("§eNames §7(" + names.size() + "): §f" + 
                        (names.isEmpty() ? "None" : String.join(", ", names)));
                sender.sendMessage("§eUUIDs §7(" + uuids.size() + "):");
                if (uuids.isEmpty()) {
                    sender.sendMessage("  §fNone");
                } else {
                    for (UUID uuid : uuids) {
                        sender.sendMessage("  §7- §f" + uuid.toString());
                    }
                }
                sender.sendMessage("§eXUIDs (Bedrock) §7(" + xuids.size() + "): §f" + 
                        (xuids.isEmpty() ? "None" : String.join(", ", xuids)));
                break;

            case "reload":
                plugin.reloadPluginConfig();
                sender.sendMessage(PREFIX + "§aConfiguration and Whitelist reloaded from disk.");
                break;

            case "clear":
                if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
                    sender.sendMessage(PREFIX + "§cWarning! This will clear all entries from the whitelist.");
                    sender.sendMessage(PREFIX + "§cType §e/pwhitelist clear confirm §cto proceed.");
                    return true;
                }
                manager.clearWhitelist();
                plugin.getAuditLogger().log("SYSTEM", String.format("Whitelist cleared by %s", sender.getName()));
                sender.sendMessage(PREFIX + "§aSuccessfully cleared all entries from the whitelist.");
                break;

            case "pending":
                List<com.peyaj.whitelist.model.PendingRequest> pending = plugin.getPendingRequests();
                sender.sendMessage(PREFIX + "§7--- §dRecent Rejected Attempts §7---");
                if (pending.isEmpty()) {
                    sender.sendMessage("  §fNo recent connection requests.");
                } else {
                    for (int i = 0; i < pending.size(); i++) {
                        com.peyaj.whitelist.model.PendingRequest r = pending.get(i);
                        long diff = (System.currentTimeMillis() - r.getTimestamp()) / 1000;
                        String timeStr = diff < 60 ? diff + "s ago" : (diff / 60) + "m ago";
                        sender.sendMessage(String.format("  §e%d. §f%s §7(%s) - §e%s", i + 1, r.getName(), r.getPlatform(), timeStr));
                    }
                    sender.sendMessage(PREFIX + "§7To approve: §e/pwhitelist approve <index|name>");
                }
                break;

            case "approve":
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + "§cUsage: /" + label + " approve <index|name>");
                    return true;
                }
                String targetArg = args[1];
                com.peyaj.whitelist.model.PendingRequest req = null;
                try {
                    int index = Integer.parseInt(targetArg) - 1;
                    req = plugin.popPendingRequest(index);
                } catch (NumberFormatException e) {
                    req = plugin.popPendingRequest(targetArg);
                }

                if (req == null) {
                    // Fallback to normal add if not found in pending
                    sender.sendMessage(PREFIX + "§cNo matching pending request found. Adding §6" + targetArg + " §eas a new whitelist entry...");
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        String result = manager.addPlayer(targetArg);
                        plugin.getAuditLogger().log("ADDED", String.format("%s by %s (Approved from empty queue)", targetArg, sender.getName()));
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(PREFIX + result);
                            if (result.contains("Successfully")) {
                                sendAdminNotification(sender, targetArg);
                            }
                            // Simple webhook notify
                            plugin.fireWebhook("whitelist", targetArg, null, null, "Unknown", sender.getName());
                        });
                    });
                } else {
                    final com.peyaj.whitelist.model.PendingRequest finalReq = req;
                    sender.sendMessage(PREFIX + "§eApproving and whitelisting §6" + finalReq.getName() + "§e...");
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        // Whitelist by Name
                        manager.addPlayer(finalReq.getName());
                        // Whitelist by UUID
                        manager.addPlayer(finalReq.getUuid().toString());
                        // Whitelist by XUID if present
                        if (finalReq.getXuid() != null) {
                            manager.addPlayer(finalReq.getXuid());
                        }

                        plugin.getAuditLogger().log("ADDED", String.format("%s (%s) by %s (Approved connection request)", finalReq.getName(), finalReq.getPlatform(), sender.getName()));

                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(PREFIX + "§aSuccessfully whitelisted §e" + finalReq.getName() + " §7(" + finalReq.getPlatform() + ") §aand linked their UUID/XUID.");
                            sendAdminNotification(sender, finalReq.getName());
                            // Rich webhook notification
                            plugin.fireWebhook("whitelist", finalReq.getName(), finalReq.getUuid().toString(), finalReq.getXuid(), finalReq.getPlatform(), sender.getName());
                        });
                    });
                }
                break;

            case "maintenance":
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + "§eMaintenance Mode: " + (plugin.isMaintenanceMode() ? "§cENABLED" : "§aDISABLED"));
                    return true;
                }
                String state = args[1].toLowerCase();
                if (!state.equals("on") && !state.equals("off")) {
                    sender.sendMessage(PREFIX + "§cUsage: /" + label + " maintenance <on|off>");
                    return true;
                }
                boolean active = state.equals("on");
                plugin.setMaintenanceMode(active);
                sender.sendMessage(PREFIX + "§aMaintenance Mode has been set to " + (active ? "§cENABLED" : "§aDISABLED") + "§a.");
                break;

            case "stats":
                sender.sendMessage(PREFIX + "§7--- §dWhitelist Statistics §7---");
                int namesSize = manager.getWhitelistedNames().size();
                int uuidsSize = manager.getWhitelistedUuids().size();
                int xuidsSize = manager.getWhitelistedXuids().size();
                int totalSize = namesSize + uuidsSize + xuidsSize;

                sender.sendMessage("§eTotal Entries: §f" + totalSize);
                sender.sendMessage("§eJava Players (UUIDs): §f" + uuidsSize);
                sender.sendMessage("§eBedrock Players (XUIDs): §f" + xuidsSize);
                sender.sendMessage("§eCustom Usernames: §f" + namesSize);
                break;

            case "audit":
                String filter = args.length > 1 ? args[1] : null;
                sender.sendMessage(PREFIX + "§7--- §dAudit Log Trail (Latest 10) §7---");
                List<String> auditTrail = plugin.getAuditLogger().getRecentLogs(10, filter);
                if (auditTrail.isEmpty()) {
                    sender.sendMessage("  §fNo log history found.");
                } else {
                    for (String line : auditTrail) {
                        // Premium color highlights
                        String coloredLine = line
                                .replace("[ADDED]", "§a[ADDED]§f")
                                .replace("[REMOVED]", "§c[REMOVED]§f")
                                .replace("[MAINTENANCE]", "§6[MAINTENANCE]§f")
                                .replace("[KICK]", "§4[KICK]§f")
                                .replace("[SYSTEM]", "§3[SYSTEM]§f");
                        sender.sendMessage("  " + coloredLine);
                    }
                }
                break;

            case "import-vanilla":
                sender.sendMessage(PREFIX + "§eLocating vanilla whitelist.json...");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    File file = new File("whitelist.json");
                    if (!file.exists()) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(PREFIX + "§cError: whitelist.json was not found in the server root."));
                        return;
                    }

                    try (Reader reader = new FileReader(file)) {
                        com.google.gson.JsonArray array = com.google.gson.JsonParser.parseReader(reader).getAsJsonArray();
                        int importedCount = 0;
                        for (com.google.gson.JsonElement el : array) {
                            com.google.gson.JsonObject obj = el.getAsJsonObject();
                            if (obj.has("name") && obj.has("uuid")) {
                                String targetName = obj.get("name").getAsString();
                                String targetUuid = obj.get("uuid").getAsString();

                                manager.addPlayer(targetName);
                                manager.addPlayer(targetUuid);
                                importedCount++;
                            }
                        }
                        final int finalCount = importedCount;
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(PREFIX + "§aSuccessfully imported §e" + finalCount + " §aplayers from vanilla whitelist.");
                            plugin.getAuditLogger().log("SYSTEM", String.format("Imported %d players from vanilla whitelist.json (Executor: %s)", finalCount, sender.getName()));
                        });
                    } catch (Exception e) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(PREFIX + "§cParsing error: " + e.getMessage()));
                    }
                });
                break;

            default:
                sendHelp(sender, label);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§d§lPeyajWhitelist §7- §fCommands:");
        sender.sendMessage("§e/" + label + " on §7- Enable the whitelist");
        sender.sendMessage("§e/" + label + " off §7- Disable the whitelist");
        sender.sendMessage("§e/" + label + " add <name|uuid|xuid> §7- Add player to whitelist");
        sender.sendMessage("§e/" + label + " remove <name|uuid|xuid> §7- Remove player from whitelist");
        sender.sendMessage("§e/" + label + " list §7- List all whitelisted entries");
        sender.sendMessage("§e/" + label + " reload §7- Reload config files");
        sender.sendMessage("§e/" + label + " pending §7- View recent connection rejection attempts");
        sender.sendMessage("§e/" + label + " approve <index|name> §7- Whitelist and link a pending player");
        sender.sendMessage("§e/" + label + " maintenance <on|off> §7- Toggle server maintenance mode");
        sender.sendMessage("§e/" + label + " stats §7- Show database counts and analytics");
        sender.sendMessage("§e/" + label + " audit [player] §7- Check the database audit trail logs");
        sender.sendMessage("§e/" + label + " import-vanilla §7- Bulk-import standard whitelist.json");
        sender.sendMessage("§e/" + label + " clear §7- Clear the entire whitelist");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("peyajwhitelist.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("on", "off", "add", "remove", "list", "reload", "clear", "pending", "approve", "maintenance", "stats", "audit", "import-vanilla");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
            Collections.sort(completions);
            return completions;
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("remove")) {
                Set<String> whitelistedNames = plugin.getWhitelistManager().getWhitelistedNames();
                StringUtil.copyPartialMatches(args[1], whitelistedNames, completions);
                Collections.sort(completions);
                return completions;
            } else if (subCommand.equals("add")) {
                List<String> onlineNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!plugin.getWhitelistManager().isWhitelisted(player.getUniqueId(), player.getName())) {
                        onlineNames.add(player.getName());
                    }
                }
                StringUtil.copyPartialMatches(args[1], onlineNames, completions);
                Collections.sort(completions);
                return completions;
            } else if (subCommand.equals("approve")) {
                List<String> pendingNames = new ArrayList<>();
                List<com.peyaj.whitelist.model.PendingRequest> requests = plugin.getPendingRequests();
                for (com.peyaj.whitelist.model.PendingRequest r : requests) {
                    pendingNames.add(r.getName());
                }
                StringUtil.copyPartialMatches(args[1], pendingNames, completions);
                Collections.sort(completions);
                return completions;
            } else if (subCommand.equals("maintenance")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList("on", "off"), completions);
                return completions;
            } else if (subCommand.equals("clear")) {
                StringUtil.copyPartialMatches(args[1], Collections.singletonList("confirm"), completions);
                return completions;
            }
        }

        return Collections.emptyList();
    }

    private String translateColors(String message) {
        if (message == null) return "";
        java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher matcher = hexPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hexCode.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    private void sendAdminNotification(CommandSender sender, String targetPlayerName) {
        if (!(sender instanceof Player)) return;
        Player admin = (Player) sender;
        if (!plugin.getConfig().getBoolean("admin-notifications.enabled", true)) return;

        String soundName = plugin.getConfig().getString("admin-notifications.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        try {
            admin.playSound(admin.getLocation(), org.bukkit.Sound.valueOf(soundName), 1.0f, 1.0f);
        } catch (Exception ignored) {}

        String title = plugin.getConfig().getString("admin-notifications.title", "&aPlayer Approved");
        String subtitle = plugin.getConfig().getString("admin-notifications.subtitle", "&7%player% has been whitelisted")
                .replace("%player%", targetPlayerName);

        admin.sendTitle(
                translateColors(title),
                translateColors(subtitle),
                10, 40, 10
        );
    }
}
