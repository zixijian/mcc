package net.mcc;

/**
 * 完全重构的性能监控系统
 */
public class PerformanceMonitor {
    private static final int WINDOW_SIZE = 10;
    private static long[] timeSamples = new long[WINDOW_SIZE];
    private static long[] tickSamples = new long[WINDOW_SIZE];
    private static int head = 0;
    private static int count = 0;

    private static long lastGameTime = -1;
    private static long lastDayTime = -1;
    private static long lastRealTime = -1;

    public static long getLastGameTime() {
        return lastGameTime;
    }

    public static long getLastDayTime() {
        return lastDayTime;
    }

    public static synchronized void onWorldTimeUpdate(long gameTime, long dayTime) {
        long now = System.currentTimeMillis();
        if (lastGameTime != -1) {
            long deltaGame = gameTime - lastGameTime;
            long deltaReal = now - lastRealTime;

            if (deltaGame > 0 && deltaReal > 0) {
                timeSamples[head] = deltaReal;
                tickSamples[head] = deltaGame;
                head = (head + 1) % WINDOW_SIZE;
                if (count < WINDOW_SIZE) count++;
            }
        }
        lastGameTime = gameTime;
        lastDayTime = dayTime;
        lastRealTime = now;
    }

    public static void showPerformance() {
        if (count < 2) {
            CommandDispatcher.addFeedback("§e正在采集样本 (" + count + "/" + WINDOW_SIZE + ")...");
            return;
        }

        long totalReal = 0;
        long totalTicks = 0;
        for (int i = 0; i < count; i++) {
            totalReal += timeSamples[i];
            totalTicks += tickSamples[i];
        }

        double tps = Math.min(20.0, (totalTicks * 1000.0) / totalReal);
        double mspt = (double) totalReal / totalTicks;

        String tpsColor = tps > 15 ? "§a" : (tps > 10 ? "§e" : "§c");
        String msptColor = mspt <= 50 ? "§a" : (mspt <= 55 ? "§e" : "§c");

        CommandDispatcher.addFeedback(String.format("§fTPS: %s%.2f §r§fMSPT: %s%.2f", tpsColor, tps, msptColor, mspt));
    }
}
