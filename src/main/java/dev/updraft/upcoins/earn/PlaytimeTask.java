package dev.updraft.upcoins.earn;

import dev.updraft.upcoins.Msg;
import dev.updraft.upcoins.UpCoinsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Pays out coins for staying online, every configured interval. */
public final class PlaytimeTask extends BukkitRunnable {

    private final UpCoinsPlugin plugin;
    private final Map<UUID, Integer> minutes = new ConcurrentHashMap<>();

    public PlaytimeTask(UpCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    public void forget(UUID uuid) {
        minutes.remove(uuid);
    }

    @Override
    public void run() {
        int step = 1;
        for (Player player : Bukkit.getOnlinePlayers()) {
            int count = minutes.merge(player.getUniqueId(), step, Integer::sum);
            int interval = Math.max(1, plugin.getConfig().getInt("rewards.playtime.interval-minutes", 10));
            if (count < interval) {
                continue;
            }
            minutes.put(player.getUniqueId(), count - interval);
            long amount = Math.max(0, plugin.getConfig().getLong("rewards.playtime.amount", 5));
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (amount <= 0) {
                    return;
                }
                plugin.coins().add(player, amount);
                Msg.send(player, "&7Playtime reward: &e" + amount + " &7UpCoins.");
            });
        }
    }
}
