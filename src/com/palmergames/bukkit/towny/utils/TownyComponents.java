package com.palmergames.bukkit.towny.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Utility class for common interactions with adventure components.
 */
public final class TownyComponents {
	public static String plain(@NotNull Component input) {
		return PlainTextComponentSerializer.plainText().serialize(input);
	}
	
	public static Component miniMessage(@NotNull String input) {
		return MiniMessage.miniMessage().deserialize(input);
	}
	
	public static String unMiniMessage(@NotNull Component input) {
		return MiniMessage.miniMessage().serialize(input);
	}
	
	public static Component fromLegacy(@NotNull String input) {
		return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
	}
	
	public static String toLegacy(@NotNull Component component) {
		return LegacyComponentSerializer.legacyAmpersand().serialize(component);
	}
	
	public static String stripTags(@NotNull Component input) {
		return MiniMessage.miniMessage().stripTags(unMiniMessage(input));
	}
	
	public static Component prependMiniMessage(@NotNull Component component, @NotNull String prepend) {
		return miniMessage(prepend + unMiniMessage(component));
	}
	
	public static Component joinList(@NotNull List<@NotNull Component> components, @NotNull Component separator) {
		Component full = Component.empty();
		
		for (int i = 0; i < components.size(); i++) {
			full = full.append(components.get(i));
			
			if (i != components.size() - 1)
				full = full.append(separator);
		}

		return full;
	}
}
