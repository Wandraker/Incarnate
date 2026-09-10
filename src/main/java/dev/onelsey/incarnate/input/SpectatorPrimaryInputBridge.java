package dev.onelsey.incarnate.input;

import dev.onelsey.incarnate.IncarnatePlugin;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class SpectatorPrimaryInputBridge implements Listener {
    private static final String HANDLER_NAME = "incarnate_spectator_primary";
    private static final String PACKET_SIMPLE_NAME = "ServerboundSpectatorActionPacket";
    private static final String VANILLA_PACKET_HANDLER = "packet_handler";

    private final IncarnatePlugin plugin;
    private final Consumer<Player> primaryInput;
    private final Map<UUID, Channel> channels = new ConcurrentHashMap<>();
    private final AtomicBoolean warnedUnavailable = new AtomicBoolean(false);

    public SpectatorPrimaryInputBridge(IncarnatePlugin plugin, Consumer<Player> primaryInput) {
        this.plugin = plugin;
        this.primaryInput = primaryInput;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduleInjection(player, 0);
        }
    }

    public void stop() {
        for (Channel channel : channels.values()) {
            removeHandler(channel);
        }
        channels.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        scheduleInjection(event.getPlayer(), 0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Channel channel = channels.remove(event.getPlayer().getUniqueId());
        if (channel != null) {
            removeHandler(channel);
        }
    }

    private void scheduleInjection(Player player, int attempt) {
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline() || channels.containsKey(player.getUniqueId())) {
                return;
            }
            if (!inject(player) && attempt < 4) {
                scheduleInjection(player, attempt + 1);
            }
        }, null, attempt == 0 ? 1L : 2L);
    }

    private boolean inject(Player player) {
        try {
            Channel channel = resolveChannel(player);
            if (channel == null) {
                return false;
            }
            Runnable install = () -> {
                try {
                    ChannelPipeline pipeline = channel.pipeline();
                    if (pipeline.get(HANDLER_NAME) != null) {
                        pipeline.remove(HANDLER_NAME);
                    }
                    if (pipeline.context(VANILLA_PACKET_HANDLER) == null) {
                        warnUnavailable("vanilla packet_handler was not found in the player pipeline", null);
                        return;
                    }
                    pipeline.addBefore(VANILLA_PACKET_HANDLER, HANDLER_NAME, new ChannelDuplexHandler() {
                        @Override
                        public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                            try {
                                if (isNoTargetSpectatorAction(message)) {
                                    dispatchPrimary(player);
                                }
                            } catch (Throwable ex) {
                                warnUnavailable("spectator primary packet decoding failed", ex);
                            }
                            super.channelRead(context, message);
                        }
                    });
                    channels.put(player.getUniqueId(), channel);
                } catch (Throwable ex) {
                    warnUnavailable("spectator primary pipeline injection failed", ex);
                }
            };
            if (channel.eventLoop().inEventLoop()) {
                install.run();
            } else {
                channel.eventLoop().execute(install);
            }
            return true;
        } catch (Throwable ex) {
            if (attemptable(ex)) {
                return false;
            }
            warnUnavailable("player network channel could not be resolved", ex);
            return false;
        }
    }

    private void dispatchPrimary(Player player) {
        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                primaryInput.accept(player);
            }
        }, null);
    }

    private static Channel resolveChannel(Player player) throws ReflectiveOperationException {
        Method getHandle = player.getClass().getMethod("getHandle");
        Object serverPlayer = getHandle.invoke(player);
        Object packetListener = readField(serverPlayer, "connection");
        Object connection = readField(packetListener, "connection");
        Object channel = readField(connection, "channel");
        return channel instanceof Channel nettyChannel ? nettyChannel : null;
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

    static boolean isNoTargetSpectatorAction(Object packet) {
        if (packet == null || !PACKET_SIMPLE_NAME.equals(packet.getClass().getSimpleName())) {
            return false;
        }
        for (Class<?> type = packet.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != OptionalInt.class || !field.trySetAccessible()) {
                    continue;
                }
                try {
                    Object value = field.get(packet);
                    return value instanceof OptionalInt optional && optional.isEmpty();
                } catch (IllegalAccessException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private void removeHandler(Channel channel) {
        Runnable remove = () -> {
            try {
                ChannelPipeline pipeline = channel.pipeline();
                if (pipeline.get(HANDLER_NAME) != null) {
                    pipeline.remove(HANDLER_NAME);
                }
            } catch (Throwable ignored) {
            }
        };
        if (channel.eventLoop().inEventLoop()) {
            remove.run();
        } else {
            channel.eventLoop().execute(remove);
        }
    }

    private static boolean attemptable(Throwable throwable) {
        return throwable instanceof NoSuchFieldException || throwable instanceof IllegalAccessException;
    }

    private void warnUnavailable(String reason, Throwable throwable) {
        if (!warnedUnavailable.compareAndSet(false, true)) {
            return;
        }
        String message = "Minecraft 26.2 spectator primary input bridge unavailable: " + reason + ". Bukkit input fallbacks remain active.";
        if (throwable == null) {
            plugin.getLogger().warning(message);
        } else {
            plugin.getLogger().log(java.util.logging.Level.WARNING, message, throwable);
        }
    }
}
