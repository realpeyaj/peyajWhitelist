# PeyajWhitelist

A robust, crossplay-compatible whitelist plugin for Minecraft 1.21.x to 26.2. Supports Bedrock Edition players joining via GeyserMC/Floodgate.

## Features

- **Crossplay Support:** Automatic matching for Bedrock names, XUIDs, and Floodgate UUIDs.
- **Rejection Queue:** View rejected connections using `/pwl pending` and approve via `/pwl approve <index>`.
- **Discord Integration:** Asynchronous Webhooks with player head thumbnails and customizable embed styling.
- **Bypass Integration:** Support for LuckPerms bypass via `peyajwhitelist.bypass` permission.
- **Audit Logging:** Detailed database modification log (`audit.log`) queryable directly in-game.
- **Maintenance Mode:** Lock connection gates for maintenance while allowing staff bypasses.
- **PlaceholdersAPI:** Native placeholders for scoreboard and tablist integration.
- **Vanilla Importer:** Fast one-command bulk transfer from standard Minecraft `whitelist.json`.
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
