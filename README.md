# PeyajWhitelist

A robust, crossplay-compatible whitelist plugin supporting Minecraft 1.13.x to 1.21.x (and older/newer versions), compiled for Java 17+. Supports Bedrock Edition players joining via GeyserMC/Floodgate.

## Features

- **Crossplay Support:** Automatic matching for Bedrock names, XUIDs, and Floodgate UUIDs.
- **Rejection Queue:** View rejected connections using `/pwl pending` and approve via `/pwl approve <index>`.
- **Discord Integration:** Asynchronous Webhooks with player skin head thumbnails, custom webhooks bot name, custom avatar URLs, and embed styling.
- **Bypass Integration:** Support for LuckPerms bypass via `peyajwhitelist.bypass` permission.
- **Audit Logging:** Detailed database modification log (`audit.log`) queryable directly in-game.
- **Maintenance Mode:** Lock connection gates for maintenance while allowing staff bypasses.
- **PlaceholdersAPI:** Native placeholders for scoreboard and tablist integration.
- **Vanilla Importer:** Fast one-command bulk transfer from standard Minecraft `whitelist.json`.
- **Auto-Backups:** Dynamic timestamped backups with automated folder cleanup (limits to 10 latest files).
- **Instant Kick:** Automatically kicks online players immediately when they are removed from the whitelist.
- **Whitelist Cap:** Enforce a strict database size limit (`max-whitelist-entries`) to prevent bloating.
- **Admin SFX & Titles:** Displays on-screen titles and plays sound cues for in-game staff upon approving players.
- **Self-Healing Configs:** Automatically backs up corrupted database files to prevent server crashes.

## Commands

All commands require the `peyajwhitelist.admin` permission (alias: `/pwl`):

- `/pwhitelist on` | `/pwhitelist off` - Toggle the whitelist.
- `/pwhitelist add` | `remove <name|uuid|xuid>` - Add or remove player entries.
- `/pwhitelist list` - Display all whitelisted players.
- `/pwhitelist pending` - List recent connection rejections.
- `/pwhitelist approve <index|name>` - Whitelist a pending connection.
- `/pwhitelist maintenance <on|off>` - Toggle server maintenance mode.
- `/pwhitelist stats` - View database counts and analytics.
- `/pwhitelist audit [player]` - Check database modification logs.
- `/pwhitelist import-vanilla` - Import players from vanilla `whitelist.json`.
- `/pwhitelist reload` - Reload config files.
- `/pwhitelist clear` - Clear the entire whitelist database.

## Placeholders

- `%peyajwhitelist_status%` - Enabled / Disabled status
- `%peyajwhitelist_count%` - Size of player database
- `%peyajwhitelist_pending_count%` - Current size of connection rejects queue
- `%peyajwhitelist_maintenance%` - Enabled / Disabled maintenance status

## License

Licensed under the **GNU General Public License v3.0**.
