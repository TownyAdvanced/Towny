package com.palmergames.bukkit.util;

import org.bukkit.map.MinecraftFont;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.ApiStatus;
import solar.squares.pixelwidth.DefaultCharacterWidthFunction;
import solar.squares.pixelwidth.PixelWidthSource;

public class FontUtil {
	@ApiStatus.Internal
	public static final MinecraftFont font = new MinecraftFont();
	
	private static final PixelWidthSource widthSource = PixelWidthSource
			.pixelWidth(new DefaultCharacterWidthFunction() {
				@Override
				public float handleMissing(int codepoint, Style style) {
					// Use MinecraftFont as a backup
					try {
						return font.getWidth(
								String.valueOf((char) codepoint) + (style.hasDecoration(TextDecoration.BOLD) ? 1 : 0));
					} catch (IllegalArgumentException e) {
						return 6.0f;
					}
				}
			});

	public static float measureWidth(Component source) {
		return widthSource.width(source);
	}

	public static float measureWidth(String source) {
		return widthSource.width(source, Style.empty());
	}

	public static float measureWidth(String source, Style style) {
		return widthSource.width(source, style);
	}

	public static float measureWidth(char source) {
		return widthSource.width(source, Style.empty());
	}

	public static float measureWidth(char source, Style style) {
		return widthSource.width(source, style);
	}

	public static boolean isValidMinecraftFont(String text) {
		return font.isValid(text);
	}

}
