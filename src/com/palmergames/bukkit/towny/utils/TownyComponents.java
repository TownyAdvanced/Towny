package com.palmergames.bukkit.towny.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Internal utility class for common interactions with adventure components.
 */
@ApiStatus.Internal
public class TownyComponents {
	public static String plain(@NotNull Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component);
	}
	
	public static Component legacySection(@NotNull String string) {
		return LegacyComponentSerializer.legacySection().deserialize(string);
	}
	
	public static Component legacyAmpersand(@NotNull String string) {
		return LegacyComponentSerializer.legacyAmpersand().deserialize(string);
	}
	
	public static String toLegacy(@NotNull Component component) {
		return LegacyComponentSerializer.legacySection().serialize(component);
	}
	
	public static Component joinList(List<Component> components, Component delimiter) {
		Component full = Component.empty();
		
		for (int i = 0; i < components.size(); i++) {
			full = Component.empty().append(full).append(components.get(i));
			
			if (i != components.size() - 1)
				full = Component.empty().append(full).append(delimiter);
		}
		
		return full;
	}
}
