package dev.updraft.upcoins;

import dev.updraft.upcoins.model.PlayerData;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDate;
import java.time.ZoneId;

public final class PlayerListener implements Listener {

    private final UpCoinsPlugin plugin;

    public PlayerListener(UpCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.coins().load(player).thenAccept(data ->
                plugin.getServer().getScheduler().runTask(plugin, () -> handleJoined(player, data)));
    }

    private void handleJoined(Player player, PlayerData data) {
        plugin.cosmetics().clear(player.getUniqueId());
        if (!player.isOnline()) {
            return;
        }
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        if (data.lastSeen == null) {
            data.streak = 1;
        } else if (data.lastSeen.equals(today)) {
            return; // already counted today
        } else if (data.lastSeen.plusDays(1).equals(today)) {
            data.streak++;
        } else {
            data.streak = 1;
        }
        data.lastSeen = today;

        int base = Math.max(0, plugin.getConfig().getInt("rewards.streak.base", 10));
        int perDay = Math.max(0, plugin.getConfig().getInt("rewards.streak.per-day", 5));
        int cap = Math.max(base, plugin.getConfig().getInt("rewards.streak.max", 50));
        long reward = Math.min(base + perDay * Math.max(0, data.streak - 1), cap);
        data.coins += reward;
        plugin.coins().persist(data);
        plugin.perks().refresh(data);
        Msg.send(player, "&7Day &e" + data.streak + " &7streak! You got &e" + reward + " &7UpCoins.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.cosmetics().clear(player.getUniqueId());
        plugin.playtime().forget(player.getUniqueId());
        PlayerData data = plugin.coins().cached(player.getUniqueId());
        if (data != null) {
            plugin.coins().persist(data);
        }
        plugin.coins().drop(player.getUniqueId());
    }
}
