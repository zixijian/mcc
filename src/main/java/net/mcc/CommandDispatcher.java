package net.mcc;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CommandDispatcher {

    public enum OutputChannel {
        CHAT(1), STDOUT(2), BOTH(3), CHAT_COLOR(4);
        private final int value;
        OutputChannel(int v) { this.value = v; }
        public int getValue() { return value; }
        public static OutputChannel fromInt(int i) {
            for (OutputChannel c : values()) if (c.value == i) return c;
            return CHAT;
        }
    }

    private static OutputChannel currentChannel = OutputChannel.STDOUT; // 默认 tune 2
    private static final List<String> feedbackBuffer = new ArrayList<>();

    public static boolean dispatch(String command) {
        if (!command.startsWith("/mcc")) {
            return false;
        }

        feedbackBuffer.clear();
        String[] parts = command.trim().split("\\s+");
        String sub = parts.length > 1 ? parts[1].toLowerCase() : "";

        try {
            switch (sub) {
                case "": showHelp(); break;
                case "time":
                    try { showTime(); } catch (Throwable t) { addFeedback("§cTime执行失败: " + t.getMessage()); }
                    break;
                case "hp": showHP(); break;
                case "xp": showXP(); break;
                case "tune":
                    if (parts.length > 2) {
                        int channelVal = Integer.parseInt(parts[2]);
                        currentChannel = OutputChannel.fromInt(channelVal);
                        addFeedback("§a频道已切换至: " + currentChannel + " (" + channelVal + ")");
                    } else {
                        addFeedback("§e当前频道: " + currentChannel.getValue());
                    }
                    break;
                case "tps": PerformanceMonitor.showPerformance(); break;
                case "list": PlayerListInfo.showList(); break;
                case "choose":
                case "cs":
                    if (parts.length > 2) InventoryManager.chooseSlot(Integer.parseInt(parts[2]), true);
                    break;
                case "slot":
                    int slotIdx = -1;
                    if (parts.length > 2) {
                        try { slotIdx = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
                    }
                    InventoryManager.showSlot(slotIdx);
                    break;
                case "tools": InventoryManager.showTools(); break;
                case "drop":
                    if (parts.length > 2) {
                        if (parts[2].equalsIgnoreCase("all")) {
                            InventoryManager.dropAll();
                        } else {
                            try {
                                InventoryManager.dropSlot(Integer.parseInt(parts[2]));
                            } catch (NumberFormatException e) {
                                addFeedback("§c无效槽位数字");
                            }
                        }
                    } else {
                        InventoryManager.dropSlot(-1);
                    }
                    break;
                case "attack":
                case "atk":
                    try {
                        int freq = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                        AutomationManager.setAttack(freq, parts.length > 2);
                    } catch (NumberFormatException e) {
                        addFeedback("§c参数必须是数字");
                    }
                    break;
                case "use":
                    try {
                        int freq = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
                        AutomationManager.setUse(freq, parts.length > 2);
                    } catch (NumberFormatException e) {
                        addFeedback("§c参数必须是数字");
                    }
                    break;
                case "respawn": AutomationManager.toggleAutoRespawn(); break;
                case "look":
                    if (parts.length > 3) {
                        AutomationManager.setLook(Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
                    } else {
                        AutomationManager.showLook();
                    }
                    break;
                case "status": AutomationManager.showStatus(); break;
                case "stop": AutomationManager.stopAll(); break;
                case "debug":
                case "mapping":
                    AutomationManager.probeMappings();
                    break;
                default: addFeedback("§c未知子命令: " + sub); break;
            }
        } catch (Exception e) {
            addFeedback("§c执行出错: " + e.toString());
        }

        flushFeedback();
        return true;
    }

    private static void showHelp() {
        addFeedback("§b[MCC 命令列表]");
        addFeedback("§f/mcc time | list | hp | xp | tps");
        addFeedback("§f/mcc choose/cs <slot> | slot [slot] | tools | drop [slot|all]");
        addFeedback("§f/mcc attack/atk [freq] | use [freq] | respawn");
        addFeedback("§f/mcc look <pitch> <yaw>");
        addFeedback("§f/mcc status | stop | tune <1-4>");
    }

    private static void showTime() throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("GMT+8"));
        Object world = getClientWorld();

        long timeOfDay = -1;

        // 策略 0: 尝试从 PerformanceMonitor 获取最近拦截的 DayTime (最高优先级)
        timeOfDay = PerformanceMonitor.getLastDayTime();
        if (timeOfDay != -1) timeOfDay = Math.abs(timeOfDay);

        if (world == null && timeOfDay == -1) {
            addFeedback("§c无法获取 World 对象且无缓存时间");
            return;
        }

        // 策略 1: 尝试各种已知的时间获取方法
        String[] methods = {"getTimeOfDay", "getTime", "method_8510", "method_11871", "method_145", "method_144"};
        for (String m : methods) {
            try {
                Object res = MappingHelper.invokeMethod(world, m);
                if (res instanceof Number) {
                    timeOfDay = ((Number)res).longValue();
                    if (timeOfDay >= 0) break;
                }
            } catch (Exception ignored) {}
        }

        // 策略 1.5: 尝试获取属性对象的世界时间 (1.21.1 适配)
        if (timeOfDay == -1) {
            try {
                Object props = MappingHelper.invokeMethod(world, "method_8503"); // getLevelProperties
                if (props != null) {
                    try { timeOfDay = ((Number) MappingHelper.invokeMethod(props, "getTimeOfDay")).longValue(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        // 策略 2: 深度扫描 World 及其子属性
        if (timeOfDay == -1) {
            try {
                Class<?> curr = world.getClass();
                while (curr != null && curr != Object.class) {
                    for (java.lang.reflect.Field f : curr.getDeclaredFields()) {
                        try {
                            f.setAccessible(true);
                            Object val = f.get(world);
                            if (val == null) continue;

                            // 如果是 long 字段
                            if (f.getType() == long.class) {
                                long lv = f.getLong(world);
                                if (lv > 10000) { timeOfDay = lv; break; }
                            }

                            // 如果是属性对象，尝试其方法
                            if (val.getClass().getName().contains("Properties") || val.getClass().getName().contains("class_31")) {
                                for (String m : methods) {
                                    try {
                                        Object res = MappingHelper.invokeMethod(val, m);
                                        if (res instanceof Number && ((Number)res).longValue() > 0) {
                                            timeOfDay = ((Number)res).longValue(); break;
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        } catch (Exception ignored) {}
                        if (timeOfDay != -1) break;
                    }
                    if (timeOfDay != -1) break;
                    curr = curr.getSuperclass();
                }
            } catch (Exception ignored) {}
        }

        if (timeOfDay != -1) {
            long day = timeOfDay / 24000;
            long hh = (timeOfDay % 24000) / 1000 + 6;
            if (hh >= 24) hh -= 24;
            long mm = (timeOfDay % 1000) * 60 / 1000;
            addFeedback(String.format("§e现实时间: %s", now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
            addFeedback(String.format("§6游戏时间: Day %d, %02d:%02d", day, hh, mm));
        } else {
            addFeedback("§c无法获取游戏时间");
        }
    }

    private static void showHP() throws Exception {
        Object player = getClientPlayer();
        if (player == null) {
            addFeedback("§fHP：§7NULL §f饱食度：§7NULL");
            return;
        }

        float health = ((Number) MappingHelper.invokeMethod(player, "getHealth")).floatValue();
        float maxHealth = ((Number) MappingHelper.invokeMethod(player, "getMaxHealth")).floatValue();

        int food = -1;
        try {
            // 彻底重构的 HungerManager 定位逻辑
            Object hunger = MappingHelper.findUniqueFieldByType(player, MappingHelper.getClass("HungerManager"));
            if (hunger == null) {
                // 尝试扫描包含 FoodLevel 方法的对象
                for (java.lang.reflect.Field f : player.getClass().getSuperclass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Object val = f.get(player);
                    if (val != null && val.getClass().getName().contains("class_1702")) {
                        hunger = val; break;
                    }
                }
            }

            if (hunger != null) {
                // 尝试获取饱食度：字段 > 方法 > 暴力
                try {
                    food = ((Number) MappingHelper.getFieldValue(hunger, "foodLevel", null)).intValue();
                } catch (Exception e) {
                    try {
                        food = ((Number) MappingHelper.invokeMethod(hunger, "getFoodLevel")).intValue();
                    } catch (Exception ignored) {}
                }

                if (food < 0 || food > 20) {
                    // 暴力扫描 HungerManager 里的 int
                    for (java.lang.reflect.Field f : hunger.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            f.setAccessible(true);
                            int v = f.getInt(hunger);
                            if (v >= 0 && v <= 20) { food = v; break; }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        String hpColor = health > 10 ? "§a" : (health > 4 ? "§e" : "§c");
        String foodColor = food > 14 ? "§a" : (food > 6 ? "§e" : "§c");

        addFeedback(String.format("§fHP：%s%.0f§7/§f%.0f §fFOOD：%s%s§7/§f20", hpColor, health, maxHealth, foodColor, (food == -1 ? "§7NULL" : String.valueOf(food))));
    }

    private static void showXP() throws Exception {
        Object player = getClientPlayer();
        if (player != null) {
            int level = ((Number) MappingHelper.getFieldValue(player, "experienceLevel", null)).intValue();
            float progress = ((Number) MappingHelper.getFieldValue(player, "experienceProgress", null)).floatValue();

            long totalXp = 0;
            if (level <= 16) totalXp = (long)level * level + 6L * level;
            else if (level <= 31) totalXp = (long)(2.5 * level * level - 40.5 * level + 360);
            else totalXp = (long)(4.5 * level * level - 162.5 * level + 2220);

            int nextLevelReq;
            if (level >= 30) nextLevelReq = 112 + (level - 30) * 9;
            else if (level >= 15) nextLevelReq = 37 + (level - 15) * 5;
            else nextLevelReq = 7 + level * 2;

            totalXp += Math.round(progress * nextLevelReq);

            addFeedback(String.format("§fLV：%d XP：%d %.0f%%", level, totalXp, progress * 100));
        } else {
            addFeedback("§fLV：NULL XP：NULL");
        }
    }

    public static Object getClient() throws Exception {
        return MappingHelper.invokeStaticMethod(MappingHelper.getClass("MinecraftClient"), "getInstance");
    }

    public static Object getClientPlayer() throws Exception {
        return MappingHelper.getFieldValue(getClient(), "player", null);
    }

    public static Object getClientWorld() throws Exception {
        Object client = getClient();
        // 尝试多种字段获取 world
        Object world = null;
        try { world = MappingHelper.getFieldValue(client, "world", null); } catch (Exception ignored) {}
        if (world == null) {
            try { world = MappingHelper.findUniqueFieldByType(client, MappingHelper.getClass("ClientWorld")); } catch (Exception ignored) {}
        }
        if (world == null) {
            // 暴力搜索 ClientWorld 类型的字段
            for (java.lang.reflect.Field f : client.getClass().getSuperclass().getDeclaredFields()) {
                if (f.getType().getName().contains("ClientWorld") || f.getType().getName().contains("class_638")) {
                    f.setAccessible(true);
                    world = f.get(client);
                    if (world != null) break;
                }
            }
        }
        return world;
    }

    public static void addFeedback(String message) {
        feedbackBuffer.add(message);
    }

    private static void flushFeedback() {
        if (feedbackBuffer.isEmpty()) return;
        String fullMessage = String.join("\n", feedbackBuffer);
        sendFeedbackInternal(fullMessage);
        feedbackBuffer.clear();
    }

    private static void sendFeedbackInternal(String message) {
        String noColor = message.replaceAll("§[0-9a-fk-lor]", "");

        // 针对 STDOUT 的 ANSI 颜色代码转换
        String ansiColor = message
            .replace("§l", "\u001B[1m")
            .replace("§r", "\u001B[0m")
            .replace("§0", "\u001B[30m").replace("§1", "\u001B[34m").replace("§2", "\u001B[32m")
            .replace("§3", "\u001B[36m").replace("§4", "\u001B[31m").replace("§5", "\u001B[35m")
            .replace("§6", "\u001B[33m").replace("§7", "\u001B[37m").replace("§8", "\u001B[90m")
            .replace("§9", "\u001B[94m").replace("§a", "\u001B[92m").replace("§b", "\u001B[96m")
            .replace("§c", "\u001B[91m").replace("§d", "\u001B[95m").replace("§e", "\u001B[93m")
            .replace("§f", "\u001B[97m")
            + "\u001B[0m";

        switch (currentChannel) {
            case CHAT: sendToChat(noColor); break;
            case STDOUT: System.out.println(ansiColor); break;
            case BOTH: sendToChat(noColor); System.out.println(ansiColor); break;
            case CHAT_COLOR: sendToChat(message); break;
        }
    }

    private static void sendToChat(String msg) {
        try {
            Object player = getClientPlayer();
            if (player == null) return;

            Object text = (currentChannel == OutputChannel.CHAT_COLOR) ? TextParser.createLiteral(msg) : TextParser.parse(msg);
            if (text != null) {
                try {
                    MappingHelper.invokeMethod(player, "sendMessage", text, false);
                } catch (Exception e1) {
                    try {
                        MappingHelper.invokeMethod(player, "method_43496", text, false);
                    } catch (Exception e2) {
                        try {
                            MappingHelper.invokeMethod(player, "sendMessage", text);
                        } catch (Exception e3) {
                            System.err.println("[MCC] Failed to send chat message: " + e3);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MCC] sendToChat error: " + e);
        }
    }
}
