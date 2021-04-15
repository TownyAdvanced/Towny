package com.palmergames.bukkit.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.format.NamedTextColor;

public class Colors {

	public static final String Black = "\u00A70";
	public static final String Navy = "\u00A71";
	public static final String Green = "\u00A72";
	public static final String Blue = "\u00A73";
	public static final String Red = "\u00A74";
	public static final String Purple = "\u00A75";
	public static final String Gold = "\u00A76";
	public static final String LightGray = "\u00A77";
	public static final String Gray = "\u00A78";
	public static final String DarkPurple = "\u00A79";
	public static final String LightGreen = "\u00A7a";
	public static final String LightBlue = "\u00A7b";
	public static final String Rose = "\u00A7c";
	public static final String LightPurple = "\u00A7d";
	public static final String Yellow = "\u00A7e";
	public static final String White = "\u00A7f";

	public static String strip(String line) {

		for (ChatColor cc : ChatColor.values())
			line = line.replaceAll(cc.toString(), "");
		return line;
	}

	public static String translateColorCodes(String str) {
		return ChatColor.translateAlternateColorCodes('&', str);
	}

	/**
	 * @param colorCode The color code.
	 * @return the {@link NamedTextColor} for the entered color string, or null if it is invalid.
	 */
	@Nullable
	public static NamedTextColor toNamedTextColor(String colorCode) {
		switch(colorCode) {
			case "\u00A70":
				return NamedTextColor.BLACK;
			case "\u00A71":
				return NamedTextColor.DARK_BLUE;
			case "\u00A72":
				return NamedTextColor.DARK_GREEN;
			case "\u00A73":
				return NamedTextColor.DARK_AQUA;
			case "\u00A74":
				return NamedTextColor.DARK_RED;
			case "\u00A75":
				return NamedTextColor.DARK_PURPLE;
			case "\u00A76":
				return NamedTextColor.GOLD;
			case "\u00A77":
				return NamedTextColor.GRAY;
			case "\u00A78":
				return NamedTextColor.DARK_GRAY;
			case "\u00A79":
				return NamedTextColor.BLUE;
			case "\u00A7a":
				return NamedTextColor.GREEN;
			case "\u00A7b":
				return NamedTextColor.AQUA;
			case "\u00A7c":
				return NamedTextColor.RED;
			case "\u00A7d":
				return NamedTextColor.LIGHT_PURPLE;
			case "\u00A7e":
				return NamedTextColor.YELLOW;
			case "\u00A7f":
				return NamedTextColor.WHITE;
			default:
				return null;
		}
	}
}
