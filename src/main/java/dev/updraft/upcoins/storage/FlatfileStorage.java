package dev.updraft.upcoins.storage;

import dev.updraft.upcoins.UpCoinsPlugin;
import dev.updraft.upcoins.model.PlayerData;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FlatfileStorage implements Storage {

    private final File file;
    private final YamlConfiguration yaml;

    public FlatfileStorage(UpCoinsPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    private ConfigurationSection section(UUID uuid) {
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) {
            root = yaml.createSection("players");
        }
        String key = uuid.toString();
        ConfigurationSection sec = root.getConfigurationSection(key);
        return sec != null ? sec : root.createSection(key);
    }

    private PlayerData read(ConfigurationSection sec) {
        UUID uuid = UUID.fromString(sec.getName());
        String lastSeen = sec.getString("last-seen");
        Set<String> owned = new HashSet<>(sec.getStringList("cosmetics"));
        return new PlayerData(uuid, sec.getString("name"), sec.getLong("coins", 0),
                sec.getInt("streak", 0), lastSeen == null ? null : LocalDate.parse(lastSeen), owned);
    }

    private void write(PlayerData data, ConfigurationSection sec) {
        sec.set("name", data.name);
        sec.set("coins", data.coins);
        sec.set("streak", data.streak);
        sec.set("last-seen", data.lastSeen == null ? null : data.lastSeen.toString());
        sec.set("cosmetics", new HashSet<>(data.cosmetics));
        flush();
    }

    private void flush() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            throw new StorageException("data.yml write failed", e);
        }
    }

    @Override
    public String name() {
        return "flatfile (data.yml)";
    }

    @Override
    public PlayerData load(UUID uuid, String name) {
        ConfigurationSection sec = section(uuid);
        PlayerData data = read(sec);
        data.name = name;
        write(data, sec);
        return data;
    }

    @Override
    public PlayerData loadByName(String name) {
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return null;
        }
        for (String key : players.getKeys(false)) {
            if (name.equalsIgnoreCase(players.getString(key + ".name"))) {
                return read(players.getConfigurationSection(key));
            }
        }
        return null;
    }

    @Override
    public void save(PlayerData data) {
        write(data, section(data.uuid));
    }

    @Override
    public void close() {
        flush();
    }
}
