package dev.updraft.upcoins;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.command.CommandSender;

public final class Msg {

    public static final String PREFIX = "&8[&6UpCoins&8] &f";

    private Msg() {
    }

    public static String fmt(String raw) {
        return ChatColor.translateAlternateColorCodes('&', PREFIX + raw);
    }

    public static void send(CommandSender to, String raw) {
        to.sendMessage(fmt(raw));
    }
}
