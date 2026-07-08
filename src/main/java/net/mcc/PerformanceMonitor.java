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
    private static long lastSyncRealTime = -1;

    private static long lastEstimatedDayTime = -1;
    private static long lastUpdatePacketRealTime = -1;

    public static long getLastGameTime() {
        return lastGameTime;
    }

    public static long getLastDayTime() {
        return lastDayTime;
    }

    /**
     * 获取估算的游戏时间，支持平滑增加
     */
    public static synchronized long getEstimatedDayTime() {
        if (lastDayTime == -1 || lastSyncRealTime == -1) return -1;

        long absTime = Math.abs(lastDayTime);
        long now = System.currentTimeMillis();
        long deltaReal = now - lastSyncRealTime;

        // 如果 dayTime >= 0，说明服务器开启了日夜循环
        // 允许长达 60 秒的插值，以应对极其不稳定的网络或服务器
        if (lastDayTime >= 0 && deltaReal > 0 && deltaReal < 60000) {
            double currentTps = 20.0;
            if (count >= 2) {
                long totalReal = 0;
                long totalTicks = 0;
                for (int i = 0; i < count; i++) {
                    totalReal += timeSamples[i];
                    totalTicks += tickSamples[i];
                }
                if (totalReal > 0) currentTps = Math.min(20.0, (totalTicks * 1000.0) / totalReal);
            }

            long extraTicks = (long)(deltaReal * currentTps / 1000.0);
            long estimated = absTime + extraTicks;

            // 严格单调递增，但允许在服务器时间大幅度落后时进行重置
            if (lastEstimatedDayTime != -1 && estimated < lastEstimatedDayTime) {
                if (lastEstimatedDayTime - estimated < 200) { // 小幅度回退（网络波动），保持不变
                    return lastEstimatedDayTime;
                }
            }
            lastEstimatedDayTime = estimated;
            return estimated;
        }

        return absTime;
    }

    public static synchronized void onWorldTimeUpdate(long gameTime, long dayTime) {
        long now = System.currentTimeMillis();
        lastUpdatePacketRealTime = now;

        if (lastGameTime != -1) {
            long deltaGame = gameTime - lastGameTime;
            long deltaReal = now - lastRealTime;
            if (deltaGame > 0 && deltaGame < 2000 && deltaReal > 0) {
                timeSamples[head] = deltaReal;
                tickSamples[head] = deltaGame;
                head = (head + 1) % WINDOW_SIZE;
                if (count < WINDOW_SIZE) count++;
            }
        }

        // 核心：即使 dayTime 没变（服务器冻结），我们也应该更新 lastDayTime 以便 getEstimatedDayTime 知道最新的 abs 基准
        // 但是，如果 dayTime 确实变了，我们一定要重置插值基准 lastSyncRealTime
        if (dayTime != lastDayTime || lastSyncRealTime == -1) {
            lastDayTime = dayTime;
            lastSyncRealTime = now;
            // 如果是大幅度同步，允许重置单调性
            if (lastEstimatedDayTime != -1 && Math.abs(Math.abs(dayTime) - lastEstimatedDayTime) > 1000) {
                lastEstimatedDayTime = -1;
            }
        }

        lastGameTime = gameTime;
        lastRealTime = now;
    }

    public static long getLastUpdatePacketAge() {
        if (lastUpdatePacketRealTime == -1) return -1;
        return System.currentTimeMillis() - lastUpdatePacketRealTime;
    }

    public static void showPerformance() {
        if (count < 2) {
            CommandDispatcher.addFeedback("§e正在采集样本 (" + count + "/" + WINDOW_SIZE + ")...");
            return;
        }
        long totalReal = 0; long totalTicks = 0;
        for (int i = 0; i < count; i++) {
            totalReal += timeSamples[i]; totalTicks += tickSamples[i];
        }
        double tps = Math.min(20.0, (totalTicks * 1000.0) / totalReal);
        double mspt = (double) totalReal / totalTicks;
        String tpsColor = tps > 15 ? "§a" : (tps > 10 ? "§e" : "§c");
        String msptColor = mspt <= 50 ? "§a" : (mspt <= 55 ? "§e" : "§c");
        CommandDispatcher.addFeedback(String.format("§fTPS: %s%.2f §r§fMSPT: %s%.2f", tpsColor, tps, msptColor, mspt));
    }
}
