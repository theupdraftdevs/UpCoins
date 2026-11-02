package dev.updraft.upcoins.coins;

import dev.updraft.upcoins.Msg;
import dev.updraft.upcoins.UpCoinsPlugin;
import dev.updraft.upcoins.coins.CoinManager.Action;
import dev.updraft.upcoins.model.PlayerData;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;

public final class UpCoinsCommand implements CommandExecutor {

    private final UpCoinsPlugin plugin;

    public UpCoinsCommand(UpCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("balance")) {
            return balance(sender, args);
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give", "set", "take" -> adminEdit(sender, args);
            case "event" -> eventReward(sender, args);
            case "reload" -> reload(sender);
            default -> balance(sender, args);
        }
        return true;
    }

    private boolean balance(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("balance")) {
            if (!sender.hasPermission("upcoins.balance.others")) {
                Msg.send(sender, "&cYou can only check your own balance.");
                return true;
            }
            showOther(sender, args[1]);
            return true;
        }
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cUse /upcoins balance <player> from console.");
            return true;
        }
        PlayerData data = plugin.coins().cached(player.getUniqueId());
        long shown = data == null ? 0 : data.coins;
        Msg.send(player, "&fYou have &e" + shown + " &fUpCoins.");
        Msg.send(player, "&7Use /cosmetics to spend them on trails, wings and perks.");
        return true;
    }

    private void showOther(CommandSender sender, String name) {
        Player target = plugin.getServer().getPlayerExact(name);
        if (target != null) {
            PlayerData data = plugin.coins().cached(target.getUniqueId());
            Msg.send(sender, "&e" + target.getName() + "&f has &e" + (data == null ? 0 : data.coins) + " &fUpCoins.");
            return;
        }
        plugin.coins().edit(name, Action.GIVE, 0, null).thenAccept(result ->
                printLookup(sender, name, result));
    }

    private void printLookup(CommandSender sender, String name, Optional<PlayerData> result) {
        PlayerData data = result.orElse(null);
        if (data == null) {
            Msg.send(sender, "&cNo UpCoins record found for &e" + name + "&c.");
        } else {
            Msg.send(sender, "&e" + data.name + "&f has &e" + data.coins + " &fUpCoins.");
        }
    }

    private void adminEdit(CommandSender sender, String[] args) {
        Action action = Action.valueOf(args[0].toUpperCase(Locale.ROOT));
        if (!sender.hasPermission("upcoins." + action.name().toLowerCase(Locale.ROOT))) {
            Msg.send(sender, "&cMissing permission upcoins." + action.name().toLowerCase(Locale.ROOT) + ".");
            return;
        }
        if (args.length < 3) {
            Msg.send(sender, "&cUsage: /upcoins " + action.name().toLowerCase(Locale.ROOT) + " <player> <amount>");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            Msg.send(sender, "&cAmount must be a whole number.");
            return;
        }
        if (amount <= 0 || (action != Action.GIVE && amount > 1_000_000_000)) {
            Msg.send(sender, "&cAmount must be positive.");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        String name = target != null ? target.getName() : args[1];
        plugin.coins().edit(name, action, amount, target).thenAccept(result -> {
            PlayerData data = result.orElse(null);
            if (data == null) {
                Msg.send(sender, "&cNo UpCoins record found for &e" + name + "&c.");
                return;
            }
            switch (action) {
                case GIVE -> Msg.send(sender, "&aGave &e" + amount + " &aUpCoins to &e" + data.name + "&a. New total: &e" + data.coins + "&a.");
                case SET -> Msg.send(sender, "&aSet &e" + data.name + "&a's balance to &e" + amount + "&a.");
                case TAKE -> Msg.send(sender, "&aTook &e" + amount + " &aUpCoins from &e" + data.name + "&a. New total: &e" + data.coins + "&a.");
            }
            if (target != null) {
                Msg.send(target, "&fYour UpCoins were updated: &e" + data.coins + "&f.");
            }
        });
    }

    private void eventReward(CommandSender sender, String[] args) {
        if (!sender.hasPermission("upcoins.event")) {
            Msg.send(sender, "&cMissing permission upcoins.event.");
            return;
        }
        if (args.length < 2) {
            Msg.send(sender, "&cUsage: /upcoins event <player> [amount]");
            return;
        }
        long amount = args.length >= 3 ? parseAmount(sender, args[2])
                : Math.max(0, plugin.getConfig().getLong("rewards.event.amount", 50));
        if (amount <= 0) {
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        String name = target != null ? target.getName() : args[1];
        plugin.coins().edit(name, Action.GIVE, amount, target).thenAccept(result -> {
            PlayerData data = result.orElse(null);
            if (data == null) {
                Msg.send(sender, "&cNo UpCoins record found for &e" + name + "&c.");
                return;
            }
            Msg.send(sender, "&aPaid &e" + amount + " &aUpCoins to &e" + data.name + "&a for winning the event.");
            plugin.getServer().broadcastMessage(Msg.fmt("&e" + data.name + " &fwon the event and got &e"
                    + amount + " &fUpCoins!"));
        });
    }

    private long parseAmount(CommandSender sender, String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            Msg.send(sender, "&cAmount must be a whole number.");
            return -1;
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("upcoins.reload")) {
            Msg.send(sender, "&cMissing permission upcoins.reload.");
            return;
        }
        plugin.reloadConfig();
        plugin.perks().setupTagGroup();
        Msg.send(sender, "&aUpCoins config reloaded.");
    }
}
