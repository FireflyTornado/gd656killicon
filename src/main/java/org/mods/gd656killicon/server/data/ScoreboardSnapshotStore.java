package org.mods.gd656killicon.server.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One snapshot per requesting player; accessed on the server thread. */
public final class ScoreboardSnapshotStore<T> {
    private final Map<UUID, Snapshot<T>> snapshots = new HashMap<>();
    private final long idleTimeoutMillis;
    private long nextId;

    public ScoreboardSnapshotStore(long idleTimeoutMillis) {
        this.idleTimeoutMillis = idleTimeoutMillis;
    }

    public Snapshot<T> create(UUID playerId, T data, long now) {
        snapshots.values().removeIf(snapshot -> now >= snapshot.expiresAt());
        Snapshot<T> snapshot = new Snapshot<>(++nextId, data, now + idleTimeoutMillis);
        snapshots.put(playerId, snapshot);
        return snapshot;
    }

    public Snapshot<T> get(UUID playerId, long snapshotId, long now) {
        Snapshot<T> snapshot = snapshots.get(playerId);
        if (snapshot == null || snapshot.id() != snapshotId) {
            return null;
        }
        if (now >= snapshot.expiresAt()) {
            snapshots.remove(playerId);
            return null;
        }
        Snapshot<T> renewed = new Snapshot<>(snapshot.id(), snapshot.data(), now + idleTimeoutMillis);
        snapshots.put(playerId, renewed);
        return renewed;
    }

    public void remove(UUID playerId) {
        snapshots.remove(playerId);
    }

    public void clear() {
        snapshots.clear();
    }

    public record Snapshot<T>(long id, T data, long expiresAt) {
    }
}
