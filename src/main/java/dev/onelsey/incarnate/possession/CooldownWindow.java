package dev.onelsey.incarnate.possession;

public final class CooldownWindow {
    private volatile long untilTick = Long.MIN_VALUE;

    public boolean tryAcquire(long nowTick, int cooldownTicks) {
        long until = untilTick;
        if (until != Long.MIN_VALUE && nowTick < until) {
            return false;
        }
        untilTick = nowTick + Math.max(1, cooldownTicks);
        return true;
    }

    public int remaining(long nowTick) {
        long until = untilTick;
        if (until == Long.MIN_VALUE || nowTick >= until) {
            return 0;
        }
        long remaining = until - nowTick;
        return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }
}
