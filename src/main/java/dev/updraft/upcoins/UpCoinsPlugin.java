package dev.updraft.upcoins;

import dev.updraft.upcoins.coins.CoinManager;
import dev.updraft.upcoins.coins.UpCoinsCommand;
import dev.updraft.upcoins.cosmetics.Cosmetics;
import dev.updraft.upcoins.cosmetics.CosmeticsCommand;
import dev.updraft.upcoins.earn.PlaytimeTask;
import dev.updraft.upcoins.perks.PerkManager;
import dev.updraft.upcoins.storage.FlatfileStorage;
import dev.updraft.upcoins.storage.MySqlStorage;
import dev.updraft.upcoins.storage.Storage;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class UpCoinsPlugin extends JavaPlugin {

    private Storage storage;
    private CoinManager coins;
    private PerkManager perks;
    private Cosmetics cosmetics;
    private PlaytimeTask playtime;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Not touched async: the cosmetics tasks need the scheduler set up first.
        this.storage = createStorage();
        this.coins = new CoinManager(this, storage);
        this.perks = new PerkManager(this);
        this.cosmetics = new Cosmetics(this);
        this.playtime = new PlaytimeTask(this);

        playtime.runTaskTimerAsynchronously(this, 20L * 60, 20L * 60);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        register("upcoins", new UpCoinsCommand(this));
        register("cosmetics", new CosmeticsCommand(this));

        perks.setupTagGroup();
        getLogger().info("UpCoins enabled, using " + storage.name() + ".");
    }

    private Storage createStorage() {
        if ("mysql".equalsIgnoreCase(getConfig().getString("storage.type", "flatfile"))) {
            try {
                return new MySqlStorage(
                        getConfig().getString("storage.mysql.host", "127.0.0.1"),
                        getConfig().getInt("storage.mysql.port", 3306),
                        getConfig().getString("storage.mysql.database", "upcoins"),
                        getConfig().getString("storage.mysql.user", "upcoins"),
                        getConfig().getString("storage.mysql.password", ""));
            } catch (SQLException e) {
                getLogger().severe("Could not reach MySQL, falling back to data.yml: " + e.getMessage());
            }
        }
        return new FlatfileStorage(this);
    }

    private void register(String name, Object executor) {
        PluginCommand command = getCommand(name);
        if (command != null && executor instanceof org.bukkit.command.CommandExecutor ex) {
            command.setExecutor(ex);
            if (executor instanceof org.bukkit.command.TabCompleter tab) {
                command.setTabCompleter(tab);
            }
        }
    }

    @Override
    public void onDisable() {
        if (playtime != null) {
            playtime.cancel();
        }
        if (storage != null) {
            storage.close();
        }
    }

    public CoinManager coins() {
        return coins;
    }

    public PerkManager perks() {
        return perks;
    }

    public Cosmetics cosmetics() {
        return cosmetics;
    }

    public PlaytimeTask playtime() {
        return playtime;
    }
}
