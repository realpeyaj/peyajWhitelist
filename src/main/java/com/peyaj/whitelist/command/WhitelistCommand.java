package com.peyaj.whitelist.command;

import com.peyaj.whitelist.PeyajWhitelist;
import com.peyaj.whitelist.manager.WhitelistManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

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
                    plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(PREFIX + result));
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
                    plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(PREFIX + result));
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
                sender.sendMessage(PREFIX + "§aSuccessfully cleared all entries from the whitelist.");
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
        sender.sendMessage("§e/" + label + " clear §7- Clear the entire whitelist");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("peyajwhitelist.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("on", "off", "add", "remove", "list", "reload", "clear");
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
            } else if (subCommand.equals("clear")) {
                StringUtil.copyPartialMatches(args[1], Collections.singletonList("confirm"), completions);
                return completions;
            }
        }

        return Collections.emptyList();
    }
}
