# Player Ping

Server-side Minecraft mod to check latency and IP addresses in-game. This project ports the original [PingPlayer](https://github.com/HoneyBerries/PingPlayer) plugin to modern mod loaders.

## Loaders

- Fabric (root project)
- NeoForge (`pingplayer-neoforge/`)

## What's New in 1.1.0

- Added operator threshold management command: `/pingplayer threshold ...`
- Added chat-safe comparator keywords: `equal-or-more`, `equal-or-less`, `equal`
- Threshold updates apply immediately (no reload command required)
- Added tab-completion and suggestion hints for threshold editing
- Added overlap validation so two tiers cannot share the same range
- Added legacy-config migration support (`medium`/`bad` -> `fair`/`poor`)

## Features

- `/ping` and `/ping <player>` latency checks
- `/ip <player>` for operators
- Color-coded ping quality in command output and tab list
- Config stored at `config/player-ping/config.json`
- Server-side only (no client mod required)

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ping` | Check your own ping | All players |
| `/ping <player>` | Check another player's ping | All players |
| `/ip <player>` | View a player's IP address | Operators (level 2) |
| `/pingplayer help` | Display help | Operators (level 2) |
| `/pingplayer threshold <tier> equal-or-more <value>` | Set tier to >= value | Operators (level 2) |
| `/pingplayer threshold <tier> equal-or-less <value>` | Set tier to <= value | Operators (level 2) |
| `/pingplayer threshold <tier> equal <value>` | Set tier to exact value | Operators (level 2) |
| `/pingplayer threshold <tier>` | Show current tier range | Operators (level 2) |
| `/pingplayer threshold <tier> <min> <max>` | Set tier to interval | Operators (level 2) |

`tier` values: `excellent`, `good`, `fair`, `poor`, `terrible`

Examples:

- `/pingplayer threshold terrible equal-or-more 500`
- `/pingplayer threshold fair 101 250`

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
- `build/libs/player-ping-1.1.0.jar`

### NeoForge

```bash
gradlew :pingplayer-neoforge:build
```

Output:
- `pingplayer-neoforge/build/libs/player-ping-neoforge-1.1.0.jar`

## Compatibility

- Minecraft: `1.21.x`
- Java: `21`

## Credits

- HoneyBerries - original PingPlayer plugin creator.
- Phoenix_28 - Fabric, Forge, and NeoForge ports.
