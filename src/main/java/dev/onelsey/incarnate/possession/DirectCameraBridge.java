package dev.onelsey.incarnate.possession;

import dev.onelsey.incarnate.IncarnatePlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class DirectCameraBridge {
    private final IncarnatePlugin plugin;
    private final AtomicBoolean warnedUnavailable = new AtomicBoolean(false);

    public DirectCameraBridge(IncarnatePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean attach(Player player, Entity target) {
        return setCamera(player, target);
    }

    public boolean reset(Player player) {
        return setCamera(player, player);
    }

    private boolean setCamera(Player player, Entity target) {
        if (player == null || target == null || !player.isOnline()) {
            return false;
        }
        try {
            Object serverPlayer = handle(player);
            Object targetHandle = handle(target);
            Method setter = findCameraSetter(serverPlayer.getClass(), targetHandle.getClass());
            setter.invoke(serverPlayer, targetHandle);

            Method getter = findCameraGetter(serverPlayer.getClass());
            if (getter == null) {
                return true;
            }
            Object actual = getter.invoke(serverPlayer);
            return actual == targetHandle;
        } catch (Throwable ex) {
            warnUnavailable(ex);
            return false;
        }
    }

    private static Object handle(Entity entity) throws ReflectiveOperationException {
        Method getHandle = entity.getClass().getMethod("getHandle");
        return getHandle.invoke(entity);
    }

    private static Method findCameraSetter(Class<?> ownerClass, Class<?> targetClass) throws NoSuchMethodException {
        for (Class<?> type = ownerClass; type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (!method.getName().equals("setCamera")
                    || parameters.length != 1
                    || !parameters[0].isAssignableFrom(targetClass)
                    || !method.trySetAccessible()) {
                    continue;
                }
                return method;
            }
        }
        throw new NoSuchMethodException(ownerClass.getName() + "#setCamera(Entity)");
    }

    private static Method findCameraGetter(Class<?> ownerClass) {
        for (Class<?> type = ownerClass; type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals("getCamera")
                    && method.getParameterCount() == 0
                    && method.trySetAccessible()) {
                    return method;
                }
            }
        }
        return null;
    }

    private void warnUnavailable(Throwable throwable) {
        if (!warnedUnavailable.compareAndSet(false, true)) {
            return;
        }
        plugin.getLogger().log(
            Level.WARNING,
            "Direct entity camera is unavailable on this server build. Incarnate will fail closed instead of degrading possession input.",
            throwable
        );
    }
}
