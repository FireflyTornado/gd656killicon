package org.mods.gd656killicon.common.scoreboard;

public final class OfflineTimeDisplay {
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;

    private OfflineTimeDisplay() {
    }

    public static String format(long offlineMillis, long unknownRemainingMillis) {
        if (offlineMillis >= 0L) {
            if (offlineMillis < HOUR) {
                return offlineMillis / MINUTE + " m";
            }
            if (offlineMillis < DAY) {
                return offlineMillis / HOUR + " h";
            }
            return offlineMillis / DAY + " d";
        }
        if (unknownRemainingMillis >= 0L) {
            long days = unknownRemainingMillis / DAY + (unknownRemainingMillis % DAY == 0L ? 0L : 1L);
            return "(" + days + "d)";
        }
        return "-";
    }
}
