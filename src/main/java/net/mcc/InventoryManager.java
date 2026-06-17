package net.mcc;

import java.util.Optional;

public class InventoryManager {
    public static void chooseSlot(int slot, boolean feedback) throws Exception {
        if (slot < 0 || slot > 8) {
            if (feedback) CommandDispatcher.addFeedback("§c无效槽位: " + slot);
            return;
        }
        Object player = CommandDispatcher.getClientPlayer();
        Object inv = MappingHelper.getFieldValue(player, "inventory", null);
        MappingHelper.setFieldValue(inv, "selectedSlot", slot);

        // 关键：向服务器同步槽位更改以防止幽灵物品
        syncSlot(slot);

        if (feedback) {
            CommandDispatcher.addFeedback("§a已选择槽位 " + slot);
            showSlot(-1);
        }
    }

    private static void syncSlot(int slot) {
        try {
            Object client = CommandDispatcher.getClient();
            Object nh = MappingHelper.invokeMethod(client, "getNetworkHandler");
            if (nh != null) {
                // 1. 尝试直接调用 syncSelectedSlot (InteractionManager)
                try {
                    Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
                    if (im != null) {
                        // method_2895 = syncSelectedSlot (1.21.1)
                        MappingHelper.invokeMethod(im, "method_2895");
                    }
                } catch (Exception ignored) {}

                // 2. 尝试手动发送 UpdateSelectedSlotC2SPacket
                try {
                    Class<?> packetClass = MappingHelper.getClass("net/minecraft/class_2868"); // UpdateSelectedSlotC2SPacket
                    Object packet = packetClass.getConstructor(int.class).newInstance(slot);
                    // method_10743 = sendPacket (ClientPlayNetworkHandler)
                    MappingHelper.invokeMethod(nh, "method_10743", packet);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    public static void showSlot(int specificSlot) throws Exception {
        Object player = CommandDispatcher.getClientPlayer();
        Object inv = MappingHelper.getFieldValue(player, "inventory", null);
        int slot = (specificSlot >= 0 && specificSlot <= 8) ? specificSlot : ((Number) MappingHelper.getFieldValue(inv, "selectedSlot", null)).intValue();
        Object main = MappingHelper.getFieldValue(inv, "main", null);
        Object stack = ((java.util.List<?>) main).get(slot);
        displayStack(slot, stack);
    }

    public static void showTools() throws Exception {
        Object player = CommandDispatcher.getClientPlayer();
        Object inv = MappingHelper.getFieldValue(player, "inventory", null);
        Object main = MappingHelper.getFieldValue(inv, "main", null);
        java.util.List<?> mainList = (java.util.List<?>) main;

        CommandDispatcher.addFeedback("§b[快捷栏物品]");
        for (int i = 0; i < 9; i++) {
            displayStack(i, mainList.get(i));
        }
    }

    public static void dropSlot(int slot) {
        new Thread(() -> {
            try {
                Object player = CommandDispatcher.getClientPlayer();
                Object inv = MappingHelper.getFieldValue(player, "inventory", null);
                int current = ((Number) MappingHelper.getFieldValue(inv, "selectedSlot", null)).intValue();

                int target = (slot >= 0 && slot <= 8) ? slot : current;

                if (target != current) {
                    chooseSlot(target, false);
                    Thread.sleep(50); // 增加同步等待时间
                }

                // 扔出物品: player.dropSelectedItem(true)
                try {
                    MappingHelper.invokeMethod(player, "method_7290", true);
                } catch (Exception e) {
                    try { MappingHelper.invokeMethod(player, "dropSelectedItem", true); } catch (Exception ignored) {}
                }

                if (target != current) {
                    Thread.sleep(50);
                    chooseSlot(current, false);
                }
                CommandDispatcher.addFeedback("§a已扔出槽位 " + target + " 的物品");
            } catch (Exception e) {
                CommandDispatcher.addFeedback("§c丢弃失败: " + e.getMessage());
            }
        }).start();
    }

    public static void dropAll() {
        new Thread(() -> {
            try {
                Object player = CommandDispatcher.getClientPlayer();
                Object inv = MappingHelper.getFieldValue(player, "inventory", null);
                int original = ((Number) MappingHelper.getFieldValue(inv, "selectedSlot", null)).intValue();

                CommandDispatcher.addFeedback("§e开始丢弃快捷栏所有物品...");
                for (int i = 0; i < 9; i++) {
                    chooseSlot(i, false);
                    Thread.sleep(60); // 切换槽位并等待服务器同步

                    try {
                        MappingHelper.invokeMethod(player, "method_7290", true);
                    } catch (Exception e) {
                        try { MappingHelper.invokeMethod(player, "dropSelectedItem", true); } catch (Exception ignored) {}
                    }
                    Thread.sleep(60); // 等待丢弃动作完成
                }

                // 丢弃完毕后不需要强制切回原槽位，但如果需要可以取消下面注释
                // chooseSlot(original, false);
                CommandDispatcher.addFeedback("§a已完成快捷栏清空");
            } catch (Exception e) {
                CommandDispatcher.addFeedback("§c全部丢弃失败: " + e.getMessage());
            }
        }).start();
    }

    private static void displayStack(int slot, Object stack) throws Exception {
        if (stack == null || (boolean) MappingHelper.invokeMethod(stack, "isEmpty")) {
            CommandDispatcher.addFeedback(String.format("§7Slot %d: 空", slot));
            return;
        }

        int count = ((Number) MappingHelper.invokeMethod(stack, "getCount")).intValue();
        Object item = MappingHelper.invokeMethod(stack, "getItem");

        String id = "minecraft:air";
        String name = "";
        try {
            // 策略 1: Registries.ITEM.getId(item) (获取 ID: minecraft:stone)
            Object registry = MappingHelper.getRegistry("ITEM");
            if (registry != null) {
                Object identifier = MappingHelper.invokeMethod(registry, "getId", item);
                if (identifier != null) {
                    String sid = identifier.toString();
                    if (!sid.isEmpty() && !sid.equals("minecraft:air")) id = sid;
                }
            }

            // 策略 2: 获取物品显示名称 (getName -> getString)
            try {
                Object text = MappingHelper.invokeMethod(item, "getName");
                if (text != null) {
                    String sname = (String) MappingHelper.invokeMethod(text, "getString");
                    if (sname != null && !sname.isEmpty()) name = sname;
                }
            } catch (Exception ignored) {}

            // 策略 3: Fallback ID (getRegistryEntry)
            if (id.equals("minecraft:air")) {
                try {
                    Object entry = MappingHelper.invokeMethod(item, "getRegistryEntry");
                    Object key = MappingHelper.invokeMethod(entry, "registryKey");
                    Object value = MappingHelper.invokeMethod(key, "getValue");
                    id = value.toString();
                } catch (Exception ignored) {}
            }

            // 策略 4: 最终 Fallback (toString, 排除混淆地址)
            if (id.equals("minecraft:air") || id.startsWith("[")) {
                String sid = item.toString();
                if (!sid.isEmpty() && !sid.startsWith("[")) id = sid;
            }
        } catch (Exception e) {
            id = "err:" + e.getMessage();
        }

        String displayName = name.isEmpty() ? id : (id.equals("minecraft:air") ? name : id + " (" + name + ")");

        int maxDmg = ((Number) MappingHelper.invokeMethod(stack, "getMaxDamage")).intValue();
        int dmg = ((Number) MappingHelper.invokeMethod(stack, "getDamage")).intValue();
        String dur = maxDmg > 0 ? String.format(" (%d/%d)", maxDmg - dmg, maxDmg) : "";

        CommandDispatcher.addFeedback(String.format("Slot %d：§e%s §fx%d%s", slot, displayName, count, dur));
    }
}
