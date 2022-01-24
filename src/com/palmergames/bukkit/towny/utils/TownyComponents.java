package com.palmergames.bukkit.towny.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Utility class for common interactions with adventure components.
 */
public final class TownyComponents {
	public static String plain(Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component);
	}
	
	public static Component miniMessage(String input) {
		return MiniMessage.miniMessage().parse(input);
	}
	
	public static Component fromLegacy(String input) {
		return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
	}
	
	public static String toLegacy(Component component) {
		return LegacyComponentSerializer.legacyAmpersand().serialize(component);
	}
}
