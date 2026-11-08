package com.kittytalents.util;

/**
 * Off-by-default timer for server-side Talented Cat ticks (Phase 8 stress tests). When enabled, TalentedCat#tick adds
 * its own wall time here, so a test can report the cats' cost per server tick without the noise of everything else
 * the server does. Costs one boolean check per cat tick when disabled.
 */
public final class TickCost {
    private TickCost() {}

    public static volatile boolean enabled;
    private static long nanos;
    private static long catTicks;

    public static void reset() { nanos = 0; catTicks = 0; }
    public static void add(long n) { nanos += n; catTicks++; }
    public static long nanos() { return nanos; }
    public static long catTicks() { return catTicks; }
}
