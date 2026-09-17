# MaceSMP

A Paper/Spigot plugin (1.20.x, Java 17+) that adds:

- **Cores currency** — a per-player balance tracked and saved in `plugins/MaceSMP/cores.yml`.
- **PvP kill rewards** — killing another player gives **10 Cores** by default (configurable).
- **Core Shop** — a category → item GUI shop, similar in structure to ShopGUIPlus, fully configured in `shop.yml`.
- **Live scoreboard** — a built-in sidebar scoreboard showing each player's exact Cores and Kills, updated every second.
- **PlaceholderAPI hook** — `%macesmp_cores%` and `%macesmp_kills%` are auto-registered if PlaceholderAPI is installed, so if you're already running a different scoreboard plugin (FeatureAnimations, AnimatedScoreboard, etc.) you can point it at these placeholders instead and it will always read the correct live value.

## ⚠️ Important — build it yourself

I wrote and reviewed all the code carefully, What you're getting is complete, ready-to-build source code, not a pre-tested binary. Please build and test it on your own server before relying on it. If anything doesn't compile or behave as expected, send me the exact error and I'll fix it immediately.

## How to build

You need **Java 17+** and **Maven** installed, plus an internet connection (Maven needs to download the Paper API once).

```bash
cd mace-smp
mvn clean package
```

This produces `target/MaceSMP.jar`. Drop that file into your server's `plugins/` folder and restart (or `/reload` — a restart is safer).

If you don't use PlaceholderAPI, that's fine — it's a *soft* dependency, the plugin works without it and the built-in scoreboard handles everything on its own.

## Commands & permissions

| Command | Description | Permission |
|---|---|---|
| `/cores` | Check your own Cores balance | `macesmp.cores` (default: true) |
| `/cores balance <player>` | Check someone else's balance | `macesmp.cores` |
| `/cores give <player> <amount>` | Give Cores | `macesmp.admin` (default: op) |
| `/cores set <player> <amount>` | Set a balance exactly | `macesmp.admin` |
| `/cores reload` | Reload config.yml + shop.yml | `macesmp.admin` |
| `/coreshop` | Open the shop GUI | `macesmp.shop` (default: true) |
| `/scoreboard` | Toggle the built-in scoreboard on/off for yourself | none |

## Configuring the shop

Edit `plugins/MaceSMP/shop.yml`. Each category becomes a clickable icon in `/coreshop`; each item inside it becomes a purchasable icon in that category's page. You can either have MaceSMP hand the item straight into the buyer's inventory, or set a `command:` line (e.g. an EssentialsX kit, a rank command, etc.) to run instead — `{player}` is replaced with the buyer's name.

## Configuring cores/kills/messages/scoreboard

Everything else lives in `plugins/MaceSMP/config.yml` — the kill reward amount, whether victims lose Cores, all chat messages (color codes with `&`), and the scoreboard title/lines/update speed.

## Notes on "linking to the scoreboard"

I built in a working sidebar scoreboard so this works out of the box with zero extra plugins. If you actually meant you already have a separate scoreboard plugin and want *that one* to show Cores correctly, use the PlaceholderAPI placeholders above in that plugin's config instead of (or in addition to) the built-in one — just set `scoreboard.enabled: false` in config.yml to turn the built-in one off.
