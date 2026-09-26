package net.mcc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * 终极健壮性零链接反射工具类。
 * 全量适配 Mojang 官方 26.x (26.1, 26.2, 26.3) 原生命名与旧版混淆兼容。
 */
public class MappingHelper {
    private static final Map<String, String> MAPPINGS = new HashMap<>();
    private static final Map<String, Class<?>> CLASS_CACHE = new HashMap<>();
    private static final Class<?> NOT_FOUND_MARKER = Void.class;
    public static boolean is26Plus = true;

    static {
        // 类名映射 (Yarn / Intermediary -> Mojang Official Native)
        MAPPINGS.put("MinecraftClient", "net.minecraft.client.Minecraft");
        MAPPINGS.put("ClientPlayerEntity", "net.minecraft.client.player.LocalPlayer");
        MAPPINGS.put("PlayerEntity", "net.minecraft.world.entity.player.Player");
        MAPPINGS.put("LivingEntity", "net.minecraft.world.entity.LivingEntity");
        MAPPINGS.put("HungerManager", "net.minecraft.world.food.FoodData");
        MAPPINGS.put("PlayerInventory", "net.minecraft.world.entity.player.Inventory");
        MAPPINGS.put("ItemStack", "net.minecraft.world.item.ItemStack");
        MAPPINGS.put("Text", "net.minecraft.network.chat.Component");
        MAPPINGS.put("ClientPlayNetworkHandler", "net.minecraft.client.multiplayer.ClientPacketListener");
        MAPPINGS.put("ClientWorld", "net.minecraft.client.multiplayer.ClientLevel");
        MAPPINGS.put("PlayerListEntry", "net.minecraft.client.multiplayer.PlayerInfo");
        MAPPINGS.put("Registries", "net.minecraft.core.registries.BuiltInRegistries");
        MAPPINGS.put("Registry", "net.minecraft.core.Registry");
        MAPPINGS.put("Identifier", "net.minecraft.resources.ResourceLocation");
        MAPPINGS.put("GameOptions", "net.minecraft.client.Options");
        MAPPINGS.put("KeyBinding", "net.minecraft.client.KeyMapping");
        MAPPINGS.put("Input", "net.minecraft.client.player.Input");
        MAPPINGS.put("Team", "net.minecraft.world.scores.PlayerTeam");
        MAPPINGS.put("Style", "net.minecraft.network.chat.Style");
        MAPPINGS.put("TextColor", "net.minecraft.network.chat.TextColor");
        MAPPINGS.put("LevelProperties", "net.minecraft.world.level.storage.PrimaryLevelData");
        MAPPINGS.put("Entity", "net.minecraft.world.entity.Entity");
        MAPPINGS.put("EntityHitResult", "net.minecraft.world.phys.EntityHitResult");
        MAPPINGS.put("BlockHitResult", "net.minecraft.world.phys.BlockHitResult");
        MAPPINGS.put("Hand", "net.minecraft.world.InteractionHand");
        MAPPINGS.put("Screen", "net.minecraft.client.gui.screens.Screen");
        MAPPINGS.put("FishingRodItem", "net.minecraft.world.item.FishingRodItem");

        // 字段映射 (Yarn / Intermediary -> Mojang Official Native)
        MAPPINGS.put("player", "player");
        MAPPINGS.put("world", "level");
        MAPPINGS.put("options", "options");
        MAPPINGS.put("networkHandler", "connection");
        MAPPINGS.put("interactionManager", "gameMode");
        MAPPINGS.put("playerListEntries", "playerInfoMap");
        MAPPINGS.put("inventory", "inventory");
        MAPPINGS.put("hungerManager", "foodData");
        MAPPINGS.put("foodLevel", "foodLevel");
        MAPPINGS.put("prevFoodLevel", "lastFoodLevel");
        MAPPINGS.put("experienceLevel", "experienceLevel");
        MAPPINGS.put("experienceProgress", "experienceProgress");
        MAPPINGS.put("totalExperience", "totalExperience");
        MAPPINGS.put("selectedSlot", "selectedSlot");
        MAPPINGS.put("currentScreen", "screen");
        MAPPINGS.put("main", "items");
        MAPPINGS.put("input", "input");
        MAPPINGS.put("attackKey", "keyAttack");
        MAPPINGS.put("useKey", "keyUse");
        MAPPINGS.put("pressed", "isDown");
        MAPPINGS.put("attackCooldown", "missTime");
        MAPPINGS.put("itemUseCooldown", "rightClickDelay");
        MAPPINGS.put("ITEM", "ITEM");
        MAPPINGS.put("lastAttackedTicks", "attackStrengthTicker");
        MAPPINGS.put("hurtResistantTime", "invulnerableTime");
        MAPPINGS.put("hurtTime", "hurtTime");
        MAPPINGS.put("crosshairTarget", "hitResult");
        MAPPINGS.put("MAIN_HAND", "MAIN_HAND");

        // 方法映射 (Yarn / Intermediary -> Mojang Official Native)
        MAPPINGS.put("getInstance", "getInstance");
        MAPPINGS.put("getSession", "getUser");
        MAPPINGS.put("getUsername", "getName");
        MAPPINGS.put("getNetworkHandler", "getConnection");
        MAPPINGS.put("getHealth", "getHealth");
        MAPPINGS.put("getMaxHealth", "getMaxHealth");
        MAPPINGS.put("getHungerManager", "getFoodData");
        MAPPINGS.put("getFoodLevel", "getFoodLevel");
        MAPPINGS.put("getTimeOfDay", "getDayTime");
        MAPPINGS.put("getTime", "getGameTime");
        MAPPINGS.put("gameTime", "getGameTime");
        MAPPINGS.put("dayTime", "getDayTime");
        MAPPINGS.put("keysById", "ALL");
        MAPPINGS.put("translationKey", "name");
        MAPPINGS.put("literal", "literal");
        MAPPINGS.put("sendMessage", "sendSystemMessage");
        MAPPINGS.put("getPlayerList", "getOnlinePlayers");
        MAPPINGS.put("getPlayerListEntries", "getListedOnlinePlayers");
        MAPPINGS.put("getRegistryEntry", "get");
        MAPPINGS.put("getProfile", "getProfile");
        MAPPINGS.put("getDisplayName", "getTabListDisplayName");
        MAPPINGS.put("getName", "getName");
        MAPPINGS.put("getString", "getString");
        MAPPINGS.put("isEmpty", "isEmpty");
        MAPPINGS.put("getCount", "getCount");
        MAPPINGS.put("getItem", "getItem");
        MAPPINGS.put("getMaxDamage", "getMaxDamage");
        MAPPINGS.put("getDamage", "getDamageValue");
        MAPPINGS.put("requestRespawn", "respawn");
        MAPPINGS.put("doAttack", "startAttack");
        MAPPINGS.put("attackEntity", "attack");
        MAPPINGS.put("attackBlock", "startDestroyBlock");
        MAPPINGS.put("doItemUse", "startUseItem");
        MAPPINGS.put("interactItem", "useItem");
        MAPPINGS.put("interactBlock", "useItemOn");
        MAPPINGS.put("swingHand", "swing");
        MAPPINGS.put("getEntity", "getEntity");
        MAPPINGS.put("getBlockPos", "getBlockPos");
        MAPPINGS.put("getSide", "getDirection");
        MAPPINGS.put("stopUsingItem", "releaseUsingItem");
        MAPPINGS.put("isUsingItem", "isUsingItem");
        MAPPINGS.put("getYaw", "getYRot");
        MAPPINGS.put("getPitch", "getXRot");
        MAPPINGS.put("setYaw", "setYRot");
        MAPPINGS.put("setPitch", "setXRot");
        MAPPINGS.put("getId", "getKey");
        MAPPINGS.put("getCommandDispatcher", "getCommands");
        MAPPINGS.put("getRoot", "getRoot");
        MAPPINGS.put("addChild", "addChild");
        MAPPINGS.put("setPressed", "setDown");
        MAPPINGS.put("append", "append");
        MAPPINGS.put("setStyle", "setStyle");
        MAPPINGS.put("withColor", "withColor");
        MAPPINGS.put("withBold", "withBold");
        MAPPINGS.put("withItalic", "withItalic");
        MAPPINGS.put("withUnderline", "withUnderlined");
        MAPPINGS.put("fromRgb", "fromRgb");
        MAPPINGS.put("getLevelProperties", "getLevelData");
        MAPPINGS.put("getScoreboardTeam", "getTeam");
        MAPPINGS.put("getColor", "getColor");
        MAPPINGS.put("getStyle", "getStyle");
        MAPPINGS.put("getRgb", "getValue");
        MAPPINGS.put("isAccepted", "consumesAction");
        MAPPINGS.put("getAttackCooldownProgressPerTick", "getCurrentItemAttackStrengthDelay");
        MAPPINGS.put("getAttackCooldownProgress", "getAttackStrengthScale");
        MAPPINGS.put("EntityAttributes", "net.minecraft.world.entity.ai.attributes.Attributes");
        MAPPINGS.put("GENERIC_ATTACK_SPEED", "ATTACK_SPEED");
        MAPPINGS.put("getAttributeValue", "getAttributeValue");
    }

    public static String map(String name) {
        return MAPPINGS.getOrDefault(name, name);
    }

    public static Class<?> getClass(String yarnName) throws ClassNotFoundException {
        if (CLASS_CACHE.containsKey(yarnName)) {
            Class<?> cached = CLASS_CACHE.get(yarnName);
            if (cached == NOT_FOUND_MARKER) throw new ClassNotFoundException(yarnName);
            return cached;
        }

        String mapped = map(yarnName).replace('/', '.');
        try {
            Class<?> clazz = Class.forName(mapped);
            CLASS_CACHE.put(yarnName, clazz);
            return clazz;
        } catch (ClassNotFoundException e) {
            String official = getOfficialClassName(yarnName);
            if (official != null) {
                try {
                    Class<?> clazz = Class.forName(official);
                    CLASS_CACHE.put(yarnName, clazz);
                    return clazz;
                } catch (ClassNotFoundException ignored) {}
            }
            CLASS_CACHE.put(yarnName, NOT_FOUND_MARKER);
            throw e;
        }
    }

    private static String getOfficialClassName(String yarnName) {
        switch (yarnName) {
            case "MinecraftClient": return "net.minecraft.client.Minecraft";
            case "ClientPlayerEntity": return "net.minecraft.client.player.LocalPlayer";
            case "ClientPlayNetworkHandler": return "net.minecraft.client.multiplayer.ClientPacketListener";
            case "PlayerListEntry": return "net.minecraft.client.multiplayer.PlayerInfo";
            case "Text": return "net.minecraft.network.chat.Component";
            case "Style": return "net.minecraft.network.chat.Style";
            case "TextColor": return "net.minecraft.network.chat.TextColor";
            case "Input": return "net.minecraft.client.player.Input";
            case "Screen": return "net.minecraft.client.gui.screens.Screen";
            case "FishingRodItem": return "net.minecraft.world.item.FishingRodItem";
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

    public static Method findMethodByStructure(Class<?> clazz, Class<?> returnType, Class<?>... paramTypes) {
        if (clazz == null) return null;
        Class<?> curr = clazz;
        while (curr != null && curr != Object.class) {
            for (Method m : curr.getDeclaredMethods()) {
                if (m.getParameterCount() == paramTypes.length) {
                    if (returnType != null && !returnType.isAssignableFrom(m.getReturnType())) continue;
                    boolean match = true;
                    Class<?>[] mParams = m.getParameterTypes();
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (paramTypes[i] != null && !mParams[i].isAssignableFrom(paramTypes[i])) {
                            match = false; break;
                        }
                    }
                    if (match) return m;
                }
            }
            curr = curr.getSuperclass();
        }
        return null;
    }

    public static Object getEnumConstant(String className, String constantName) {
        try {
            Class<?> clazz = getClass(className);
            if (clazz == null) clazz = Class.forName(className.replace('/', '.'));
            for (Object obj : clazz.getEnumConstants()) {
                if (String.valueOf(obj).equals(constantName)) return obj;
            }
            return getFieldValue(null, constantName, clazz);
        } catch (Exception ignored) {}
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
        Class<?> clazz = (obj instanceof Class) ? (Class<?>) obj : obj.getClass();
        Object targetObj = (obj instanceof Class) ? null : obj;

        java.util.List<String> names = new java.util.ArrayList<>();
        names.add(map(yarnName));
        names.add(yarnName);

        if (yarnName.equals("doItemUse")) { names.add("startUseItem"); names.add("useItem"); names.add("method_1531"); names.add("method_1583"); }
        if (yarnName.equals("interactItem")) { names.add("useItem"); names.add("method_2896"); names.add("method_2919"); }
        if (yarnName.equals("interactBlock")) { names.add("useItemOn"); names.add("method_2905"); names.add("method_2896"); }
        if (yarnName.equals("attackBlock")) { names.add("startDestroyBlock"); names.add("method_2902"); names.add("method_2910"); }
        if (yarnName.equals("doAttack")) { names.add("startAttack"); names.add("method_1536"); names.add("method_1587"); }
        if (yarnName.equals("attackEntity")) { names.add("attack"); names.add("method_2918"); names.add("method_2912"); }
        if (yarnName.equals("dayTime")) { names.add("getDayTime"); names.add("method_11870"); }
        if (yarnName.equals("gameTime")) { names.add("getGameTime"); names.add("method_11871"); }
        if (yarnName.equals("getTimeOfDay")) { names.add("getDayTime"); names.add("method_8510"); }
        if (yarnName.equals("isUsingItem")) { names.add("isUsingItem"); names.add("method_6115"); }
        if (yarnName.equals("isAccepted")) { names.add("consumesAction"); names.add("isAccepted"); }
        if (yarnName.equals("getAttackCooldownProgressPerTick")) { names.add("getCurrentItemAttackStrengthDelay"); names.add("method_26352"); }
        if (yarnName.equals("getAttackCooldownProgress")) { names.add("getAttackStrengthScale"); names.add("method_7261"); }
        if (yarnName.equals("swingHand")) { names.add("swing"); names.add("method_6104"); }
        if (yarnName.equals("getName")) { names.add("getName"); names.add("method_7848"); }
        if (yarnName.equals("getString")) { names.add("getString"); names.add("method_10851"); }
        if (yarnName.equals("getBlockPos")) { names.add("getBlockPos"); names.add("method_17777"); }
        if (yarnName.equals("getSide")) { names.add("getDirection"); names.add("getSide"); }
        if (yarnName.equals("getItem")) { names.add("getItem"); names.add("method_7909"); }
        if (yarnName.equals("getCount")) { names.add("getCount"); names.add("method_7947"); }
        if (yarnName.equals("isEmpty")) { names.add("isEmpty"); names.add("method_7960"); }
        if (yarnName.equals("getMaxDamage")) { names.add("getMaxDamage"); names.add("method_7936"); }
        if (yarnName.equals("getDamage")) { names.add("getDamageValue"); names.add("getDamage"); }

        for (String name : names) {
            try { return invokeMethodInternal(targetObj, clazz, name, args); } catch (NoSuchMethodException ignored) {}
        }

        if (args.length > 0) {
            try {
                Method m = findMethodStructural(clazz, args);
                if (m != null) {
                    m.setAccessible(true);
                    return m.invoke(targetObj, convertArgs(m.getParameterTypes(), args));
                }
            } catch (Exception ignored) {}
        }

        throw new NoSuchMethodException(yarnName + " in " + clazz.getName());
    }

    public static Object invokeStaticMethod(Class<?> clazz, String yarnName, Object... args) throws Exception {
        return invokeMethod(clazz, yarnName, args);
    }

    private static Object invokeMethodInternal(Object obj, Class<?> clazz, String name, Object... args) throws Exception {
        String altName = name.replace("_", "");
        if (clazz.isRecord()) {
            for (java.lang.reflect.RecordComponent rc : clazz.getRecordComponents()) {
                if (rc.getName().equals(name) || rc.getName().equals(altName)) {
                    return rc.getAccessor().invoke(obj);
                }
            }
        }

        Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) types[i] = (args[i] == null) ? Object.class : args[i].getClass();

        Class<?> curr = clazz;
        while (curr != null) {
            for (Method m : curr.getDeclaredMethods()) {
                if ((m.getName().equals(name) || m.getName().equals(altName)) && m.getParameterCount() == args.length) {
                    if (isParameterMatch(m.getParameterTypes(), types, args)) {
                        m.setAccessible(true);
                        return m.invoke(obj, convertArgs(m.getParameterTypes(), args));
                    }
                }
            }
            for (Class<?> itf : curr.getInterfaces()) {
                Method m = findMethodInInterface(itf, name, altName, args.length, types, args);
                if (m != null) {
                    m.setAccessible(true);
                    return m.invoke(obj, convertArgs(m.getParameterTypes(), args));
                }
            }
            if (curr == Object.class) break;
            curr = curr.getSuperclass();
        }
        throw new NoSuchMethodException(name);
    }

    private static boolean isParameterMatch(Class<?>[] pTypes, Class<?>[] aTypes, Object[] args) {
        for (int i = 0; i < pTypes.length; i++) {
            Class<?> p = pTypes[i];
            Class<?> a = aTypes[i];
            if (args[i] == null) {
                if (p.isPrimitive()) return false;
                continue;
            }
            if (!p.isAssignableFrom(a)) {
                if (p == int.class && (Number.class.isAssignableFrom(a))) continue;
                if (p == boolean.class && a == Boolean.class) continue;
                if (p == float.class && (Number.class.isAssignableFrom(a))) continue;
                if (p == long.class && (Number.class.isAssignableFrom(a))) continue;
                if (p == double.class && (Number.class.isAssignableFrom(a))) continue;
                if (p == byte.class && a == Byte.class) continue;
                if (p == short.class && a == Short.class) continue;
                if (p == char.class && a == Character.class) continue;
                return false;
            }
        }
        return true;
    }

    private static Object[] convertArgs(Class<?>[] pTypes, Object[] args) {
        Object[] res = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) res[i] = null;
            else if (pTypes[i] == int.class) res[i] = ((Number) args[i]).intValue();
            else if (pTypes[i] == float.class) res[i] = ((Number) args[i]).floatValue();
            else if (pTypes[i] == long.class) res[i] = ((Number) args[i]).longValue();
            else if (pTypes[i] == double.class) res[i] = ((Number) args[i]).doubleValue();
            else if (pTypes[i] == byte.class) res[i] = ((Number) args[i]).byteValue();
            else if (pTypes[i] == short.class) res[i] = ((Number) args[i]).shortValue();
            else res[i] = args[i];
        }
        return res;
    }

    private static Method findMethodStructural(Class<?> clazz, Object[] args) {
        Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) types[i] = (args[i] == null) ? Object.class : args[i].getClass();
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getParameterCount() == args.length && isParameterMatch(m.getParameterTypes(), types, args)) return m;
        }
        return null;
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

    private static Method findMethodInInterface(Class<?> itf, String name, String altName, int argCount, Class<?>[] argTypes, Object[] args) {
        for (Method m : itf.getDeclaredMethods()) {
            if ((m.getName().equals(name) || m.getName().equals(altName)) && m.getParameterCount() == argCount) {
                if (isParameterMatch(m.getParameterTypes(), argTypes, args)) return m;
            }
        }
        for (Class<?> superItf : itf.getInterfaces()) {
            Method m = findMethodInInterface(superItf, name, altName, argCount, argTypes, args);
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
        String[] possibleFields = {"playerInfoMap", "playerInfo", "f_104895_", "field_42514", "field_52609", "field_3695", "playerListEntries"};
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

        Map<?, ?> found = scanForPlayerMapInternal(nh);
        if (found != null) {
            cachedPlayerMap = found; lastPlayerMapOwner = nh;
            return found;
        }

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
        if (cn.contains("PlayerInfo") || cn.contains("PlayerListEntry") || cn.contains("class_640") || cn.contains("NetworkPlayerInfo") || cn.contains("Profile")) return true;
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
