package net.mcc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 终极健壮性零链接反射工具类。
 * 内置全量 1.21.x 核心成员映射，并引入缓存以支持 20Hz 高频调用性能。
 */
public class MappingHelper {
    private static final Map<String, String> MAPPINGS = new HashMap<>();
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    static {
        boolean is1214 = false;
        try {
            Class<?> mc = Class.forName("net.minecraft.class_310");
            try {
                Field f = mc.getDeclaredField("field_1755");
                if (f.getType() == int.class) is1214 = true;
            } catch (Exception e) {
                try {
                    Class.forName("net.minecraft.class_634").getDeclaredField("field_52609");
                    is1214 = true;
                } catch (Exception e2) {
                    try {
                        Class<?> im = Class.forName("net.minecraft.class_636");
                        im.getDeclaredMethod("method_2919", Class.forName("net.minecraft.class_1657"), Class.forName("net.minecraft.class_1268"));
                        is1214 = true;
                    } catch (Exception ignored) {}
                }
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
        MAPPINGS.put("ChatScreen", "net/minecraft/class_408");

        // 字段映射
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
        MAPPINGS.put("sprinting", "field_6012");
        MAPPINGS.put("onGround", "field_5971");
        MAPPINGS.put("handSwinging", "field_6277");
        MAPPINGS.put("handSwingTicks", "field_6259");
        MAPPINGS.put("isClient", "field_9236");
        MAPPINGS.put("MAIN_HAND", "field_5808");

        // 方法映射
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
        MAPPINGS.put("keysById", "field_1655");
        MAPPINGS.put("translationKey", "field_1654");
        MAPPINGS.put("literal", "method_43471");
        MAPPINGS.put("sendMessage", "method_7353");
        MAPPINGS.put("getPlayerList", "method_2871");
        MAPPINGS.put("getPlayerListEntries", "method_31363");
        MAPPINGS.put("getRegistryEntry", "method_40223");
        MAPPINGS.put("getProfile", "method_2966");
        MAPPINGS.put("getDisplayName", "method_2963");
        MAPPINGS.put("getName", "getName");
        MAPPINGS.put("getString", "method_10851");
        MAPPINGS.put("isEmpty", "method_7960");
        MAPPINGS.put("getCount", "method_7947");
        MAPPINGS.put("getItem", "method_7909");
        MAPPINGS.put("getMaxDamage", "method_7936");
        MAPPINGS.put("getDamage", "method_7919");
        MAPPINGS.put("requestRespawn", "method_7331");
        MAPPINGS.put("doAttack", is1214 ? "method_1587" : "method_1536");
        MAPPINGS.put("attackEntity", is1214 ? "method_2912" : "method_2918");
        MAPPINGS.put("attackBlock", is1214 ? "method_2910" : "method_2902");
        MAPPINGS.put("doItemUse", is1214 ? "method_1583" : "method_1531");
        MAPPINGS.put("interactItem", is1214 ? "method_2919" : "method_2896");
        MAPPINGS.put("interactBlock", is1214 ? "method_2896" : "method_2905");
        MAPPINGS.put("swingHand", "method_6104");
        MAPPINGS.put("getEntity", "method_17770");
        MAPPINGS.put("getBlockPos", "method_17777");
        MAPPINGS.put("getSide", "method_17778");
        MAPPINGS.put("stopUsingItem", "method_2907");
        MAPPINGS.put("isUsingItem", "method_6115");
        MAPPINGS.put("getYaw", "method_36454");
        MAPPINGS.put("getPitch", "method_36455");
        MAPPINGS.put("setYaw", "method_36456");
        MAPPINGS.put("setPitch", "method_36457");
        MAPPINGS.put("setSprinting", "method_5735");
        MAPPINGS.put("resetLastAttackedTicks", "method_5851");
        MAPPINGS.put("getServer", "method_8501");
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
        MAPPINGS.put("getColor", "method_1135");
        MAPPINGS.put("getStyle", "method_10855");
        MAPPINGS.put("getRgb", "method_35842");
        MAPPINGS.put("isAccepted", "method_23665");
    }

    public static String map(String name) {
        return MAPPINGS.getOrDefault(name, name);
    }

    public static Class<?> getClass(String yarnName) throws ClassNotFoundException {
        String mapped = map(yarnName).replace('/', '.');
        try {
            return Class.forName(mapped);
        } catch (ClassNotFoundException e) {
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
        String key = clazz.getName() + ":" + yarnName;
        Field cached = FIELD_CACHE.get(key);
        if (cached != null) return cached;

        String mapped = map(yarnName);
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try { Field f = current.getDeclaredField(mapped); f.setAccessible(true); FIELD_CACHE.put(key, f); return f; } catch (Exception ignored) {}
            try { Field f = current.getDeclaredField(yarnName); f.setAccessible(true); FIELD_CACHE.put(key, f); return f; } catch (Exception ignored) {}
            current = current.getSuperclass();
        }
        throw new NoSuchFieldException(yarnName);
    }

    public static Object getFieldValue(Object obj, String yarnName, Class<?> clazz) throws Exception {
        Class<?> targetClass = (clazz != null) ? clazz : (obj instanceof Class ? (Class<?>) obj : obj.getClass());
        Field f = findField(targetClass, yarnName);
        return f.get(obj instanceof Class ? null : obj);
    }

    public static void setFieldValue(Object obj, String yarnName, Object value) throws Exception {
        Field f = findField(obj.getClass(), yarnName);
        if (f.getType() == int.class && value instanceof Number) f.setInt(obj, ((Number)value).intValue());
        else if (f.getType() == boolean.class && value instanceof Boolean) f.setBoolean(obj, (Boolean)value);
        else f.set(obj, value);
    }

    public static Method findMethod(Class<?> clazz, String yarnName, Class<?>... params) throws NoSuchMethodException {
        String key = clazz.getName() + ":" + yarnName + ":" + params.length;
        Method cached = METHOD_CACHE.get(key);
        if (cached != null) return cached;

        String mapped = map(yarnName);
        Class<?> current = clazz;
        while (current != null) {
            try { Method m = current.getDeclaredMethod(mapped, params); m.setAccessible(true); METHOD_CACHE.put(key, m); return m; } catch (Exception ignored) {}
            try { Method m = current.getDeclaredMethod(yarnName, params); m.setAccessible(true); METHOD_CACHE.put(key, m); return m; } catch (Exception ignored) {}
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(yarnName);
    }

    public static Object invokeMethod(Object obj, String yarnName, Object... args) throws Exception {
        if (obj == null) return null;
        Class<?> clazz = (obj instanceof Class) ? (Class<?>) obj : obj.getClass();
        Object target = (obj instanceof Class) ? null : obj;
        Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) types[i] = (args[i] == null) ? Object.class : args[i].getClass();
        Method m = findMethod(clazz, yarnName, types);
        return m.invoke(target, convertArgs(m.getParameterTypes(), args));
    }

    public static Object invokeStaticMethod(Class<?> clazz, String yarnName, Object... args) throws Exception {
        return invokeMethod(clazz, yarnName, args);
    }

    private static Object[] convertArgs(Class<?>[] pTypes, Object[] args) {
        Object[] res = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) res[i] = null;
            else if (pTypes[i] == int.class) res[i] = ((Number) args[i]).intValue();
            else res[i] = args[i];
        }
        return res;
    }

    public static Object findUniqueFieldByType(Object obj, Class<?> type) {
        if (obj == null || type == null) return null;
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (type.isAssignableFrom(f.getType())) {
                try { f.setAccessible(true); return f.get(obj); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public static Object getEnumConstant(String className, String constantName) {
        try {
            Class<?> clazz = getClass(className);
            for (Object obj : clazz.getEnumConstants()) {
                if (String.valueOf(obj).equals(constantName)) return obj;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static Method findMethodByStructure(Class<?> clazz, Class<?> returnType, Class<?>... paramTypes) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getParameterCount() == paramTypes.length) {
                boolean match = true;
                for (int i = 0; i < paramTypes.length; i++) {
                    if (paramTypes[i] != null && !m.getParameterTypes()[i].isAssignableFrom(paramTypes[i])) {
                        match = false; break;
                    }
                }
                if (match) { m.setAccessible(true); return m; }
            }
        }
        return null;
    }

    public static Object getRegistry(String name) {
        try { return getFieldValue(null, name, getClass("Registries")); } catch (Exception e) {
            try { return getFieldValue(null, name, getClass("Registry")); } catch (Exception ignored) {}
        }
        return null;
    }

    private static java.util.Set<Integer> visited = new java.util.HashSet<>();
    public static Map<?, ?> findPlayerMapFingerprint(Object nh) {
        if (nh == null) return null;
        visited.clear();
        return scanForPlayerMapInternal(nh);
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
                            if (firstKey instanceof java.util.UUID) return map;
                        }
                    } catch (Exception ignored) {}
                }
            }
            curr = curr.getSuperclass();
        }
        return null;
    }
}
