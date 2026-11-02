package dev.updraft.upcoins.perks;

import dev.updraft.upcoins.UpCoinsPlugin;
import dev.updraft.upcoins.model.PlayerData;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PerkManager {

    private final UpCoinsPlugin plugin;
    private final Map<UUID, long[]> lastState = new ConcurrentHashMap<>();

    public PerkManager(UpCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Applies the [COIN COLLECTER] tag and the extra-homes permission once the thresholds are crossed. */
    public void refresh(PlayerData data) {
        if (!luckPermsAvailable()) {
            return;
        }
        boolean wantsTag = plugin.getConfig().getBoolean("perks.tag.enabled")
                && data.coins > plugin.getConfig().getLong("perks.tag.minimum", 15);
        boolean wantsHomes = plugin.getConfig().getBoolean("perks.extra-homes.enabled")
                && data.coins > plugin.getConfig().getLong("perks.extra-homes.minimum", 25);

        long[] prev = lastState.get(data.uuid);
        if (prev != null && prev[0] == (wantsTag ? 1 : 0) && prev[1] == (wantsHomes ? 1 : 0)) {
            return;
        }
        lastState.put(data.uuid, new long[]{wantsTag ? 1 : 0, wantsHomes ? 1 : 0});

        setupTagGroup();
        String homesPermission = plugin.getConfig().getString("perks.extra-homes.permission",
                "essentials.sethome.multiple.upcoins");
        String tagGroup = plugin.getConfig().getString("perks.tag.group", "coin-collector");

        Node tag = Node.builder("group." + tagGroup).value(true).build();
        Node homes = Node.builder(homesPermission).value(true).build();
        LuckPermsProvider.get().getUserManager().modifyUser(data.uuid, user -> {
            user.data().remove(tag);
            user.data().remove(homes);
            if (wantsTag) {
                user.data().add(tag);
            }
            if (wantsHomes) {
                user.data().add(homes);
            }
        });
    }

    /** Creates the tag group with its prefix once, so TAB and LPC can display it. */
    public void setupTagGroup() {
        if (!luckPermsAvailable() || !plugin.getConfig().getBoolean("perks.tag.enabled")) {
            return;
        }
        String group = plugin.getConfig().getString("perks.tag.group", "coin-collector");
        String prefix = plugin.getConfig().getString("perks.tag.prefix", "&8[&6COIN COLLECTER&8]");
        GroupManager manager = LuckPermsProvider.get().getGroupManager();
        Group existing = manager.getGroup(group);
        if (existing != null && existing.getCachedData().getMetaData().getPrefix() != null) {
            return;
        }
        if (existing == null) {
            manager.createAndLoadGroup(group).join();
            existing = manager.getGroup(group);
            if (existing == null) {
                return;
            }
        }
        manager.modifyGroup(group, g -> {
            g.data().clear(node -> node.getType() == NodeType.PREFIX);
            g.data().add(PrefixNode.builder(prefix, 100).build());
        });
    }

    private boolean luckPermsAvailable() {
        try {
            LuckPermsProvider.get();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
