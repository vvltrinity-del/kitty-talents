package com.kittytalents.client.gui.theme;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Companion-screen strings. Every label goes through a "gui.kittytalents.screen.*" key with an English fallback, so
 * the screen works before a translation exists and en_us.json can be regenerated from the call sites.
 */
public final class KTText {
    private KTText() {}

    public static MutableComponent t(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("gui.kittytalents.screen." + key, fallback, args);
    }

    public static String s(String key, String fallback, Object... args) {
        return t(key, fallback, args).getString();
    }
}
