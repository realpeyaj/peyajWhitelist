# PeyajWhitelist

A robust, premium, and crossplay-compatible whitelist plugin for Minecraft server versions **1.21.x up to 26.2**. Designed specifically for servers running **GeyserMC** and **Floodgate** to handle Bedrock Edition players seamlessly alongside standard Java Edition players.

## Features

- **Crossplay Compatibility:** Interfaces with the Floodgate API to handle Bedrock players' Xbox Live Gamertags, XUIDs, and Floodgate-generated UUIDs.
- **Flexible Matching:** Automatically matches players by Java UUID, Bedrock XUID, exact name (case-insensitive), raw Bedrock name (without prefixes like `.`), or Java-mapped Bedrock name.
- **Prefix Auto-Detection:** Automatically strips prefixes (e.g. `.`) or replaces spaces with underscores to resolve common gamertag mismatch issues.
- **High Performance:** Loads and caches all whitelist entries in memory upon startup/reload. Connections are evaluated asynchronously (`AsyncPlayerPreLoginEvent`) to prevent blocking the main server thread.
- **Custom Kick Message:** Supports formatting with both legacy color codes (`&`) and modern Hex colors (`&#RRGGBB`).
- **Soft Dependency:** Runs completely fine even if Geyser/Floodgate is not installed (falls back to Java-only whitelist).

## Commands & Permissions

All commands require the permission node: `peyajwhitelist.admin`

- `/pwhitelist on` - Enables the whitelist.
- `/pwhitelist off` - Disables the whitelist.
- `/pwhitelist add <name|uuid|xuid>` - Adds an entry to the whitelist. (Detects format automatically).
- `/pwhitelist remove <name|uuid|xuid>` - Removes an entry from the whitelist.
- `/pwhitelist list` - Displays all whitelisted names, UUIDs, and XUIDs.
- `/pwhitelist reload` - Reloads configurations and players from the disk.
- `/pwhitelist clear` - Clears the entire whitelist (requires confirmation: `/pwhitelist clear confirm`).

## Configuration

### `config.yml`
```yaml
# Whether the whitelist is enabled on startup
enabled: true

# The message displayed to players who are not whitelisted.
# Supports legacy color codes (&) and hex color codes (&#FFFFFF)
kick-message: "&#ff4444&lPeyajWhitelist &7» &cYou are not whitelisted on this server!\n\n&7If you think this is a mistake, please contact the server administrator.\n&7Bedrock Players: Please provide your gamertag."

# If true, the plugin will allow Bedrock players to join if they are whitelisted
# either with or without their Floodgate prefix
auto-detect-bedrock-prefix: true

# Enable debug logging in the console
debug: false
```

### `whitelist.yml`
Saves whitelist data inside your plugin folder:
```yaml
names:
  - notch
  - bedrock_player
uuids:
  - 4f8263cf-2cf9-42b7-8739-cd59f3cb1d4f
xuids:
  - '25354123456789'
```

## Compilation

Build the plugin using Apache Maven:
```bash
mvn clean package
```
The compiled jar file will be generated in the `target/` directory: `peyajwhitelist-1.0.0.jar`.

## License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.
