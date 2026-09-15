package dev.onelsey.incarnate.input;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SecondaryInputDeduplicator {
    private static final long DEFAULT_PACKET_SHIELD_NANOS = 100_000_000L;

    private final long packetShieldNanos;
    private final ConcurrentHashMap<UUID, State> states = new ConcurrentHashMap<>();

    public SecondaryInputDeduplicator() {
        this(DEFAULT_PACKET_SHIELD_NANOS);
    }

    SecondaryInputDeduplicator(long packetShieldNanos) {
        this.packetShieldNanos = Math.max(0L, packetShieldNanos);
    }

    public void markPacket(UUID playerId, long nowNanos) {
        State state = states.computeIfAbsent(playerId, ignored -> new State());
        state.lastPacketSeenNanos = nowNanos;
    }

    public boolean accept(UUID playerId, SecondaryInputTransport transport, long nowNanos) {
        State state = states.computeIfAbsent(playerId, ignored -> new State());
        synchronized (state) {
            if (transport == SecondaryInputTransport.PLAYER_ACTION_PACKET) {
                return true;
            }

            long lastPacket = state.lastPacketSeenNanos;
            if (lastPacket != Long.MIN_VALUE) {
                long delta = nowNanos - lastPacket;
                if (delta >= 0L && delta <= packetShieldNanos) {
                    return false;
                }
            }
            return true;
        }
    }

    public void clear(UUID playerId) {
        states.remove(playerId);
    }

    private static final class State {
        private volatile long lastPacketSeenNanos = Long.MIN_VALUE;
    }
}
