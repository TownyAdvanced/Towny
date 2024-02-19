package com.palmergames.bukkit.util;

import com.palmergames.util.StringMgmt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColorConversionTests {
	
	// https://github.com/TownyAdvanced/Towny/issues/6291
	// MiniMessage gradient tags should be untouched
	@Test
	void testNoHexGradientConversion() {
		final String gradient = "<gradient:#AA076B:#61045F>";
		assertEquals(gradient, Colors.translateLegacyHex(gradient));
	}
	
	// MiniMessage hex tags should also not be touched
	@Test
	void testNoMiniMessageConversion() {
		final String hex = "<#aaaaaa>";
		assertEquals(hex, Colors.translateLegacyHex(hex));
	}
	
	@Test
	void testUnusualRepeatingPattern() {
		final String hexFormat = "§x§a§a§a§a§a§a";
		assertEquals("<#aaaaaa>", Colors.translateLegacyHex(hexFormat));
	}
	
	@Test
	void testAmpersandPoundFormat() {
		String hexFormat = "&#aaaaaa";
		assertEquals("<#aaaaaa>", Colors.translateLegacyHex(hexFormat));
	}
	
	@Test
	void testBracketFormat() {
		String hexFormat = "{aaaaaa}";
		assertEquals("<#aaaaaa>", Colors.translateLegacyHex(hexFormat));
	}
	
	@Test
	void testMiniMessageToLegacy() {
		String hex = "<#aaaaaa>";
		assertEquals("§x§a§a§a§a§a§a", StringMgmt.translateHexColors(hex));
	}

	@Test
	void testLegacyConversions() {
		assertEquals(Colors.BLACK, Colors.translateLegacyCharacters(Colors.Black));
		assertEquals(Colors.DARK_BLUE, Colors.translateLegacyCharacters(Colors.Navy));
		assertEquals(Colors.DARK_GREEN, Colors.translateLegacyCharacters(Colors.Green));
		assertEquals(Colors.DARK_AQUA, Colors.translateLegacyCharacters(Colors.Blue));
		assertEquals(Colors.DARK_RED, Colors.translateLegacyCharacters(Colors.Red));
		assertEquals(Colors.DARK_PURPLE, Colors.translateLegacyCharacters(Colors.Purple));
		assertEquals(Colors.GOLD, Colors.translateLegacyCharacters(Colors.Gold));
		assertEquals(Colors.GRAY, Colors.translateLegacyCharacters(Colors.LightGray));
		assertEquals(Colors.DARK_GRAY, Colors.translateLegacyCharacters(Colors.Gray));
		assertEquals(Colors.BLUE, Colors.translateLegacyCharacters(Colors.DarkPurple));

		assertEquals(Colors.GREEN, Colors.translateLegacyCharacters(Colors.LightGreen));
		assertEquals(Colors.AQUA, Colors.translateLegacyCharacters(Colors.LightBlue));
		assertEquals(Colors.RED, Colors.translateLegacyCharacters(Colors.Rose));
		assertEquals(Colors.LIGHT_PURPLE, Colors.translateLegacyCharacters(Colors.LightPurple));
		assertEquals(Colors.YELLOW, Colors.translateLegacyCharacters(Colors.Yellow));
		assertEquals(Colors.WHITE, Colors.translateLegacyCharacters(Colors.White));

		assertEquals(Colors.OBFUSCATED, Colors.translateLegacyCharacters("§k"));
		assertEquals(Colors.BOLD, Colors.translateLegacyCharacters("§l"));
		assertEquals(Colors.STRIKETHROUGH, Colors.translateLegacyCharacters("§m"));
		assertEquals(Colors.UNDERLINED, Colors.translateLegacyCharacters("§n"));
		assertEquals(Colors.ITALIC, Colors.translateLegacyCharacters("§o"));
		assertEquals(Colors.RESET, Colors.translateLegacyCharacters("§r"));
	}

	@Test
	void testNoLegacyUnaffected() {
		assertEquals("test", Colors.translateLegacyCharacters("test"));
	}
}
