# PeyajWhitelist

A robust, crossplay-compatible whitelist plugin for Minecraft 1.21.x to 26.2. Supports Bedrock Edition players joining via GeyserMC/Floodgate.

## Features

- **Crossplay Support:** Automatic matching for Bedrock names, XUIDs, and Floodgate UUIDs.
- **Rejection Queue:** View rejected connections using `/pwl pending` and approve via `/pwl approve <index>`.
- **Discord Integration:** Asynchronous Webhook notifications with player head thumbnails.
- **Bypass Integration:** Support for LuckPerms bypass via `peyajwhitelist.bypass` permission.
- **Prefix Handling:** Automatic stripping of Bedrock prefixes (e.g. `.`) and spacing conversions.
- **Self-Healing Configs:** Automatically backs up corrupted player files to prevent server crashes.

## Commands

All commands require the `peyajwhitelist.admin` permission (alias: `/pwl`):

- `/pwhitelist on` | `/pwhitelist off` - Toggle the whitelist.
- `/pwhitelist add` | `remove <name|uuid|xuid>` - Add or remove player entries.
- `/pwhitelist list` - Display all whitelisted players.
- `/pwhitelist pending` - List recent connection rejections.
- `/pwhitelist approve <index|name>` - Whitelist a pending connection.
- `/pwhitelist reload` - Reload config files.
- `/pwhitelist clear` - Clear the entire whitelist database.

## License

Licensed under the **GNU General Public License v3.0**.
