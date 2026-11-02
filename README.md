# UpCoins

Server-wide currency for the UpDraft network. Players earn **UpCoins** by playing, keeping up a
login streak and winning events, then spend them on cosmetics and perks.

Works on **Paper / Purpur** (put the matching jar in every backend server's `plugins` folder) and
keeps every server in sync through one shared **MySQL** database, so it plays nicely behind
BungeeCord / Velocity without any proxy-side install.

## Builds

| Jar                  | For                        |
|----------------------|----------------------------|
| `UpCoins-1.21.11.jar`| Minecraft 1.21.11 (Java 21)|
| `UpCoins-1.21.4.jar` | Minecraft 1.21.4 (Java 21) |
| `UpCoins-26.3.jar`   | Minecraft 26.3 (Java 25)   |

```
mvn clean package -P1.21.11
mvn package -P1.21.4
mvn package -P26.3
```

Both jars land in `target/`. The only difference is the API version they are compiled against,
the config and commands are identical.

## Earning UpCoins

* **Playtime** – `rewards.playtime.amount` every `interval-minutes` while online.
* **Join streaks** – `rewards.streak.base` + `rewards.streak.per-day` per consecutive day, capped
  at `rewards.streak.max`.
* **Events** – hand out wins with `/upcoins event <player> [amount]` (default pays
  `rewards.event.amount` and broadcasts the winner).

## Commands

| Command | Permission | Description |
|---|---|---|
| `/upcoins` | `upcoins.balance` | Show your own balance |
| `/upcoins balance <player>` | `upcoins.balance.others` | Check someone's balance |
| `/upcoins give <player> <amount>` | `upcoins.give` | Add an exact amount |
| `/upcoins set <player> <amount>` | `upcoins.set` | Overwrite a balance |
| `/upcoins take <player> <amount>` | `upcoins.take` | Remove coins |
| `/upcoins event <player> [amount]` | `upcoins.event` | Pay an event winner |
| `/upcoins reload` | `upcoins.reload` | Reload config.yml |
| `/cosmetics [fire-trail\|wings]` | everyone | Buy / toggle cosmetics |

Aliases: `/uc`, `/coins` for `/upcoins`, `/cosmetic` for `/cosmetics`.

## Cosmetics

* **Fire Trail** (`cosmetics.fire-trail.price`, default 100) – flames and smoke follow the player while moving.
* **Wings** (`cosmetics.wings.price`, default 250) – an arch of end-rod particles behind the shoulders.

Once bought a cosmetic is permanent; `/cosmetics <name>` toggles it on or off.

## Perks

* **More homes in survival** – above `perks.extra-homes.minimum` (default 25) coins the player is
  granted `foxhomes.maxhomes.10`, i.e. **10 homes in FoxHomes** (raise the number in the config to
  give more). LuckPerms applies it the moment a balance crosses the threshold, no reload needed.
* **`[COIN COLLECTER]` tag** – above `perks.tag.minimum` (default 15) coins, players are added to
  the `coin-collector` LuckPerms group, which gets created automatically with the prefix from
  `perks.tag.prefix`. **TAB** and **LPC** read prefixes straight from LuckPerms, so the tag shows
  up in both with no extra setup (make sure both are configured to use LuckPerms as their source).
* Both fields are updated live whenever a balance changes, and removed again if someone drops
  below the threshold. Requires LuckPerms on the server.

## Storage

```yaml
storage:
  type: mysql        # mysql = shared database (recommended for networks), flatfile = data.yml
  mysql:
    host: 127.0.0.1
    port: 3306
    database: upcoins
    user: upcoins
    password: ""
```

The `upcoins_players` table is created automatically. Balances cache in memory and write back on
every change and on quit, so all servers behind the proxy stay consistent.
