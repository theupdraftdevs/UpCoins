package dev.updraft.upcoins.cosmetics;

import dev.updraft.upcoins.Msg;
import dev.updraft.upcoins.UpCoinsPlugin;
import dev.updraft.upcoins.model.PlayerData;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Cosmetics {

    public static final List<String> IDS = List.of("fire-trail", "wings");

    private final UpCoinsPlugin plugin;
    private final Map<UUID, Set<String>> active = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastPos = new ConcurrentHashMap<>();

    public Cosmetics(UpCoinsPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickFireTrail, 2L, 2L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickWings, 2L, 2L);
    }

    public Set<String> activeOf(UUID uuid) {
        return active.computeIfAbsent(uuid, id -> ConcurrentHashMap.newKeySet());
    }

    public void clear(UUID uuid) {
        active.remove(uuid);
        lastPos.remove(uuid);
    }

    public boolean toggle(Player player, String id) {
        PlayerData data = plugin.coins().cached(player.getUniqueId());
        if (data == null) {
            Msg.send(player, "&7Balance still loading, try again in a second.");
            return false;
        }
        double price = Math.max(0, plugin.getConfig().getDouble("cosmetics." + id + ".price", 0));
        if (!data.owns(id)) {
            if (data.coins < price) {
                Msg.send(player, "&cYou need &e" + (long) (price - data.coins)
                        + " &cmore UpCoins for &e" + id + "&c (" + (long) price + ").");
                return false;
            }
            data.coins -= (long) price;
            data.cosmetics.add(id);
            plugin.coins().persist(data);
            plugin.perks().refresh(data);
            activeOf(player.getUniqueId()).add(id);
            Msg.send(player, "&aBought &e" + title(id) + " &afor &e" + (long) price
                    + " &aUpCoins and turned it on!");
            return true;
        }
        Set<String> ids = activeOf(player.getUniqueId());
        if (ids.remove(id)) {
            Msg.send(player, "&e" + title(id) + " &7turned off.");
        } else {
            ids.add(id);
            Msg.send(player, "&e" + title(id) + " &7turned on.");
        }
        return true;
    }

    private String title(String id) {
        return switch (id) {
            case "fire-trail" -> "Fire Trail";
            case "wings" -> "Wings";
            default -> id;
        };
    }

    private void tickFireTrail() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!activeOf(p.getUniqueId()).contains("fire-trail")) {
                continue;
            }
            Location loc = p.getLocation();
            Location last = lastPos.put(p.getUniqueId(), loc.clone());
            if (last == null || last.distanceSquared(loc) < 0.02) {
                continue;
            }
            World world = p.getWorld();
            world.spawnParticle(Particle.FLAME, loc.clone().add(0, 0.15, 0), 4, 0.2, 0.05, 0.2, 0.005);
            world.spawnParticle(Particle.SMOKE, loc, 2, 0.3, 0.1, 0.3, 0.01);
        }
    }

    private void tickWings() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!activeOf(p.getUniqueId()).contains("wings")) {
                continue;
            }
            Location anchor = p.getLocation().add(0, 1.25, 0);
            Vector forward = anchor.getDirection().setY(0);
            if (forward.lengthSquared() < 0.001) {
                continue;
            }
            forward.normalize();
            Vector back = forward.clone().multiply(-0.45);
            Vector right = new Vector(forward.getZ(), 0, -forward.getX());
            anchor = anchor.add(back);
            for (int c = -3; c <= 3; c++) {
                double arc = Math.sqrt(9.0 - c * c) * 0.15;
                Location point = anchor.clone().add(right.clone().multiply(0.18 * c)).add(0, arc, 0);
                p.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
                if (Math.abs(c) == 3) {
                    point = anchor.clone().add(right.clone().multiply(0.22 * c)).add(0, arc - 0.3, 0);
                    p.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
                }
            }
        }
    }
}
