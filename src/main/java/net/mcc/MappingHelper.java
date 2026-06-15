package net.mcc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * 终极健壮性零链接反射工具类。
 * 内置全量 1.21.x 核心成员映射，支持自动化、监控及命令系统。
 */
public class MappingHelper {
    private static final Map<String, String> MAPPINGS = new HashMap<>();

    static {
        boolean is1214 = false;
        try {
            // 探测 1.21.4+: ClientPlayNetworkHandler.playerListEntries 从 field_3695 变为 field_52609
            Class<?> cpnh = Class.forName("net.minecraft.class_634");
            try {
                cpnh.getDeclaredField("field_52609");
                is1214 = true;
            } catch (NoSuchFieldException e) {
                is1214 = false;
            }
        } catch (Throwable ignored) {}

        // 类名映射
        MAPPINGS.put("MinecraftClient", "net/minecraft/class_310");
        MAPPINGS.put("ClientPlayerEntity", "net/minecraft/class_746");
        MAPPINGS.put("PlayerEntity", "net/minecraft/class_1657");
        MAPPINGS.put("LivingEntity", "net/minecraft/class_1309");
        MAPPINGS.put("HungerManager", "net/minecraft/class_1702");
        MAPPINGS.put("PlayerInventory", "net/minecraft/class_1661");
        MAPPINGS.put("ItemStack", "net/minecraft/class_1799");
        MAPPINGS.put("Text", "net/minecraft/class_2561");
        MAPPINGS.put("ClientPlayNetworkHandler", "net/minecraft/class_634");
        MAPPINGS.put("ClientWorld", "net/minecraft/class_638");
        MAPPINGS.put("PlayerListEntry", "net/minecraft/class_640");
        MAPPINGS.put("Registries", "net/minecraft/class_7923");
        MAPPINGS.put("Registry", "net/minecraft/class_2378");
        MAPPINGS.put("Identifier", "net/minecraft/class_2960");
        MAPPINGS.put("GameOptions", "net/minecraft/class_315");
        MAPPINGS.put("KeyBinding", "net/minecraft/class_304");
        MAPPINGS.put("Input", "net/minecraft/class_744");
        MAPPINGS.put("Team", "net/minecraft/class_268");
        MAPPINGS.put("Style", "net/minecraft/class_2583");
        MAPPINGS.put("TextColor", "net/minecraft/class_5251");
        MAPPINGS.put("LevelProperties", "net/minecraft/class_31");
        MAPPINGS.put("Entity", "net/minecraft/class_1297");
        MAPPINGS.put("EntityHitResult", "net/minecraft/class_3966");
        MAPPINGS.put("BlockHitResult", "net/minecraft/class_3965");
        MAPPINGS.put("Hand", "net/minecraft/class_1268");
        MAPPINGS.put("Screen", "net/minecraft/class_437");

        // 字段映射 (Yarn -> Intermediary)
        MAPPINGS.put("player", "field_1724");
        MAPPINGS.put("world", "field_1687");
        MAPPINGS.put("options", "field_1690");
        MAPPINGS.put("networkHandler", "field_1769");
        MAPPINGS.put("interactionManager", "field_1761");
        MAPPINGS.put("playerListEntries", is1214 ? "field_52609" : "field_3695");
        MAPPINGS.put("inventory", "field_7514");
        MAPPINGS.put("hungerManager", "field_7509");
        MAPPINGS.put("foodLevel", "field_7496");
        MAPPINGS.put("prevFoodLevel", "field_7497");
        MAPPINGS.put("experienceLevel", "field_7520");
        MAPPINGS.put("experienceProgress", "field_7510");
        MAPPINGS.put("totalExperience", "field_7521");
        MAPPINGS.put("selectedSlot", "field_7545");
        MAPPINGS.put("currentScreen", is1214 ? "field_1757" : "field_1755");
        MAPPINGS.put("main", "field_7547");
        MAPPINGS.put("input", "field_3913");
        MAPPINGS.put("attackKey", "field_1904");
        MAPPINGS.put("useKey", "field_1886");
        MAPPINGS.put("pressed", "field_1653");
        MAPPINGS.put("gameProfile", "field_3944");
        MAPPINGS.put("attackCooldown", is1214 ? "field_1755" : "field_1752");
        MAPPINGS.put("itemUseCooldown", is1214 ? "field_1752" : "field_1753");
        MAPPINGS.put("ITEM", "field_41175");
        MAPPINGS.put("lastAttackedTicks", "field_6010");
        MAPPINGS.put("hurtResistantTime", "field_6008");
        MAPPINGS.put("hurtTime", "field_6007");
        MAPPINGS.put("crosshairTarget", "field_1765");
        MAPPINGS.put("MAIN_HAND", "field_5808");

        // 方法映射 (Yarn -> Intermediary)
        MAPPINGS.put("getInstance", "method_1551");
        MAPPINGS.put("getSession", "method_1548");
        MAPPINGS.put("getUsername", "method_1676");
        MAPPINGS.put("getNetworkHandler", "method_1562");
        MAPPINGS.put("getHealth", "method_6032");
        MAPPINGS.put("getMaxHealth", "method_6063");
        MAPPINGS.put("getHungerManager", "method_6122");
        MAPPINGS.put("getFoodLevel", "method_7586");
        MAPPINGS.put("getTimeOfDay", "method_8510");
        MAPPINGS.put("getTime", "method_11871");
        MAPPINGS.put("gameTime", is1214 ? "comp_2190" : "method_11871");
        MAPPINGS.put("dayTime", is1214 ? "comp_2191" : "method_11870");
        MAPPINGS.put("getMainHandStack", "method_18861");
        MAPPINGS.put("getStackInHand", "method_6047");
        MAPPINGS.put("isFood", "method_19230");
        MAPPINGS.put("keysById", "field_1655"); // KeyBinding.keysById
        MAPPINGS.put("translationKey", "field_1654"); // KeyBinding.translationKey
        MAPPINGS.put("literal", "method_43471");
        MAPPINGS.put("sendMessage", "method_7353");
        MAPPINGS.put("getPlayerList", "method_2871"); // 获取玩家列表 Collection
        MAPPINGS.put("getPlayerListEntries", "method_31363"); // 获取玩家列表 entries
        MAPPINGS.put("getRegistryEntry", "method_40223");
        MAPPINGS.put("getProfile", "method_2966");
        MAPPINGS.put("getDisplayName", "method_2963");
        MAPPINGS.put("getName", "getName"); // GameProfile.getName
        MAPPINGS.put("getString", "method_10851");
        MAPPINGS.put("isEmpty", "method_7960");
        MAPPINGS.put("getCount", "method_7947");
        MAPPINGS.put("getItem", "method_7909");
        MAPPINGS.put("getMaxDamage", "method_7936");
        MAPPINGS.put("getDamage", "method_7919");
        MAPPINGS.put("requestRespawn", "method_7331");
        MAPPINGS.put("doAttack", "method_1536");
        MAPPINGS.put("attackEntity", "method_2918");
        MAPPINGS.put("doItemUse", is1214 ? "method_1583" : "method_1531");
        MAPPINGS.put("interactItem", is1214 ? "method_2919" : "method_2896");
        MAPPINGS.put("interactBlock", is1214 ? "method_2902" : "method_2905");
        MAPPINGS.put("getUseAction", "method_7951");
        MAPPINGS.put("swingHand", "method_6104");
        MAPPINGS.put("getEntity", "method_17770");
        MAPPINGS.put("stopUsingItem", "method_2907");
        MAPPINGS.put("isUsingItem", "method_6115");
        MAPPINGS.put("getYaw", "method_36454");
        MAPPINGS.put("getPitch", "method_36455");
        MAPPINGS.put("setYaw", "method_36456");
        MAPPINGS.put("setPitch", "method_36457");
        MAPPINGS.put("getId", "method_10221");
        MAPPINGS.put("getCommandDispatcher", "method_2903");
        MAPPINGS.put("getRoot", "method_8257");
        MAPPINGS.put("addChild", "method_8254");
        MAPPINGS.put("setPressed", "method_1436");
        MAPPINGS.put("append", "method_10852");
        MAPPINGS.put("setStyle", "method_10862");
        MAPPINGS.put("withColor", "method_1031");
        MAPPINGS.put("withBold", "method_1039");
        MAPPINGS.put("withItalic", "method_1044");
        MAPPINGS.put("withUnderline", "method_1034");
        MAPPINGS.put("fromRgb", "method_27721");
        MAPPINGS.put("getLevelProperties", "method_8503");
        MAPPINGS.put("getScoreboardTeam", "method_2962");
        MAPPINGS.put("getColor", "method_1135"); // Team.getColor
        MAPPINGS.put("getStyle", "method_10855");
        MAPPINGS.put("getRgb", "method_35842"); // TextColor.getRgb
    }

    public static String map(String name) {
        return MAPPINGS.getOrDefault(name, name);
    }

    public static Class<?> getClass(String yarnName) throws ClassNotFoundException {
        String mapped = map(yarnName).replace('/', '.');
        try {
            return Class.forName(mapped);
        } catch (ClassNotFoundException e) {
            // 针对 1.21.11+ 的官方名回退策略
            String official = getOfficialClassName(yarnName);
            if (official != null) {
                try { return Class.forName(official); } catch (ClassNotFoundException ignored) {}
            }
            throw e;
        }
    }

    private static String getOfficialClassName(String yarnName) {
        switch (yarnName) {
            case "MinecraftClient": return "net.minecraft.client.MinecraftClient";
            case "ClientPlayerEntity": return "net.minecraft.client.network.ClientPlayerEntity";
            case "ClientPlayNetworkHandler": return "net.minecraft.client.network.ClientPlayNetworkHandler";
            case "PlayerListEntry": return "net.minecraft.client.network.PlayerListEntry";
            case "Text": return "net.minecraft.network.chat.Component";
            case "Style": return "net.minecraft.network.chat.Style";
            case "TextColor": return "net.minecraft.network.chat.TextColor";
            case "Input": return "net.minecraft.client.player.Input";
            case "Screen": return "net.minecraft.client.gui.screens.Screen";
            default: return null;
        }
    }

    public static Field findField(Class<?> clazz, String yarnName) throws NoSuchFieldException {
        String mapped = map(yarnName);
        String altMapped = mapped.replace("_", "");
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try { Field f = current.getDeclaredField(mapped); f.setAccessible(true); return f; } catch (Exception ignored) {}
            try { Field f = current.getDeclaredField(altMapped); f.setAccessible(true); return f; } catch (Exception ignored) {}
            try { Field f = current.getDeclaredField(yarnName); f.setAccessible(true); return f; } catch (Exception ignored) {}
            current = current.getSuperclass();
        }
        throw new NoSuchFieldException(yarnName + " (mapped: " + mapped + ") in " + clazz.getName());
    }

    public static Method findMethod(Class<?> clazz, String yarnName, Class<?>... params) throws NoSuchMethodException {
        String mapped = map(yarnName);
        String altMapped = mapped.replace("_", "");
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try { return current.getDeclaredMethod(mapped, params); } catch (Exception ignored) {}
            try { return current.getDeclaredMethod(altMapped, params); } catch (Exception ignored) {}
            try { return current.getDeclaredMethod(yarnName, params); } catch (Exception ignored) {}
            current = current.getSuperclass();
        }
        for (Class<?> itf : clazz.getInterfaces()) {
            try { return itf.getDeclaredMethod(mapped, params); } catch (NoSuchMethodException ignored) {}
            try { return itf.getDeclaredMethod(altMapped, params); } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(yarnName);
    }

    public static Object getFieldValue(Object obj, String yarnName, Class<?> clazz) throws Exception {
        Class<?> targetClass = clazz;
        if (targetClass == null && obj != null) {
            if (obj instanceof Class) {
                targetClass = (Class<?>) obj;
                obj = null;
            } else {
                targetClass = obj.getClass();
            }
        }
        if (targetClass == null) return null;
        Field f = findField(targetClass, yarnName);
        f.setAccessible(true);
        return f.get(obj);
    }

    public static Object findUniqueFieldByType(Object obj, Class<?> type) {
        if (obj == null || type == null) return null;
        Class<?> curr = obj.getClass();
        while (curr != null && curr != Object.class) {
            for (Field f : curr.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        return f.get(obj);
                    } catch (Exception ignored) {}
                }
            }
            curr = curr.getSuperclass();
        }
        return null;
    }

    public static Field findFieldByType(Class<?> owner, Class<?> type) {
        Class<?> curr = owner;
        while (curr != null && curr != Object.class) {
            for (Field f : curr.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return f;
                }
            }
            curr = curr.getSuperclass();
        }
        return null;
    }

    public static void setFieldValue(Object obj, String yarnName, Object value) throws Exception {
        Field f = findField(obj.getClass(), yarnName);
        f.setAccessible(true);
        if (f.getType() == boolean.class && value instanceof Boolean) {
            f.setBoolean(obj, (Boolean) value);
        } else if (f.getType() == int.class && value instanceof Number) {
            f.setInt(obj, ((Number)value).intValue());
        } else if (f.getType() == float.class && value instanceof Number) {
            f.setFloat(obj, ((Number)value).floatValue());
        } else if (f.getType() == long.class && value instanceof Number) {
            f.setLong(obj, ((Number)value).longValue());
        } else {
            f.set(obj, value);
        }
    }

    public static Object invokeMethod(Object obj, String yarnName, Object... args) throws Exception {
        if (obj == null) return null;
        if (obj instanceof Class) {
            return invokeMethod(null, (Class<?>) obj, yarnName, args);
        }
        return invokeMethod(obj, obj.getClass(), yarnName, args);
    }

    public static Object invokeStaticMethod(Class<?> clazz, String yarnName, Object... args) throws Exception {
        return invokeMethod(null, clazz, yarnName, args);
    }

    private static Object invokeMethod(Object obj, Class<?> clazz, String yarnName, Object... args) throws Exception {
        String mappedName = map(yarnName);
        String altMappedName = mappedName.replace("_", "");

        // 针对 Record 类型
        if (clazz.isRecord()) {
            for (java.lang.reflect.RecordComponent rc : clazz.getRecordComponents()) {
                if (rc.getName().equals(mappedName) || rc.getName().equals(yarnName)) {
                    return rc.getAccessor().invoke(obj);
                }
            }
        }

        Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) types[i] = Object.class;
            else {
                types[i] = args[i].getClass();
            }
        }

        Class<?> curr = clazz;
        while (curr != null) {
            for (Method m : curr.getDeclaredMethods()) {
                String n = m.getName();
                if ((n.equals(mappedName) || n.equals(altMappedName) || n.equals(yarnName)) && m.getParameterCount() == args.length) {
                    boolean match = true;
                    Class<?>[] pTypes = m.getParameterTypes();
                    for (int i = 0; i < args.length; i++) {
                        Class<?> pType = pTypes[i];
                        if (args[i] != null) {
                            Class<?> aType = types[i];
                            if (!pType.isAssignableFrom(aType)) {
                                if (pType == int.class && (aType == Integer.class || Number.class.isAssignableFrom(aType))) continue;
                                if (pType == boolean.class && aType == Boolean.class) continue;
                                if (pType == float.class && (aType == Float.class || Number.class.isAssignableFrom(aType))) continue;
                                if (pType == long.class && (aType == Long.class || Number.class.isAssignableFrom(aType))) continue;
                                if (pType == double.class && (aType == Double.class || Number.class.isAssignableFrom(aType))) continue;
                                if (pType == byte.class && aType == Byte.class) continue;
                                if (pType == short.class && aType == Short.class) continue;
                                if (pType == char.class && aType == Character.class) continue;
                                match = false; break;
                            }
                        } else if (pType.isPrimitive()) {
                            match = false; break;
                        }
                    }
                    if (match) {
                        m.setAccessible(true);
                        Object[] convertedArgs = new Object[args.length];
                        for (int i = 0; i < args.length; i++) {
                            if (args[i] == null) convertedArgs[i] = null;
                            else if (pTypes[i] == int.class) convertedArgs[i] = ((Number) args[i]).intValue();
                            else if (pTypes[i] == float.class) convertedArgs[i] = ((Number) args[i]).floatValue();
                            else if (pTypes[i] == long.class) convertedArgs[i] = ((Number) args[i]).longValue();
                            else if (pTypes[i] == double.class) convertedArgs[i] = ((Number) args[i]).doubleValue();
                            else if (pTypes[i] == byte.class) convertedArgs[i] = ((Number) args[i]).byteValue();
                            else if (pTypes[i] == short.class) convertedArgs[i] = ((Number) args[i]).shortValue();
                            else convertedArgs[i] = args[i];
                        }
                        return m.invoke(obj, convertedArgs);
                    }
                }
            }
            // 递归检查所有接口
            for (Class<?> itf : curr.getInterfaces()) {
                try {
                    Method m = findMethodInInterface(itf, mappedName, yarnName, args.length, types);
                    if (m != null) {
                        m.setAccessible(true);
                        return m.invoke(obj, args);
                    }
                } catch (Exception ignored) {}
            }
            if (curr == Object.class) break;
            curr = curr.getSuperclass();
        }

        throw new NoSuchMethodException(yarnName + " (mapped: " + mappedName + ") in " + clazz.getName());
    }

    public static Object getRegistry(String name) {
        try {
            Class<?> registries = getClass("Registries");
            return getFieldValue(null, name, registries);
        } catch (Exception e) {
            try {
                Class<?> registry = getClass("Registry");
                return getFieldValue(null, name, registry);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Method findMethodInInterface(Class<?> itf, String mapped, String yarn, int argCount, Class<?>[] argTypes) {
        for (Method m : itf.getDeclaredMethods()) {
            if ((m.getName().equals(mapped) || m.getName().equals(yarn)) && m.getParameterCount() == argCount) {
                boolean match = true;
                Class<?>[] pTypes = m.getParameterTypes();
                for (int i = 0; i < argCount; i++) {
                    if (argTypes[i] != null && !pTypes[i].isAssignableFrom(argTypes[i])) {
                        if (pTypes[i] == int.class && argTypes[i] == Integer.class) continue;
                        if (pTypes[i] == boolean.class && argTypes[i] == Boolean.class) continue;
                        match = false; break;
                    }
                }
                if (match) return m;
            }
        }
        for (Class<?> superItf : itf.getInterfaces()) {
            Method m = findMethodInInterface(superItf, mapped, yarn, argCount, argTypes);
            if (m != null) return m;
        }
        return null;
    }

    public static Object getFieldValueStrict(Object obj, String yarnName) throws Exception {
        Field f = findField(obj.getClass(), yarnName);
        return f.get(obj);
    }

    public static Object getFirstFieldByType(Object obj, Class<?> type) {
        if (obj == null) return null;
        Class<?> curr = obj.getClass();
        while (curr != null && curr != Object.class) {
            for (Field f : curr.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        return f.get(obj);
                    } catch (Exception ignored) {}
                }
            }
            curr = curr.getSuperclass();
        }
        return null;
    }

    private static Map<?, ?> cachedPlayerMap = null;
    private static Object lastPlayerMapOwner = null;
    private static java.util.Set<Integer> visited = new java.util.HashSet<>();

    public static Map<?, ?> findPlayerMapFingerprint(Object nh) {
        if (nh == null) return null;
        if (nh == lastPlayerMapOwner && cachedPlayerMap != null) return cachedPlayerMap;

        visited.clear();
        // 策略 0: 直接根据已知字段名查找 (Mojang / Intermediary / 1.21.11)
        String[] possibleFields = {"f_104895_", "field_42514", "field_52609", "field_3695", "playerListEntries"};
        for (String fName : possibleFields) {
            try {
                Field f = nh.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                Object val = f.get(nh);
                if (val instanceof Map) {
                    cachedPlayerMap = (Map<?, ?>) val; lastPlayerMapOwner = nh;
                    return cachedPlayerMap;
                }
            } catch (Exception ignored) {}
        }

        // 策略 1: 扫描 NetworkHandler
        Map<?, ?> found = scanForPlayerMapInternal(nh);
        if (found != null) {
            cachedPlayerMap = found; lastPlayerMapOwner = nh;
            return found;
        }

        // 策略 2: 深度递归扫描
        found = deepSearchPlayerMap(nh, 0);
        if (found != null) {
            cachedPlayerMap = found; lastPlayerMapOwner = nh;
        }
        return found;
    }

    private static Map<?, ?> scanForPlayerMapInternal(Object obj) {
        Class<?> curr = obj.getClass();
        while (curr != null && curr != Object.class) {
            for (Field f : curr.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Map<?, ?> map = (Map<?, ?>) f.get(obj);
                        if (map != null && !map.isEmpty()) {
                            Object firstKey = map.keySet().iterator().next();
                            Object firstVal = map.values().iterator().next();
                            if (firstKey instanceof java.util.UUID || String.valueOf(firstKey).length() > 30) {
                                if (isPlayerEntry(firstVal)) return map;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
            curr = curr.getSuperclass();
        }
        return null;
    }

    private static boolean isPlayerEntry(Object val) {
        if (val == null) return false;
        String cn = val.getClass().getName();
        if (cn.contains("PlayerListEntry") || cn.contains("class_640") || cn.contains("NetworkPlayerInfo") || cn.contains("PlayerInfo") || cn.contains("Profile")) return true;
        for (Field f : val.getClass().getDeclaredFields()) {
            String ftn = f.getType().getName();
            if (ftn.contains("GameProfile") || ftn.contains("class_1923") || ftn.contains("Profile")) return true;
        }
        return false;
    }

    private static Map<?, ?> deepSearchPlayerMap(Object obj, int depth) {
        if (obj == null || depth > 3) return null;
        int id = System.identityHashCode(obj);
        if (visited.contains(id)) return null;
        visited.add(id);

        Map<?, ?> found = scanForPlayerMapInternal(obj);
        if (found != null) return found;

        Class<?> curr = obj.getClass();
        while (curr != null && curr != Object.class) {
            for (Field f : curr.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                try {
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if (val != null && shouldScan(val)) {
                        Map<?, ?> res = deepSearchPlayerMap(val, depth + 1);
                        if (res != null) return res;
                    }
                } catch (Exception ignored) {}
            }
            curr = curr.getSuperclass();
        }
        return null;
    }

    private static boolean shouldScan(Object val) {
        String name = val.getClass().getName();
        return !name.startsWith("java.") && !name.startsWith("sun.") && !name.startsWith("com.google.") && !name.startsWith("org.lwjgl.");
    }
}
