package dev.onelsey.incarnate.input;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PrimaryInputDeduplicator {
    private static final long DEFAULT_PACKET_SHIELD_NANOS = 100_000_000L;
    private static final long DEFAULT_FALLBACK_MERGE_NANOS = 35_000_000L;

    private final long packetShieldNanos;
    private final long fallbackMergeNanos;
    private final ConcurrentHashMap<UUID, State> states = new ConcurrentHashMap<>();

    public PrimaryInputDeduplicator() {
        this(DEFAULT_PACKET_SHIELD_NANOS, DEFAULT_FALLBACK_MERGE_NANOS);
    }

    PrimaryInputDeduplicator(long packetShieldNanos, long fallbackMergeNanos) {
        this.packetShieldNanos = Math.max(0L, packetShieldNanos);
        this.fallbackMergeNanos = Math.max(0L, fallbackMergeNanos);
    }

    public void markSpectatorPacket(UUID playerId, long nowNanos) {
        State state = states.computeIfAbsent(playerId, ignored -> new State());
        state.lastPacketSeenNanos = nowNanos;
    }

    public boolean accept(UUID playerId, PrimaryInputTransport transport, long nowNanos) {
        State state = states.computeIfAbsent(playerId, ignored -> new State());
        synchronized (state) {
            if (transport == PrimaryInputTransport.SPECTATOR_PACKET) {
                state.lastAcceptedTransport = transport;
                state.lastAcceptedNanos = nowNanos;
                return true;
            }

            long lastPacket = state.lastPacketSeenNanos;
            if (lastPacket != Long.MIN_VALUE) {
                long delta = nowNanos - lastPacket;
                if (delta >= 0L && delta <= packetShieldNanos) {
                    return false;
                }
            }

            if (state.lastAcceptedTransport != null && state.lastAcceptedTransport != transport) {
                long delta = nowNanos - state.lastAcceptedNanos;
                if (delta >= 0L && delta <= fallbackMergeNanos) {
                    return false;
                }
            }

            state.lastAcceptedTransport = transport;
            state.lastAcceptedNanos = nowNanos;
            return true;
        }
    }

    public void clear(UUID playerId) {
        states.remove(playerId);
    }

    private static final class State {
        private volatile long lastPacketSeenNanos = Long.MIN_VALUE;
        private long lastAcceptedNanos = Long.MIN_VALUE;
        private PrimaryInputTransport lastAcceptedTransport;
    }
}
