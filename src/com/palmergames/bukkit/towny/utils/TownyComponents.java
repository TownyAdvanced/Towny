package com.palmergames.bukkit.towny.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

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
}
