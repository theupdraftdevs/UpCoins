package dev.updraft.upcoins.coins;

import dev.updraft.upcoins.UpCoinsPlugin;
import dev.updraft.upcoins.model.PlayerData;
import dev.updraft.upcoins.storage.Storage;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CoinManager {

    public enum Action { GIVE, SET, TAKE }

    private final UpCoinsPlugin plugin;
    private final Storage storage;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public CoinManager(UpCoinsPlugin plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    /** Load a player into the cache; resolves on first join too. */
    public CompletableFuture<PlayerData> load(Player player) {
        return CompletableFuture.supplyAsync(
                        () -> storage.load(player.getUniqueId(), player.getName()))
                .thenApply(data -> {
                    cache.put(data.uuid, data);
                    return data;
                });
    }

    public PlayerData cached(UUID uuid) {
        return cache.get(uuid);
    }

    public void drop(UUID uuid) {
        cache.remove(uuid);
    }

    /** Apply the most common change: money in or out, clamped at zero. */
    public void add(Player player, long amount) {
        PlayerData data = cached(player.getUniqueId());
        if (data == null) {
            return;
        }
        data.coins = Math.max(0, data.coins + amount);
        persist(data);
        plugin.perks().refresh(data);
    }

    public void set(Player player, long amount) {
        PlayerData data = cached(player.getUniqueId());
        if (data == null) {
            return;
        }
        data.coins = Math.max(0, amount);
        persist(data);
        plugin.perks().refresh(data);
    }

    /** Offline-aware edit used by admin commands. */
    public CompletableFuture<Optional<PlayerData>> edit(String name, Action action, long amount, Player onlineTarget) {
        if (onlineTarget != null) {
            apply(onlineTarget, action, amount);
            return CompletableFuture.completedFuture(Optional.of(cached(onlineTarget.getUniqueId())));
        }
        return CompletableFuture.supplyAsync(() -> {
            PlayerData data = storage.loadByName(name);
            if (data == null) {
                return Optional.<PlayerData>empty();
            }
            switch (action) {
                case GIVE -> data.coins += amount;
                case SET -> data.coins = amount;
                case TAKE -> data.coins = Math.max(0, data.coins - amount);
            }
            storage.save(data);
            plugin.perks().refresh(data);
            return Optional.of(data);
        });
    }

    private void apply(Player target, Action action, long amount) {
        PlayerData data = cached(target.getUniqueId());
        if (data == null) {
            return;
        }
        switch (action) {
            case GIVE -> add(target, amount);
            case SET -> set(target, amount);
            case TAKE -> add(target, -amount);
        }
    }

    public void persist(PlayerData data) {
        CompletableFuture.runAsync(() -> storage.save(data));
    }

    /** Runs against its own connection, safe to call from async. */
    public Storage storage() {
        return storage;
    }
}
