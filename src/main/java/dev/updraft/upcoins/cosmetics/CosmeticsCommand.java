package dev.updraft.upcoins.cosmetics;

import dev.updraft.upcoins.Msg;
import dev.updraft.upcoins.UpCoinsPlugin;
import dev.updraft.upcoins.model.PlayerData;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CosmeticsCommand implements CommandExecutor, TabCompleter {

    private final UpCoinsPlugin plugin;

    public CosmeticsCommand(UpCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cOnly players can use cosmetics.");
            return true;
        }
        if (args.length == 0) {
            listCosmetics(player);
            return true;
        }
        String id = args[0].toLowerCase(Locale.ROOT);
        if (!Cosmetics.IDS.contains(id)) {
            Msg.send(player, "&cUnknown cosmetic. Pick one: &e/cosmetics " + String.join(", ", Cosmetics.IDS));
            return true;
        }
        plugin.cosmetics().toggle(player, id);
        return true;
    }

    private void listCosmetics(Player player) {
        PlayerData data = plugin.coins().cached(player.getUniqueId());
        if (data == null) {
            Msg.send(player, "&7Balance still loading, try again in a second.");
            return;
        }
        Msg.send(player, "&fYou have &e" + data.coins + " &fUpCoins. Buy and toggle with /cosmetics <name>:");
        for (String id : Cosmetics.IDS) {
            long price = Math.max(0, plugin.getConfig().getLong("cosmetics." + id + ".price", 0));
            Set<String> active = plugin.cosmetics().activeOf(player.getUniqueId());
            String status = data.owns(id) ? (active.contains(id) ? "&aON" : "&7owned") : "&e" + price + " coins";
            player.sendMessage(Msg.fmt("&8- &e" + id + " &8(&f" + status + "&8)"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String id : Cosmetics.IDS) {
                if (id.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(id);
                }
            }
        }
        return out;
    }
}
