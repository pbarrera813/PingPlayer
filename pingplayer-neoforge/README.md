# Player Ping (NeoForge)

NeoForge edition of Player Ping for Minecraft 1.21.x.

## What's New in 1.1.0

- Added `/pingplayer threshold` command family for operator-managed ranges
- Replaced symbol comparators with text comparators:
  - `equal-or-more`
  - `equal-or-less`
  - `equal`
- Threshold changes apply immediately and persist to disk
- Added overlap validation across tiers
- Added command suggestions/tab-completion for faster configuration

## Features

- `/ping` and `/ping <player>` latency checks
- `/ip <player>` for operators
- Color-coded tab-list ping display
- Runtime threshold updates via commands
- Server-side only

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ping` | Check your own ping | All players |
| `/ping <player>` | Check another player's ping | All players |
| `/ip <player>` | View a player's IP address | Operators (level 2) |
| `/pingplayer help` | Display help | Operators (level 2) |
| `/pingplayer threshold <tier>` | Show current tier range | Operators (level 2) |
| `/pingplayer threshold <tier> equal-or-more <value>` | Set tier to >= value | Operators (level 2) |
| `/pingplayer threshold <tier> equal-or-less <value>` | Set tier to <= value | Operators (level 2) |
| `/pingplayer threshold <tier> equal <value>` | Set tier to exact value | Operators (level 2) |
| `/pingplayer threshold <tier> <min> <max>` | Set tier to interval | Operators (level 2) |

`tier` values: `excellent`, `good`, `fair`, `poor`, `terrible`

Examples:

- `/pingplayer threshold terrible equal-or-more 500`
- `/pingplayer threshold fair 101 250`

## Configuration

File path:
- `config/player-ping/config.json`

Default structure:

```json
{
  "pingThresholds": {
    "excellent": { "max": 50 },
    "good": { "min": 51, "max": 100 },
    "fair": { "min": 101, "max": 200 },
    "poor": { "min": 201, "max": 300 },
    "terrible": { "min": 301 }
  },
  "showPingOnTab": true
}
```

## Build

From project root:

```bash
gradlew :pingplayer-neoforge:build
```

Output:
- `pingplayer-neoforge/build/libs/player-ping-neoforge-1.1.0.jar`

## Installation

1. Install NeoForge for Minecraft 1.21.x
2. Copy `player-ping-neoforge-1.1.0.jar` to server `mods/`
3. Start server

## Compatibility

- Minecraft: `1.21.x`
- NeoForge: `21.x`
- Java: `21`

## Credits

- HoneyBerries - original PingPlayer plugin
- Phoenix_28 - Fabric, Forge, and NeoForge ports
