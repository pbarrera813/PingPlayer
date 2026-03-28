# Player Ping

Server-side Minecraft mod to check latency and IP addresses in-game. This project ports the original [PingPlayer](https://github.com/HoneyBerries/PingPlayer) plugin to modern mod loaders.

## Loaders

- Fabric (root project)
- NeoForge (`pingplayer-neoforge/`)

## What's New in 1.1.1

- Fixed Fabric server compatibility on Minecraft `1.21.11`
- Kept compatibility across the `1.21.x` line (validated on `1.21` and `1.21.11`)
- Fixed admin command detection for in-game operators
- Added admin compatibility for LuckPerms-style setups:
  - vanilla OP support
  - `playerping.admin` permission-node support (when permissions API is available)
  - minimum LuckPerms weight fallback (`minLuckPermsWeight`, default `8`)
- Added `/pingplayer debug` toggle for full server-side diagnostics
- Added `/pingplayer dump` to generate full diagnostic reports
- Updated command help styling for cleaner readability
- Added `/ip` (self lookup) for all players

## Features

- `/ping` latency check for all players
- `/ping <player>` latency check for admins
- `/ip` public IP lookup for all players
- `/ip <player>` public IP lookup for admins
- Color-coded ping quality in command output and tab list
- Threshold editing with overlap validation and live apply
- Full debug tracing mode (`/pingplayer debug`)
- Diagnostic dump generation (`/pingplayer dump`)
- Config stored at `config/player-ping/config.json` (with header comments)
- Dump reports stored at `config/player-ping/crash_reports/`
- Server-side only (no client mod required)

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ping` | Check your own ping | All players |
| `/ping <player>` | Check another player's ping | Admins |
| `/ip` | View your own public IP | All players |
| `/ip <player>` | View a player's public IP | Admins |
| `/pingplayer help` | Display admin command help | Admins |
| `/pingplayer debug` | Toggle full debug mode | Admins |
| `/pingplayer dump` | Generate a diagnostic dump file | Admins |
| `/pingplayer threshold <tier> equal-or-more <value>` | Set tier to >= value | Admins |
| `/pingplayer threshold <tier> equal-or-less <value>` | Set tier to <= value | Admins |
| `/pingplayer threshold <tier> equal <value>` | Set tier to exact value | Admins |
| `/pingplayer threshold <tier>` | Show current tier range | Admins |
| `/pingplayer threshold <tier> <min> <max>` | Set tier to interval | Admins |

`tier` values: `excellent`, `good`, `fair`, `poor`, `terrible`

Examples:

- `/pingplayer threshold terrible equal-or-more 500`
- `/pingplayer threshold fair 101 250`
- `/pingplayer debug`
- `/pingplayer dump`

Admin access notes:

- Admin checks accept vanilla OP users.
- On servers with a permissions stack, `playerping.admin` can grant access.
- LuckPerms role-weight fallback is supported through `minLuckPermsWeight` (default `8`).

## Default Thresholds

- excellent: `<= 50`
- good: `51 - 100`
- fair: `101 - 200`
- poor: `201 - 300`
- terrible: `>= 301`

## Build

### Fabric

```bash
gradlew build
```

Output:
- `build/libs/player-ping-1.1.1.jar`

### NeoForge

```bash
gradlew :pingplayer-neoforge:build
```

Output:
- `pingplayer-neoforge/build/libs/player-ping-neoforge-1.1.0.jar`

## Compatibility

- Minecraft: `1.21.x`
- Java: `21`
- Requires Fabric Loader and Fabric API on Fabric servers

## Credits

- HoneyBerries - original PingPlayer plugin creator.
- Phoenix_28 - Fabric, Forge, and NeoForge ports.