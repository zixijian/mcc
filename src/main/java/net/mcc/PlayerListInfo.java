package net.mcc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 玩家列表深度指纹探测器 (基于 1.21.x 架构优化)
 */
public class PlayerListInfo {
    public static void showList() throws Exception {
        Object client = CommandDispatcher.getClient();
        String selfName = "";
        try {
            Object session = MappingHelper.invokeMethod(client, "getSession");
            selfName = (String) MappingHelper.invokeMethod(session, "getUsername");
        } catch (Exception e) {
            try {
                Object self = CommandDispatcher.getClientPlayer();
                Object profile = MappingHelper.getFieldValue(self, "gameProfile", null);
                selfName = (String) MappingHelper.invokeMethod(profile, "getName");
            } catch (Exception ignored) {}
        }

        Object nh = null;
        try { nh = MappingHelper.invokeMethod(client, "getNetworkHandler"); } catch (Exception ignored) {}
        if (nh == null) nh = MappingHelper.findUniqueFieldByType(client, MappingHelper.getClass("ClientPlayNetworkHandler"));
        if (nh == null) {
            // 暴力扫描 Client 中的 NetworkHandler 字段 (1.21.1 适配)
            Class<?> curr = client.getClass();
            while (curr != null && curr != Object.class) {
                for (java.lang.reflect.Field f : curr.getDeclaredFields()) {
                    String tn = f.getType().getName();
                    if (tn.contains("ClientPlayNetworkHandler") || tn.contains("class_634")) {
                        try { f.setAccessible(true); nh = f.get(client); if (nh != null) break; } catch (Exception ignored) {}
                    }
                }
                if (nh != null) break;
                curr = curr.getSuperclass();
            }
        }

        if (nh == null) {
            CommandDispatcher.addFeedback("§c无法获取 NetworkHandler");
            return;
        }

        Collection<?> playerEntries = scanForPlayerMap(nh);
        if (playerEntries == null || playerEntries.isEmpty()) {
            CommandDispatcher.addFeedback("§c获取玩家列表失败");
            return;
        }

        List<EntryProxy> sorted = new ArrayList<>();
        for (Object entry : playerEntries) {
            try {
                String name = extractName(entry);
                if (name == null) continue;

                boolean isMe = name.equalsIgnoreCase(selfName);
                boolean isYellow = false;

                // 探测黄名 (1.21.x 架构优化)
                try {
                    Object displayText = MappingHelper.invokeMethod(entry, "getDisplayName");
                    if (displayText != null) {
                        // 策略 1: 检查 getString() 中是否包含 § 颜色代码 (针对 1.21.4 识别)
                        String rawStr = (String) MappingHelper.invokeMethod(displayText, "getString");
                        if (rawStr != null && (rawStr.contains("§e") || rawStr.contains("§6"))) {
                            isYellow = true;
                        }

                        if (!isYellow) {
                            Object style = MappingHelper.invokeMethod(displayText, "getStyle");
                            if (style != null) {
                                Object color = MappingHelper.invokeMethod(style, "getColor");
                                if (color != null) {
                                    String colorStr = color.toString().toUpperCase();
                                    if (colorStr.contains("YELLOW") || colorStr.contains("GOLD")) isYellow = true;
                                    else {
                                        try {
                                            int rgb = ((Number) MappingHelper.invokeMethod(color, "getRgb")).intValue();
                                            // 0xFFFF55 是黄色, 0xFFAA00 是橙色/金
                                            if (rgb == 16777045 || rgb == 16755200) isYellow = true;
                                        } catch (Exception ignored) {}
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (!isYellow) {
                    try {
                        Object team = MappingHelper.invokeMethod(entry, "getScoreboardTeam");
                        if (team != null) {
                            Object color = MappingHelper.invokeMethod(team, "getColor");
                            if (color != null) {
                                String colorName = color.toString().toUpperCase();
                                if (colorName.contains("YELLOW") || colorName.contains("GOLD")) isYellow = true;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                sorted.add(new EntryProxy(name, isYellow, isMe));
            } catch (Exception ignored) {}
        }

        // 严格排序逻辑：[本人(0)] > [黄名(1)] > [普通(2)]，组内字母序
        Collections.sort(sorted, (a, b) -> {
            int pa = a.isMe ? 0 : (a.isYellow ? 1 : 2);
            int pb = b.isMe ? 0 : (b.isYellow ? 1 : 2);
            if (pa != pb) return pa - pb;
            return a.rawName.compareToIgnoreCase(b.rawName);
        });

        StringBuilder sb = new StringBuilder(String.format("§fPlayer（§e%d§f）：", sorted.size()));
        for (int i = 0; i < sorted.size(); i++) {
            EntryProxy p = sorted.get(i);
            if (p.isMe) {
                // 本人账号：白底（白括号）、红字、加粗 [[Name]]
                // 使用 §r 确保从干净状态开始，§f 设定括号颜色，§c§l 设定名字
                sb.append("§r§f[[§c§l").append(p.rawName).append("§r§f]]");
            } else if (p.isYellow) {
                // 黄名玩家：§e
                sb.append("§r§e").append(p.rawName);
            } else {
                // 普通玩家：§f (强制白色，防止继承)
                sb.append("§r§f").append(p.rawName);
            }
            if (i < sorted.size() - 1) {
                // 使用 §r 重置样式，确保下一个玩家（特别是普通玩家）不带颜色
                sb.append("§r，");
            } else {
                sb.append("§r");
            }
        }
        CommandDispatcher.addFeedback(sb.toString());
    }

    private static Collection<?> scanForPlayerMap(Object nh) {
        // 1.21.4+ 直接尝试已知字段 field_52609
        try {
            Object val = MappingHelper.getFieldValue(nh, "field_52609", null);
            if (val instanceof Map) return ((Map<?, ?>) val).values();
        } catch (Exception ignored) {}

        Class<?> curr = nh.getClass();
        while (curr != null && curr != Object.class) {
            for (java.lang.reflect.Field f : curr.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(nh);
                    if (val == null) continue;

                    Collection<?> coll = null;
                    if (val instanceof Map) coll = ((Map<?, ?>) val).values();
                    else if (val instanceof Collection) coll = (Collection<?>) val;

                    if (coll != null && !coll.isEmpty()) {
                        for (Object first : coll) {
                            if (first == null) continue;
                            String cn = first.getClass().getName();
                            // 指纹：包含 PlayerListEntry 或 GameProfile 相关的成员
                            if (cn.contains("class_640") || cn.contains("PlayerListEntry")) return coll;
                            // 探测是否包含 GameProfile 类型的字段
                            for (java.lang.reflect.Field subF : first.getClass().getDeclaredFields()) {
                                if (subF.getType().getName().contains("GameProfile") || subF.getType().getName().contains("com.mojang.authlib.GameProfile")) return coll;
                            }
                            try {
                                if (MappingHelper.invokeMethod(first, "getProfile") != null) return coll;
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
            curr = curr.getSuperclass();
        }
        // Fallback: 尝试直接调用 getPlayerListEntries
        try {
            return (Collection<?>) MappingHelper.invokeMethod(nh, "getPlayerListEntries");
        } catch (Exception ignored) {}
        return null;
    }

    private static String extractName(Object entry) {
        try {
            Object profile = MappingHelper.invokeMethod(entry, "getProfile");
            if (profile != null) return (String) MappingHelper.invokeMethod(profile, "getName");
        } catch (Exception ignored) {}
        return null;
    }

    private static class EntryProxy {
        final String rawName;
        final boolean isYellow, isMe;
        EntryProxy(String n, boolean y, boolean m) {
            this.rawName = n; this.isYellow = y; this.isMe = m;
        }
    }
}
