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

        if (feedback) {
            CommandDispatcher.addFeedback("§a已选择槽位 " + slot);
            showSlot(-1);
        }
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

    private static void displayStack(int slot, Object stack) throws Exception {
        if (stack == null || (boolean) MappingHelper.invokeMethod(stack, "isEmpty")) {
            CommandDispatcher.addFeedback(String.format("§7Slot %d: 空", slot));
            return;
        }

        int count = ((Number) MappingHelper.invokeMethod(stack, "getCount")).intValue();
        Object item = MappingHelper.invokeMethod(stack, "getItem");

        String id = "minecraft:air";
        try {
            // 策略 1: item.getRegistryEntry().registryKey().getValue().toString()
            try {
                Object entry = MappingHelper.invokeMethod(item, "getRegistryEntry");
                Object key = MappingHelper.invokeMethod(entry, "registryKey");
                Object value = MappingHelper.invokeMethod(key, "getValue");
                id = value.toString();
            } catch (Exception ignored) {}

            // 策略 2: Registries.ITEM.getId(item)
            if (id.contains("air")) {
                Object registry = MappingHelper.getRegistry("ITEM");
                if (registry != null) {
                    Object identifier = MappingHelper.invokeMethod(registry, "getId", item);
                    if (identifier != null) id = identifier.toString();
                }
            }

            // 策略 3: toString
            if (id.contains("air")) {
                id = item.toString();
            }
        } catch (Exception e) {
            id = "err:" + e.getMessage();
        }

        int maxDmg = ((Number) MappingHelper.invokeMethod(stack, "getMaxDamage")).intValue();
        int dmg = ((Number) MappingHelper.invokeMethod(stack, "getDamage")).intValue();
        String dur = maxDmg > 0 ? String.format(" (%d/%d)", maxDmg - dmg, maxDmg) : "";

        CommandDispatcher.addFeedback(String.format("Slot %d：§e%s §fx%d%s", slot, id, count, dur));
    }
}
