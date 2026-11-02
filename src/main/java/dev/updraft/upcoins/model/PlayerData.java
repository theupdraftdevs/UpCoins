package dev.updraft.upcoins.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerData {

    public final UUID uuid;
    public String name;
    public long coins;
    public int streak;
    public LocalDate lastSeen;
    public final Set<String> cosmetics = new HashSet<>();

    public PlayerData(UUID uuid, String name, long coins, int streak, LocalDate lastSeen, Set<String> cosmetics) {
        this.uuid = uuid;
        this.name = name;
        this.coins = coins;
        this.streak = streak;
        this.lastSeen = lastSeen;
        this.cosmetics.addAll(cosmetics);
    }

    public boolean owns(String cosmeticId) {
        return cosmetics.contains(cosmeticId);
    }
}
