package player.ping;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoForgeCompat {

    private static final String ADMIN_PERMISSION_NODE = "playerping.admin";
    private static final String LUCKPERMS_PROVIDER_CLASS = "net.luckperms.api.LuckPermsProvider";
    private static final long DEBUG_LOG_THROTTLE_MS = 1500L;
    private static final int MAX_RECORDED_EVENTS = 4000;
    private static final DateTimeFormatter EVENT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Map<String, Long> LAST_DEBUG_LOG_BY_KEY = new ConcurrentHashMap<>();
    private static final Deque<String> RECENT_EVENTS = new ArrayDeque<>();

    private NeoForgeCompat() {}

    public static boolean hasPermission(CommandSourceStack source, int level) {
        if (level <= 0) {
            return true;
        }

        if (source.getPlayer() == null) {
            debugPermission(source, "hasPermission-console-" + level, "hasPermission(level=%d) -> true (console source)", level);
            return true;
        }

        boolean nativeAllowed = checkNativePermissionLevel(source, level);
        if (nativeAllowed) {
            debugPermission(source, "hasPermission-native-true-" + level, "hasPermission(level=%d) -> true (native CommandSourceStack permission check)", level);
            return true;
        }

        if (level >= 2) {
            boolean adminAllowed = hasAdminPermission(source);
            debugPermission(source, "hasPermission-admin-fallback-" + level, "hasPermission(level=%d) -> %s (admin fallback check)", level, adminAllowed);
            return adminAllowed;
        }

        debugPermission(source, "hasPermission-native-false-" + level, "hasPermission(level=%d) -> false (native CommandSourceStack permission check)", level);
        return false;
    }

    public static boolean hasAdminPermission(CommandSourceStack source) {
        if (source.getPlayer() == null) {
            debugPermission(source, "hasAdminPermission-console", "hasAdminPermission -> true (console source)");
            return true;
        }

        if (isOperator(source)) {
            debugPermission(source, "hasAdminPermission-op", "hasAdminPermission -> true (vanilla operator)");
            return true;
        }

        if (hasPermissionNode(source, ADMIN_PERMISSION_NODE)) {
            debugPermission(source, "hasAdminPermission-node", "hasAdminPermission -> true (permission node '%s')", ADMIN_PERMISSION_NODE);
            return true;
        }

        Integer weight = getLuckPermsWeight(source);
        int minWeight = PingSettings.getInstance().getMinLuckPermsWeight();
        if (weight != null && weight >= minWeight) {
            debugPermission(source, "hasAdminPermission-weight", "hasAdminPermission -> true (LuckPerms weight %d >= %d)", weight, minWeight);
            return true;
        }

        debugPermission(source, "hasAdminPermission-false", "hasAdminPermission -> false (not op, no node '%s', weight=%s, min=%d)", ADMIN_PERMISSION_NODE, String.valueOf(weight), minWeight);
        return false;
    }

    public static boolean isOperator(CommandSourceStack source) {
        if (source.getPlayer() == null) {
            debugPermission(source, "isOperator-console", "isOperator -> true (console source)");
            return true;
        }
        String currentName = source.getTextName();
        String[] opNames = source.getServer().getPlayerList().getOpNames();
        for (String opName : opNames) {
            if (opName != null && opName.equalsIgnoreCase(currentName)) {
                debugPermission(source, "isOperator-opnames-true", "isOperator -> true (matched '%s' in getOpNames)", currentName);
                return true;
            }
        }

        debugPermission(source, "isOperator-opnames-false", "isOperator -> false (name '%s' not present in getOpNames, count=%d)", currentName, opNames.length);
        return false;
    }

    public static String getProfileName(ServerPlayer player) {
        return player.getName().getString();
    }

    private static boolean hasPermissionNode(CommandSourceStack source, String permissionNode) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return true;
        }

        UUID uuid = player.getUUID();
        try {
            Class<?> providerClass = Class.forName(LUCKPERMS_PROVIDER_CLASS);
            Object luckPermsApi = providerClass.getMethod("get").invoke(null);
            Object userManager = invokeNoArgs(luckPermsApi, "getUserManager");
            Object user = invoke(userManager, "getUser", UUID.class, uuid);
            if (user == null) {
                debugPermission(source, "luckperms-user-missing-node", "LuckPerms user cache not found for %s while checking node '%s'", uuid, permissionNode);
                return false;
            }

            Object cachedData = invokeNoArgs(user, "getCachedData");
            Object permissionData = invokeNoArgs(cachedData, "getPermissionData");
            Object checkResult = invoke(permissionData, "checkPermission", String.class, permissionNode);
            boolean allowed = parsePermissionCheckResult(checkResult);
            debugPermission(source, "permissions-node-" + permissionNode, "Permission node '%s' -> %s", permissionNode, allowed);
            return allowed;
        } catch (ClassNotFoundException e) {
            debugPermission(source, "luckperms-missing-node", "LuckPerms API not installed; skipping node check for '%s'", permissionNode);
            return false;
        } catch (Exception e) {
            debugPermission(source, "permissions-node-error-" + permissionNode, "Failed to check permission node '%s': %s", permissionNode, e.getClass().getSimpleName());
            return false;
        }
    }

    private static boolean parsePermissionCheckResult(Object rawResult) {
        if (rawResult == null) {
            return false;
        }

        if (rawResult instanceof Boolean b) {
            return b;
        }

        try {
            Method asBoolean = rawResult.getClass().getMethod("asBoolean");
            Object converted = asBoolean.invoke(rawResult);
            if (converted instanceof Boolean b) {
                return b;
            }
        } catch (Exception ignored) {
            // Ignore and try enum-name fallback.
        }

        try {
            Method name = rawResult.getClass().getMethod("name");
            Object converted = name.invoke(rawResult);
            if (converted instanceof String value) {
                return "TRUE".equalsIgnoreCase(value);
            }
        } catch (Exception ignored) {
            // Ignore and return false.
        }

        return false;
    }

    private static Integer getLuckPermsWeight(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return null;
        }

        UUID uuid = player.getUUID();
        try {
            Class<?> providerClass = Class.forName(LUCKPERMS_PROVIDER_CLASS);
            Object luckPermsApi = providerClass.getMethod("get").invoke(null);
            Object userManager = invokeNoArgs(luckPermsApi, "getUserManager");
            Object user = invoke(userManager, "getUser", UUID.class, uuid);
            if (user == null) {
                debugPermission(source, "luckperms-user-missing", "LuckPerms user cache not found for %s", uuid);
                return null;
            }

            Object cachedData = invokeNoArgs(user, "getCachedData");
            Object metaData = invokeNoArgs(cachedData, "getMetaData");
            Object rawWeight = invokeNoArgs(metaData, "getWeight");
            Integer parsedWeight = parseWeight(rawWeight);
            debugPermission(source, "luckperms-weight", "LuckPerms reported weight=%s", String.valueOf(parsedWeight));
            return parsedWeight;
        } catch (ClassNotFoundException e) {
            debugPermission(source, "luckperms-missing", "LuckPerms API not installed; skipping role-weight check");
            return null;
        } catch (Exception e) {
            debugPermission(source, "luckperms-error", "LuckPerms role-weight lookup failed: %s", e.getClass().getSimpleName());
            return null;
        }
    }

    private static Integer parseWeight(Object rawWeight) {
        if (rawWeight == null) {
            return null;
        }

        if (rawWeight instanceof Number number) {
            return number.intValue();
        }

        if (rawWeight instanceof OptionalInt optionalInt) {
            return optionalInt.isPresent() ? optionalInt.getAsInt() : null;
        }

        if (rawWeight instanceof Optional<?> optional) {
            Object value = optional.orElse(null);
            if (value instanceof Number number) {
                return number.intValue();
            }
            return null;
        }

        try {
            Method isPresentMethod = rawWeight.getClass().getMethod("isPresent");
            Method getAsIntMethod = rawWeight.getClass().getMethod("getAsInt");
            Object present = isPresentMethod.invoke(rawWeight);
            if (present instanceof Boolean b && b) {
                Object value = getAsIntMethod.invoke(rawWeight);
                if (value instanceof Number number) {
                    return number.intValue();
                }
            }
        } catch (Exception ignored) {
            // Ignore: not an OptionalInt-like type.
        }

        try {
            Method intValueMethod = rawWeight.getClass().getMethod("intValue");
            Object value = intValueMethod.invoke(rawWeight);
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (Exception ignored) {
            // Ignore: not a Number-like type.
        }

        return null;
    }

    private static Object invokeNoArgs(Object target, String methodName) throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    private static Object invoke(Object target, String methodName, Class<?> parameterType, Object parameterValue) throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName, parameterType).invoke(target, parameterValue);
    }

    public static boolean isDebugEnabled() {
        return PingSettings.getInstance().getDebugPermissionChecks();
    }

    public static void debug(String section, String message, Object... args) {
        String rendered = renderDebugMessage(message, args);
        recordEvent(section, rendered);
        if (isDebugEnabled()) {
            PlayerPing.LOGGER.info("[PingPlayer Debug][{}] {}", section, rendered);
        }
    }

    public static void debugThrottled(String section, String throttleKey, long throttleMs, String message, Object... args) {
        long now = System.currentTimeMillis();
        Long previous = LAST_DEBUG_LOG_BY_KEY.put("throttle|" + throttleKey, now);
        if (previous != null && now - previous < throttleMs) {
            return;
        }

        String rendered = renderDebugMessage(message, args);
        recordEvent(section, rendered);
        if (isDebugEnabled()) {
            PlayerPing.LOGGER.info("[PingPlayer Debug][{}] {}", section, rendered);
        }
    }

    public static List<String> getRecentEventsSnapshot() {
        synchronized (RECENT_EVENTS) {
            return new ArrayList<>(RECENT_EVENTS);
        }
    }

    private static boolean checkNativePermissionLevel(CommandSourceStack source, int level) {
        try {
            Method hasPermission = source.getClass().getMethod("hasPermission", int.class);
            Object result = hasPermission.invoke(source, level);
            if (result instanceof Boolean allowed) {
                return allowed;
            }
        } catch (NoSuchMethodException ignored) {
            // Continue with alternate method names used by other mappings.
        } catch (Exception e) {
            debugPermission(source, "native-permission-error-hasPermission", "Native hasPermission lookup failed: %s", e.getClass().getSimpleName());
        }

        try {
            Method hasPermissionLevel = source.getClass().getMethod("hasPermissionLevel", int.class);
            Object result = hasPermissionLevel.invoke(source, level);
            if (result instanceof Boolean allowed) {
                return allowed;
            }
        } catch (NoSuchMethodException ignored) {
            // Continue with admin fallback logic.
        } catch (Exception e) {
            debugPermission(source, "native-permission-error-hasPermissionLevel", "Native hasPermissionLevel lookup failed: %s", e.getClass().getSimpleName());
        }

        return false;
    }

    private static void debugPermission(CommandSourceStack source, String keySuffix, String message, Object... args) {
        String actor = source.getPlayer() == null ? "console" : source.getPlayer().getName().getString();
        debugThrottled("permission/" + actor, actor + "|" + keySuffix, DEBUG_LOG_THROTTLE_MS, message, args);
    }

    private static String renderDebugMessage(String message, Object... args) {
        try {
            return String.format(message, args);
        } catch (Exception ignored) {
            return message;
        }
    }

    private static void recordEvent(String section, String rendered) {
        String eventLine = EVENT_TIME_FORMAT.format(LocalDateTime.now()) + " [" + section + "] " + rendered;
        synchronized (RECENT_EVENTS) {
            if (RECENT_EVENTS.size() >= MAX_RECORDED_EVENTS) {
                RECENT_EVENTS.removeFirst();
            }
            RECENT_EVENTS.addLast(eventLine);
        }
    }
}
