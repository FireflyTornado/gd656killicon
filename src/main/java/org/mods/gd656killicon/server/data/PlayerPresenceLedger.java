package org.mods.gd656killicon.server.data;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Presence information is independent of player scores and never deletes statistics. */
public final class PlayerPresenceLedger {
    public static final long DAY_MILLIS = 86_400_000L;
    private final Map<UUID, Presence> players = new ConcurrentHashMap<>();
    private int appliedUnknownRetentionDays;

    public void restore(Map<UUID, Presence> saved, int retentionDays) {
        players.clear();
        players.putAll(saved);
        appliedUnknownRetentionDays = retentionDays;
    }

    public Map<UUID, Presence> snapshot() {
        return Map.copyOf(players);
    }

    public int appliedUnknownRetentionDays() {
        return appliedUnknownRetentionDays;
    }

    public boolean contains(UUID playerId) {
        return players.containsKey(playerId);
    }

    public void register(Collection<UUID> playerIds, long now) {
        for (UUID id : playerIds) {
            players.compute(id, (key, presence) -> presence == null
                || (presence.lastOnlineAt() <= 0L && presence.unknownFilterStartedAt() <= 0L)
                ? new Presence(0L, now) : presence);
        }
    }

    public void recordOnline(UUID playerId, long now) {
        players.put(playerId, new Presence(now, 0L));
    }

    public void applyUnknownRetentionDays(int days, long now) {
        int validDays = Math.max(1, days);
        if (validDays != appliedUnknownRetentionDays) {
            players.replaceAll((id, presence) -> presence.lastOnlineAt() > 0L
                ? presence : new Presence(0L, now));
            appliedUnknownRetentionDays = validDays;
        }
    }

    public View view(UUID playerId, boolean online, Policy policy, long now) {
        if (online) {
            return new View(true, -1L, -1L);
        }
        Presence presence = players.compute(playerId, (id, saved) -> saved == null
            || (saved.lastOnlineAt() <= 0L && saved.unknownFilterStartedAt() <= 0L)
            ? new Presence(0L, now) : saved);
        if (presence.lastOnlineAt() > 0L) {
            long elapsed = Math.max(0L, now - presence.lastOnlineAt());
            return new View(!policy.filterEnabled() || elapsed < policy.offlineDays() * DAY_MILLIS,
                elapsed, -1L);
        }
        long elapsed = Math.max(0L, now - presence.unknownFilterStartedAt());
        long remaining = Math.max(0L, policy.unknownDays() * DAY_MILLIS - elapsed);
        return new View(!policy.filterEnabled() || remaining > 0L, -1L,
            policy.filterEnabled() ? remaining : -1L);
    }

    public record Presence(long lastOnlineAt, long unknownFilterStartedAt) {
    }

    public record View(boolean visible, long offlineDurationMillis, long unknownRemainingMillis) {
    }

    public record Policy(boolean filterEnabled, int offlineDays, int unknownDays) {
        public Policy {
            offlineDays = Math.max(1, offlineDays);
            unknownDays = Math.max(1, unknownDays);
        }
    }
}
