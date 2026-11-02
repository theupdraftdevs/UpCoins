package dev.updraft.upcoins.storage;

import dev.updraft.upcoins.model.PlayerData;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MySqlStorage implements Storage {

    private final String url;
    private final String user;
    private final String password;
    private Connection connection;

    public MySqlStorage(String host, int port, String database, String user, String password) throws SQLException {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?autoReconnect=false&useSSL=false&characterEncoding=utf8";
        this.user = user;
        this.password = password;
        createTable();
    }

    private Connection conn() throws SQLException {
        if (connection == null || !connection.isValid(3)) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    private void createTable() throws SQLException {
        try (Statement st = conn().createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS upcoins_players ("
                    + "uuid CHAR(36) PRIMARY KEY,"
                    + "name VARCHAR(16) NOT NULL,"
                    + "coins INT NOT NULL DEFAULT 0,"
                    + "streak INT NOT NULL DEFAULT 0,"
                    + "last_seen VARCHAR(10),"
                    + "cosmetics VARCHAR(64) NOT NULL DEFAULT '',"
                    + "INDEX upcoins_name (name))");
        }
    }

    @Override
    public synchronized String name() {
        return "MySQL";
    }

    @Override
    public synchronized PlayerData load(UUID uuid, String name) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT name, coins, streak, last_seen, cosmetics FROM upcoins_players WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlayerData data = row(uuid, rs);
                    updateName(data);
                    return data;
                }
            }
            try (PreparedStatement insert = conn().prepareStatement(
                    "INSERT IGNORE INTO upcoins_players (uuid, name) VALUES (?,?)")) {
                insert.setString(1, uuid.toString());
                insert.setString(2, name);
                insert.executeUpdate();
            }
            return new PlayerData(uuid, name, 0, 0, null, new HashSet<>());
        } catch (SQLException e) {
            throw new StorageException("MySQL read failed", e);
        }
    }

    @Override
    public synchronized PlayerData loadByName(String name) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT uuid, coins, streak, last_seen, cosmetics FROM upcoins_players "
                        + "WHERE name=? ORDER BY last_seen DESC LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return row(UUID.fromString(rs.getString(1)), rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new StorageException("MySQL read failed", e);
        }
    }

    private PlayerData row(UUID uuid, ResultSet rs) throws SQLException {
        String lastSeen = rs.getString(4);
        Set<String> cosmetics = new HashSet<>();
        String owned = rs.getString(5);
        if (owned != null && !owned.isEmpty()) {
            for (String id : owned.split(",")) {
                cosmetics.add(id);
            }
        }
        return new PlayerData(uuid, rs.getString(1), rs.getInt(2), rs.getInt(3),
                lastSeen == null ? null : LocalDate.parse(lastSeen), cosmetics);
    }

    private void updateName(PlayerData data) {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE upcoins_players SET name=? WHERE uuid=?")) {
            ps.setString(1, data.name);
            ps.setString(2, data.uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("MySQL write failed", e);
        }
    }

    @Override
    public synchronized void save(PlayerData data) {
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT INTO upcoins_players (uuid, name, coins, streak, last_seen, cosmetics) VALUES (?,?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE name=VALUES(name), coins=VALUES(coins), "
                        + "streak=VALUES(streak), last_seen=VALUES(last_seen), cosmetics=VALUES(cosmetics)")) {
            ps.setString(1, data.uuid.toString());
            ps.setString(2, data.name);
            ps.setLong(3, data.coins);
            ps.setInt(4, data.streak);
            ps.setString(5, data.lastSeen == null ? null : data.lastSeen.toString());
            ps.setString(6, String.join(",", data.cosmetics));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("MySQL write failed", e);
        }
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }
}
