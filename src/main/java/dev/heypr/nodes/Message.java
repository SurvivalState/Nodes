package dev.heypr.nodes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Message {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String DEFAULT_PREFIX = "&7[&bNodes&7] &r";

    public static TextComponent of(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return LEGACY.deserialize(text).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static TextComponent prefixed(String text) {
        return of(prefix()).append(of(text));
    }

    private static String prefix() {
        Nodes plugin = Nodes.get();
        return plugin == null ? DEFAULT_PREFIX : plugin.getConfig().getString("messages.prefix", DEFAULT_PREFIX);
    }
}
