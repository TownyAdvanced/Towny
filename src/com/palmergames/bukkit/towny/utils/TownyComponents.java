package com.palmergames.bukkit.towny.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import com.palmergames.bukkit.util.Colors;

import java.util.List;

/**
 * Utility class for common interactions with adventure components.
 */
public final class TownyComponents {
	// A minimessage instance with no tags
	private static final MiniMessage EMPTY = MiniMessage.builder().tags(TagResolver.empty()).build();
	
	public static String plain(@NotNull Component input) {
		return PlainTextComponentSerializer.plainText().serialize(input);
	}
	
	public static Component miniMessage(@NotNull String input) {
		return MiniMessage.miniMessage().deserialize(input);
	}
	
	public static Component miniMessageAndColour(@NotNull String input) {
		return miniMessage(Colors.translateColorCodes(input));
	}
	
	public static Component miniMessage(@NotNull Component component) {
		// Feels a bit hacky but seems like the easiest way
		// Basically 'adds up' component colors in order to color it like legacy
		return miniMessage(unMiniMessage(component));
	}
	
	public static String unMiniMessage(@NotNull Component input) {
		return MiniMessage.miniMessage().serialize(input);
	}
	
	/**
	 * Converts legacy text to a component
	 * @param input The input string with legacy ampersand characters
	 * @return The component
	 */
	public static Component fromLegacy(@NotNull String input) {
		return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
	}
	
	/**
	 * Converts a component to a legacy string using the legacy serializer
	 * @param component The component
	 * @return A string with legacy ampersand characters
	 */
	public static String toLegacy(@NotNull Component component) {
		return LegacyComponentSerializer.legacyAmpersand().serialize(component);
	}
	
	/**
	 * Strips all tags known to the default minimessage instance.
	 * @param input The input
	 * @return The stripped output
	 */
	public static String stripTags(@NotNull String input) {
		return MiniMessage.miniMessage().stripTags(input);
	}
	
	/**
	 * Strips the specified tags from the input
	 * @param input The input that may contain tags
	 * @param resolvers The resolver(s) for tags to strip from the input
	 * @return The stripped output
	 */
	public static String stripTags(@NotNull String input, TagResolver... resolvers) {
		return EMPTY.stripTags(input, resolvers);
	}
	
	public static String stripClickTags(@NotNull String input) {
		return stripTags(input, StandardTags.clickEvent());
	}
	
	/**
	 * Prepends the specified input to the component, useful for components that might not contain color.
	 * @param component The component
	 * @param prepend The string to prepend
	 * @return The prepended component
	 */
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
