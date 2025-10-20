package com.palmergames.bukkit.util;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Colors {

	private static final Map<String, String> LEGACY_LOOKUP = new HashMap<>();
	private static final Pattern LEGACY_PATTERN = Pattern.compile("[§&][0-9a-fk-or]");

	/*
	 * Legacy colors
	 */
	public static final @Deprecated(since = "0.101.2.6") String Black = "§0";       // black
	public static final @Deprecated(since = "0.101.2.6") String Navy = "§1";        // dark_blue
	public static final @Deprecated(since = "0.101.2.6") String Green = "§2";       // dark_green
	public static final @Deprecated(since = "0.101.2.6") String Blue = "§3";        // dark_aqua
	public static final @Deprecated(since = "0.101.2.6") String Red = "§4";         // dark_red
	public static final @Deprecated(since = "0.101.2.6") String Purple = "§5";      // dark_purple
	public static final @Deprecated(since = "0.101.2.6") String Gold = "§6";        // gold
	public static final @Deprecated(since = "0.101.2.6") String LightGray = "§7";   // gray
	public static final @Deprecated(since = "0.101.2.6") String Gray = "§8";        // dark_gray
	public static final @Deprecated(since = "0.101.2.6") String DarkPurple = "§9";  // blue
	public static final @Deprecated(since = "0.101.2.6") String LightGreen = "§a";  // green
	public static final @Deprecated(since = "0.101.2.6") String LightBlue = "§b";   // aqua
	public static final @Deprecated(since = "0.101.2.6") String Rose = "§c";        // red
	public static final @Deprecated(since = "0.101.2.6") String LightPurple = "§d"; // light_purple
	public static final @Deprecated(since = "0.101.2.6") String Yellow = "§e";      // yellow
	public static final @Deprecated(since = "0.101.2.6") String White = "§f";       // white

	/*
	 * Minimessage colors 
	 */
	public static final String DARK_RED = "<dark_red>";
	public static final String RED = "<red>";
	public static final String GOLD = "<gold>";
	public static final String YELLOW = "<yellow>";
	public static final String DARK_GREEN = "<dark_green>";
	public static final String GREEN = "<green>";
	public static final String DARK_AQUA = "<dark_aqua>";
	public static final String AQUA = "<aqua>";
	public static final String DARK_BLUE = "<dark_blue>";
	public static final String BLUE = "<blue>";
	public static final String LIGHT_PURPLE = "<light_purple>";
	public static final String DARK_PURPLE = "<dark_purple>";
	public static final String WHITE = "<white>";
	public static final String GRAY = "<gray>";
	public static final String DARK_GRAY = "<dark_gray>";
	public static final String BLACK = "<black>";

	public static final String OBFUSCATED = "<obfuscated>";
	public static final String BOLD = "<bold>";
	public static final String STRIKETHROUGH = "<strikethrough>";
	public static final String UNDERLINED = "<underlined>";
	public static final String ITALIC = "<italic>";
	public static final String RESET = "<reset>";

	public static String strip(String line) {
		return TownyComponents.stripTags(ChatColor.stripColor(line));
	}

	public static String translateColorCodes(String str) {
		return StringMgmt.translateHexColors(ChatColor.translateAlternateColorCodes('&', str));
	}

	public static String translateLegacyCharacters(String input) {
		final Matcher matcher = LEGACY_PATTERN.matcher(input);

		while (matcher.find()) {
			String legacy = matcher.group();
			input = input.replace(legacy, LEGACY_LOOKUP.getOrDefault(legacy.substring(1), legacy));
		}

		return input;
	}

	private static final Function<String, String> modernHexFunction = (hex) -> "<#" + hex + ">";

	/**
	 * Converts non-minimessage hex formats to minimessage.
	 * @param input The input that may or may not contain hex.
	 * @return The input, with the minimessage hex format.
	 */
	public static String translateLegacyHex(String input) {
		return StringMgmt.translateHexColors(input, modernHexFunction);
	}

	/**
	 * @param color A legacy color code or a MiniMessage {@code <color>} tag
	 * @return the {@link NamedTextColor} for the entered color string, or null if it is invalid.
	 */
	@Nullable
	public static NamedTextColor toNamedTextColor(@NotNull String color) {
		return switch (color) {
			case "§0", BLACK -> NamedTextColor.BLACK;
			case "§1", DARK_BLUE -> NamedTextColor.DARK_BLUE;
			case "§2", DARK_GREEN -> NamedTextColor.DARK_GREEN;
			case "§3", DARK_AQUA -> NamedTextColor.DARK_AQUA;
			case "§4", DARK_RED -> NamedTextColor.DARK_RED;
			case "§5", DARK_PURPLE -> NamedTextColor.DARK_PURPLE;
			case "§6", GOLD -> NamedTextColor.GOLD;
			case "§7", GRAY -> NamedTextColor.GRAY;
			case "§8", DARK_GRAY -> NamedTextColor.DARK_GRAY;
			case "§9", BLUE -> NamedTextColor.BLUE;
			case "§a", GREEN -> NamedTextColor.GREEN;
			case "§b", AQUA -> NamedTextColor.AQUA;
			case "§c", RED -> NamedTextColor.RED;
			case "§d", LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE;
			case "§e", YELLOW -> NamedTextColor.YELLOW;
			case "§f", WHITE -> NamedTextColor.WHITE;
			default -> null;
		};
	}

	public static String colorTown(String townName) {
		return translateColorCodes(getTownColor() + townName);
	}

	public static String colorTown(Town town) {
		return translateColorCodes(getTownColor() + town);
	}

	public static String colorNation(String nationName) {
		return translateColorCodes(getNationColor() + nationName);
	}

	public static String colorNation(Nation nation) {
		return translateColorCodes(getNationColor() + nation);
	}

	public static String getTownColor() {
		return TownySettings.getPAPIFormattingMayor();
	}

	public static String getNationColor() {
		return TownySettings.getPAPIFormattingKing();
	}

	public static boolean containsColourCode(String input) {
		return LEGACY_PATTERN.matcher(input).find() || input.length() != strip(input).length();
	}

	static {
		LEGACY_LOOKUP.put("0", BLACK);
		LEGACY_LOOKUP.put("1", DARK_BLUE);
		LEGACY_LOOKUP.put("2", DARK_GREEN);
		LEGACY_LOOKUP.put("3", DARK_AQUA);
		LEGACY_LOOKUP.put("4", DARK_RED);
		LEGACY_LOOKUP.put("5", DARK_PURPLE);
		LEGACY_LOOKUP.put("6", GOLD);
		LEGACY_LOOKUP.put("7", GRAY);
		LEGACY_LOOKUP.put("8", DARK_GRAY);
		LEGACY_LOOKUP.put("9", BLUE);
		LEGACY_LOOKUP.put("a", GREEN);
		LEGACY_LOOKUP.put("b", AQUA);
		LEGACY_LOOKUP.put("c", RED);
		LEGACY_LOOKUP.put("d", LIGHT_PURPLE);
		LEGACY_LOOKUP.put("e", YELLOW);
		LEGACY_LOOKUP.put("f", WHITE);

		LEGACY_LOOKUP.put("k", OBFUSCATED);
		LEGACY_LOOKUP.put("l", BOLD);
		LEGACY_LOOKUP.put("m", STRIKETHROUGH);
		LEGACY_LOOKUP.put("n", UNDERLINED);
		LEGACY_LOOKUP.put("o", ITALIC);
		LEGACY_LOOKUP.put("r", RESET);
	}

	public static String getLegacyFromNamedTextColor(NamedTextColor colour) {
		return switch ("<" +colour.toString() + ">") {
		case BLACK ->"§0";
		case DARK_BLUE -> "§1";
		case DARK_GREEN -> "§2";
		case DARK_AQUA -> "§3";
		case DARK_RED -> "§4";
		case DARK_PURPLE -> "§5";
		case GOLD -> "§6";
		case GRAY -> "§7";
		case DARK_GRAY -> "§8";
		case BLUE -> "§9";
		case GREEN -> "§a";
		case AQUA -> "§b";
		case RED -> "§c";
		case LIGHT_PURPLE -> "§d";
		case YELLOW -> "§e";
		case WHITE -> "§f";
		default -> null;
	};
	}
}
