# Player Ping

See how good every player's connection is, right from the game. Player Ping adds simple commands to check ping and IP addresses, shows each player's ping in the tab list, and works entirely on the server: nobody needs to install anything on their own computer.

This is a port of the original [PingPlayer](https://github.com/HoneyBerries/PingPlayer) plugin to Minecraft mod loaders.

## Download and compatibility

| Loader | Minecraft | Download | Needs |
|--------|-----------|----------|-------|
| Fabric | `1.21.x` | `player-ping-1.21.x-1.2.0.jar` | Java 21, Fabric Loader 0.18.4 or newer, Fabric API |
| Fabric | `26.1` - `26.3` | `player-ping-26.x-1.2.0.jar` | Java 25, Fabric Loader 0.20.1 or newer, Fabric API |
| NeoForge | WIP | | |

[Placeholder API](https://placeholders.pb4.eu/) is optional (see Placeholders).

## What's New in 1.2.0

- Works on Minecraft `26.1` - `26.3`, and still on every `1.21.x`
- More accurate ping: the server measures the connection itself once per second, so `/ping` now also shows jitter (how much the ping jumps around) and packet loss
- The tab list keeps team colors and prefixes instead of replacing the player's name
- The tab list refreshes once per second instead of 20 times per second, which is lighter on busy servers
- `/ping list` shows every online player's ping at once (admins)
- `/pingplayer reload` applies changes made to the config file without restarting
- Optional Placeholder API support, so holograms and similar mods can show a player's ping
- `/ip` now says "IP (as seen by the server)", for better clarification.

## Commands

| Command | What it does | Who can use it |
|---------|--------------|----------------|
| `/ping` | Check your own ping | Everyone |
| `/ping <player>` | Check another player's ping | Admins |
| `/ping list` | List every online player's ping, highest first (also works from the console) | Admins |
| `/ip` | Show your own IP as seen by the server | Everyone |
| `/ip <player>` | Show a player's IP as seen by the server | Admins |
| `/pingplayer help` | Show the admin help | Admins |
| `/pingplayer reload` | Reload the config file | Admins |
| `/pingplayer debug` | Turn detailed diagnostic logging on or off | Admins |
| `/pingplayer dump` | Save a diagnostic report to a file, useful when asking for help | Admins |
| `/pingplayer threshold <tier>` | Show a tier's range | Admins |
| `/pingplayer threshold <tier> equal-or-more <value>` | Tier applies from this ping and up | Admins |
| `/pingplayer threshold <tier> equal-or-less <value>` | Tier applies up to this ping | Admins |
| `/pingplayer threshold <tier> equal <value>` | Tier applies to exactly this ping | Admins |
| `/pingplayer threshold <tier> <min> <max>` | Tier applies between two pings | Admins |

Ping quality is split into five tiers: `excellent`, `good`, `fair`, `poor` and `terrible`. Each tier has its own color in chat and in the tab list. Changes apply immediately, and tiers cannot overlap.

Examples:

- `/pingplayer threshold terrible equal-or-more 500`
- `/pingplayer threshold fair 101 250`

### Default tiers

| Tier | Ping |
|------|------|
| excellent | 50 ms or less |
| good | 51 - 100 ms |
| fair | 101 - 200 ms |
| poor | 201 - 300 ms |
| terrible | 301 ms or more |

## Who counts as an admin

- Vanilla operators (OPs).
- Anyone with the permission `playerping.admin`, on servers with a permissions mod such as.
- Anyone whose LuckPerms group weight is at least `minLuckPermsWeight` (default `8`).

## Config

The settings are saved in `config/player-ping/config.json`, created the first time the server starts. Change the tiers in-game with `/pingplayer threshold`, which checks for overlaps. The other settings can be edited in the file, followed by `/pingplayer reload`. If the file has a mistake, the reload is refused and the current settings stay in place.

| Setting | Default | What it does |
|---------|---------|--------------|
| `showPingOnTab` | `true` | Show each player's ping in the tab list |
| `tabRefreshTicks` | `20` | How often the tab list updates, in ticks (20 ticks = 1 second, minimum 1) |
| `accuratePing` | `true` | Measure ping by sending each player a small packet every second. Set to `false` if an anti-cheat objects; the normal Minecraft value is used instead |
| `minLuckPermsWeight` | `8` | LuckPerms weight that counts as admin |
| `debugPermissionChecks` | `false` | Same as `/pingplayer debug` |

## Placeholders

If [Placeholder API](https://placeholders.pb4.eu/) is installed on your server, mods that support it (holograms, chat, tab list and similar) can show a player's ping:

| Placeholder | Example output |
|-------------|----------------|
| `%pingplayer:ping%` | `42 ms` (colored by tier) |
| `%pingplayer:quality%` | `excellent` (colored by tier) |

Example hologram text: `Welcome %player:displayname%, your ping is %pingplayer:ping%`. This should look like `Welcome Steve, your ping is 10 ms`.

Placeholder API and the mod that displays the text are separate downloads. Player Ping only provides the values.

## Building from source

You need JDK 25. From the project folder:

```bash
gradlew :test :build :fabric-legacy:build
```

This produces `player-ping-26.x-1.2.0.jar` and `player-ping-1.21.x-1.2.0.jar` (in the `build/libs` folder of the main project and of `fabric-legacy`).

## Credits

- HoneyBerries - original PingPlayer plugin creator.
- Phoenix_28 - Fabric, Forge, and NeoForge ports.
