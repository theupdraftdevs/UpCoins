package dev.updraft.upcoins.storage;

import dev.updraft.upcoins.model.PlayerData;

import java.util.UUID;

public interface Storage {

    /** Human friendly name, shown on startup. */
    String name();

    /** Load (creating on first sighting) the record for a player. */
    PlayerData load(UUID uuid, String name);

    /** Resolve a record by its latest known name, null when unknown. */
    PlayerData loadByName(String name);

    /** Persist the whole record. */
    void save(PlayerData data);

    void close();
}
