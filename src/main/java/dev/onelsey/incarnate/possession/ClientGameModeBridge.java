package dev.onelsey.incarnate.possession;

import dev.onelsey.incarnate.IncarnatePlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClientGameModeBridge {
    private static final String GAME_EVENT_PACKET = "net.minecraft.network.protocol.game.ClientboundGameEventPacket";
    private static final String CHANGE_GAME_MODE = "CHANGE_GAME_MODE";

    private final IncarnatePlugin plugin;
    private final GameMode mountedClientMode;
    private final AtomicBoolean warnedUnavailable = new AtomicBoolean(false);

    public ClientGameModeBridge(IncarnatePlugin plugin) {
        this.plugin = plugin;
        this.mountedClientMode = readMountedClientMode(plugin);
    }

    public void presentMounted(Player player) {
        present(player, mountedClientMode);
    }

    public void present(Player player, GameMode mode) {
        if (player == null || mode == null || !player.isOnline()) {
            return;
        }
        try {
            sendClientGameMode(player, mode);
        } catch (Throwable ex) {
            warnUnavailable(ex);
        }
    }

    static float protocolValue(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> 0.0F;
            case CREATIVE -> 1.0F;
            case ADVENTURE -> 2.0F;
            case SPECTATOR -> 3.0F;
        };
    }

    private static GameMode readMountedClientMode(IncarnatePlugin plugin) {
        String raw = plugin.getConfig().getString("camera.mounted-client-game-mode", "ADVENTURE");
        GameMode mode;
        try {
            mode = GameMode.valueOf(raw == null ? "ADVENTURE" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown camera.mounted-client-game-mode; using ADVENTURE.");
            return GameMode.ADVENTURE;
        }
        if (mode == GameMode.SPECTATOR) {
            plugin.getLogger().warning("camera.mounted-client-game-mode=SPECTATOR disables vanilla F/sprint input; using ADVENTURE.");
            return GameMode.ADVENTURE;
        }
        return mode;
    }

    private static void sendClientGameMode(Player player, GameMode mode) throws ReflectiveOperationException {
        Method getHandle = player.getClass().getMethod("getHandle");
        Object serverPlayer = getHandle.invoke(player);
        Object packetListener = readField(serverPlayer, "connection");

        ClassLoader loader = serverPlayer.getClass().getClassLoader();
        Class<?> packetClass = Class.forName(GAME_EVENT_PACKET, true, loader);
        Field changeModeField = packetClass.getField(CHANGE_GAME_MODE);
        Object changeModeType = changeModeField.get(null);
        Object packet = createPacket(packetClass, changeModeType, protocolValue(mode));
        invokeSend(packetListener, packet);
    }

    private static Object createPacket(Class<?> packetClass, Object changeModeType, float value) throws ReflectiveOperationException {
        for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length != 2
                || !parameters[0].isInstance(changeModeType)
                || parameters[1] != float.class
                || !constructor.trySetAccessible()) {
                continue;
            }
            return constructor.newInstance(changeModeType, value);
        }
        throw new NoSuchMethodException(packetClass.getName() + "(Type,float)");
    }

    private static void invokeSend(Object packetListener, Object packet) throws ReflectiveOperationException {
        for (Method method : packetListener.getClass().getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (method.getName().equals("send")
                && parameters.length == 1
                && parameters[0].isAssignableFrom(packet.getClass())) {
                method.invoke(packetListener, packet);
                return;
            }
        }
        for (Class<?> type = packetListener.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (!method.getName().equals("send")
                    || parameters.length != 1
                    || !parameters[0].isAssignableFrom(packet.getClass())
                    || !method.trySetAccessible()) {
                    continue;
                }
                method.invoke(packetListener, packet);
                return;
            }
        }
        throw new NoSuchMethodException(packetListener.getClass().getName() + "#send(Packet)");
    }

    private static Object readField(Object owner, String name) throws ReflectiveOperationException {
        if (owner == null) {
            throw new NoSuchFieldException(name);
        }
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                if (!field.trySetAccessible()) {
                    throw new IllegalAccessException(type.getName() + '#' + name);
                }
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(owner.getClass().getName() + '#' + name);
    }

    private void warnUnavailable(Throwable throwable) {
        if (!warnedUnavailable.compareAndSet(false, true)) {
            return;
        }
        plugin.getLogger().log(
            java.util.logging.Level.WARNING,
            "Mounted client input mode could not be presented. The server remains safe in spectator mode, but vanilla F/sprint input may be unavailable.",
            throwable
        );
    }
}
