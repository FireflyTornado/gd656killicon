package org.mods.gd656killicon.server.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The only file written by presence tracking; playerdata JSON files remain separate. */
public final class PlayerPresenceStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final PlayerPresenceLedger ledger = new PlayerPresenceLedger();

    public PlayerPresenceStore(Path file) {
        this.file = file;
    }

    public PlayerPresenceLedger ledger() {
        return ledger;
    }

    public void load() throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            int days = root.has("applied_unknown_retention_days") ? root.get("applied_unknown_retention_days").getAsInt() : 0;
            Map<UUID, PlayerPresenceLedger.Presence> saved = new HashMap<>();
            JsonObject players = root.getAsJsonObject("players");
            if (players != null) {
                for (Map.Entry<String, JsonElement> entry : players.entrySet()) {
                    JsonObject value = entry.getValue().getAsJsonObject();
                    long lastOnline = value.has("last_online_at") ? value.get("last_online_at").getAsLong() : 0L;
                    long unknownStart = value.has("unknown_filter_started_at") ? value.get("unknown_filter_started_at").getAsLong() : 0L;
                    saved.put(UUID.fromString(entry.getKey()), new PlayerPresenceLedger.Presence(
                        Math.max(0L, lastOnline), Math.max(0L, unknownStart)));
                }
            }
            ledger.restore(saved, days);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid presence ledger: " + file, exception);
        }
    }

    public void save() throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("applied_unknown_retention_days", ledger.appliedUnknownRetentionDays());
        JsonObject players = new JsonObject();
        ledger.snapshot().forEach((id, presence) -> {
            JsonObject value = new JsonObject();
            value.addProperty("last_online_at", presence.lastOnlineAt());
            value.addProperty("unknown_filter_started_at", presence.unknownFilterStartedAt());
            players.add(id.toString(), value);
        });
        root.add("players", players);
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
