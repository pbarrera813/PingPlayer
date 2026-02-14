# Player Ping (Fabric)

A server-side Fabric mod that lets players and admins check ping and IP addresses directly in-game. This is a recreation of the original [PingPlayer](https://github.com/HoneyBerries/PingPlayer) Paper/Bukkit plugin made by **HoneyBerries**, ported to Fabric to bring the same functionality to modded servers.

## Features

- Check your own or another player's ping
- View a player's IP address (operators only)
- Automatic color-coded ping display in the tab list
- Configurable ping thresholds via JSON config with hot-reload support
- Fully server-side — no client mod required

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ping` | Check your own ping | All players |
| `/ping <player>` | Check another player's ping | All players |
| `/ip <player>` | View a player's IP address | Operators (level 2) |
| `/pingplayer help` | Display all available commands | Operators (level 2) |
| `/pingplayer reload` | Reload the configuration file | Operators (level 2) |

## Ping Quality Indicators

Ping values are automatically color-coded for easy recognition:

| Color | Quality | Range |
|-------|---------|-------|
| Green | Excellent | 0–50 ms |
| Yellow | Good | 51–100 ms |
| Gold | Fair | 101–200 ms |
| Red | Poor | 201–300 ms |
| Dark Red | Terrible | 301+ ms |

These thresholds are configurable via the config file.

## Tab List

When enabled, the tab list displays each player's ping next to their name using the same color coding:

```
PlayerName [42 ms]
```

This updates in real time and can be toggled on or off in the configuration.

## Configuration

The config file is located at `config/player-ping/config.json` and is created automatically on first launch.

```json
{
  "pingThresholds": {
    "excellent": 50,
    "good": 100,
    "medium": 200,
    "bad": 300
  },
  "showPingOnTab": true
}
```

Changes can be applied without restarting the server using `/pingplayer reload`.

## Compatibility

- **Minecraft:** 1.21.x (all versions)
- **Mod Loader:** Fabric
- **Side:** Server-side only
- **Dependencies:** Fabric API

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) and [Fabric API](https://modrinth.com/mod/fabric-api) on your server.
2. Place the `player-ping-x.x.x.jar` file into the server's `mods` folder.
3. Start the server. The config file will be generated automatically.

## Credits

- **HoneyBerries** — Original [PingPlayer](https://github.com/HoneyBerries/PingPlayer) Paper plugin.
- **pbarrera813/Phoenix_28** — Fabric port.
