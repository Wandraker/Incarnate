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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class SpectatorPrimaryInputBridge implements Listener {
    private static final String HANDLER_NAME = "incarnate_spectator_primary";
    private static final String PACKET_SIMPLE_NAME = "ServerboundSpectatorActionPacket";
    private static final String PLAYER_ACTION_PACKET_SIMPLE_NAME = "ServerboundPlayerActionPacket";
    private static final String SWAP_OFFHAND_ACTION = "SWAP_ITEM_WITH_OFFHAND";
    private static final String VANILLA_PACKET_HANDLER = "packet_handler";
    private static final int MAX_INJECTION_ATTEMPTS = 5;

    private final IncarnatePlugin plugin;
    private final PrimaryInputDeduplicator deduplicator;
    private final SecondaryInputDeduplicator secondaryInputDeduplicator;
    private final Consumer<Player> primaryInput;
    private final Consumer<Player> secondaryInput;
    private final Map<UUID, Channel> channels = new ConcurrentHashMap<>();
    private final AtomicBoolean warnedUnavailable = new AtomicBoolean(false);

    public SpectatorPrimaryInputBridge(
        IncarnatePlugin plugin,
        PrimaryInputDeduplicator deduplicator,
        SecondaryInputDeduplicator secondaryInputDeduplicator,
        Consumer<Player> primaryInput,
        Consumer<Player> secondaryInput
    ) {
        this.plugin = plugin;
        this.deduplicator = deduplicator;
        this.secondaryInputDeduplicator = secondaryInputDeduplicator;
        this.primaryInput = primaryInput;
        this.secondaryInput = secondaryInput;
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
        UUID playerId = event.getPlayer().getUniqueId();
        Channel channel = channels.remove(playerId);
        if (channel != null) {
            removeHandler(channel);
        }
        deduplicator.clear(playerId);
        secondaryInputDeduplicator.clear(playerId);
    }

    private void scheduleInjection(Player player, int attempt) {
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline() || channels.containsKey(player.getUniqueId())) {
                return;
            }
            inject(player, attempt);
        }, null, attempt == 0 ? 1L : 2L);
    }

    private void inject(Player player, int attempt) {
        final Channel channel;
        try {
            channel = resolveChannel(player);
        } catch (Throwable ex) {
            retryOrWarn(player, attempt, "player network channel could not be resolved", ex);
            return;
        }
        if (channel == null) {
            retryOrWarn(player, attempt, "player network channel was not available", null);
            return;
        }

        Runnable install = () -> {
            if (!player.isOnline() || channels.containsKey(player.getUniqueId())) {
                return;
            }
            try {
                ChannelPipeline pipeline = channel.pipeline();
                if (pipeline.get(HANDLER_NAME) != null) {
                    pipeline.remove(HANDLER_NAME);
                }
                if (pipeline.context(VANILLA_PACKET_HANDLER) == null) {
                    retryOrWarn(player, attempt, "vanilla packet_handler was not found in the player pipeline", null);
                    return;
                }
                pipeline.addBefore(VANILLA_PACKET_HANDLER, HANDLER_NAME, new ChannelDuplexHandler() {
                    @Override
                    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
                        try {
                            long nowNanos = System.nanoTime();
                            if (isSpectatorPrimaryAction(message)) {
                                deduplicator.markSpectatorPacket(player.getUniqueId(), nowNanos);
                                dispatchPrimary(player);
                            } else if (isSwapOffhandAction(message)) {
                                secondaryInputDeduplicator.markPacket(player.getUniqueId(), nowNanos);
                                dispatchSecondary(player);
                            }
                        } catch (Throwable ex) {
                            warnUnavailable("spectator input packet decoding failed", ex);
                        }
                        super.channelRead(context, message);
                    }
                });
                channels.put(player.getUniqueId(), channel);
            } catch (Throwable ex) {
                retryOrWarn(player, attempt, "spectator input pipeline injection failed", ex);
            }
        };

        try {
            if (channel.eventLoop().inEventLoop()) {
                install.run();
            } else {
                channel.eventLoop().execute(install);
            }
        } catch (Throwable ex) {
            retryOrWarn(player, attempt, "spectator input event-loop dispatch failed", ex);
        }
    }

    private void retryOrWarn(Player player, int attempt, String reason, Throwable throwable) {
        if (!player.isOnline() || channels.containsKey(player.getUniqueId())) {
            return;
        }
        if (attempt + 1 < MAX_INJECTION_ATTEMPTS) {
            scheduleInjection(player, attempt + 1);
            return;
        }
        warnUnavailable(reason, throwable);
    }

    private void dispatchPrimary(Player player) {
        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                primaryInput.accept(player);
            }
        }, null);
    }

    private void dispatchSecondary(Player player) {
        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                secondaryInput.accept(player);
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

    static boolean isSpectatorPrimaryAction(Object packet) {
        return packet != null && PACKET_SIMPLE_NAME.equals(packet.getClass().getSimpleName());
    }

    static boolean isSwapOffhandAction(Object packet) {
        if (packet == null || !PLAYER_ACTION_PACKET_SIMPLE_NAME.equals(packet.getClass().getSimpleName())) {
            return false;
        }
        return SWAP_OFFHAND_ACTION.equals(playerActionName(packet));
    }

    static String playerActionName(Object packet) {
        if (packet == null) {
            return null;
        }
        for (String accessorName : new String[]{"getAction", "action"}) {
            try {
                Method accessor = packet.getClass().getMethod(accessorName);
                if (!accessor.trySetAccessible()) {
                    continue;
                }
                Object value = accessor.invoke(packet);
                if (value instanceof Enum<?> action) {
                    return action.name();
                }
            } catch (ReflectiveOperationException | SecurityException ignored) {
            }
        }
        for (Class<?> type = packet.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().isEnum() || !field.trySetAccessible()) {
                    continue;
                }
                try {
                    Object value = field.get(packet);
                    if (value instanceof Enum<?> action) {
                        return action.name();
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        return null;
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
        try {
            if (channel.eventLoop().inEventLoop()) {
                remove.run();
            } else {
                channel.eventLoop().execute(remove);
            }
        } catch (Throwable ignored) {
        }
    }

    private void warnUnavailable(String reason, Throwable throwable) {
        if (!warnedUnavailable.compareAndSet(false, true)) {
            return;
        }
        String message = "Minecraft 26.2 spectator input bridge unavailable: " + reason + ". Bukkit input fallbacks remain active.";
        if (throwable == null) {
            plugin.getLogger().warning(message);
        } else {
            plugin.getLogger().log(java.util.logging.Level.WARNING, message, throwable);
        }
    }
}
