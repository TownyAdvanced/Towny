package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.util.Colors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal utility class for common interactions with adventure components.
 */
@ApiStatus.Internal
public class TownyComponents {
	// A minimessage instance with no tags
	private static final MiniMessage EMPTY = MiniMessage.builder().tags(TagResolver.empty()).build();

	/**
	 * A minimessage instance with tags that are deemed safe for players to be able to use.
	 */
	public static final MiniMessage USER_SAFE = MiniMessage.builder()
		.tags(TagResolver.builder()
			.resolver(StandardTags.color())
			.resolvers(StandardTags.decorations())
			.resolvers(StandardTags.gradient())
			.resolvers(StandardTags.rainbow())
			.resolvers(getRecentlyAddedTagResolvers())
			.build())
		.build();
	
	public static Component miniMessage(@NotNull String string) {
		return MiniMessage.miniMessage().deserialize(Colors.translateLegacyCharacters(Colors.translateLegacyHex(string)));
	}

	public static String toMiniMessage(@NotNull Component component) {
		return MiniMessage.miniMessage().serialize(component);
	}
	
	public static String plain(@NotNull Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component);
	}
	
	/**
	 * Converts legacy text to a component
	 * @param string The input string with legacy ampersand/section characters
	 * @return The component equivalent
	 */
	public static Component legacy(@NotNull String string) {
		return LegacyComponentSerializer.legacySection().deserialize(Colors.translateColorCodes(string));
	}
	
	/**
	 * Converts a component to a legacy string using the legacy serializer
	 * @param component The component to convert
	 * @return A string with legacy section characters
	 */
	public static String toLegacy(@NotNull Component component) {
		return LegacyComponentSerializer.legacySection().serialize(component);
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
	
	public static Component joinList(List<Component> components, Component delimiter) {
		Component full = Component.empty();
		
		for (int i = 0; i < components.size(); i++) {
			full = Component.empty().append(full).append(components.get(i));
			
			if (i != components.size() - 1)
				full = Component.empty().append(full).append(delimiter);
		}
		
		return full;
	}

	// Uses reflection to retrieve standard tag resolvers that aren't present in the current minimum supported version.
	private static List<TagResolver> getRecentlyAddedTagResolvers() {
		final List<TagResolver> resolvers = new ArrayList<>();
		addTagResolver("shadowColor", resolvers);
		addTagResolver("pride", resolvers);

		return resolvers;
	}

	private static void addTagResolver(final String name, final List<TagResolver> resolvers) {
		try {
			resolvers.add((TagResolver) StandardTags.class.getMethod(name).invoke(null));
		} catch (ReflectiveOperationException ignored) {}
	}
}
