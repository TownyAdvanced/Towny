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
		assertEquals(Colors.BLACK, Colors.translateLegacyCharacters("§0"));
		assertEquals(Colors.DARK_BLUE, Colors.translateLegacyCharacters("§1"));
		assertEquals(Colors.DARK_GREEN, Colors.translateLegacyCharacters("§2"));
		assertEquals(Colors.DARK_AQUA, Colors.translateLegacyCharacters("§3"));
		assertEquals(Colors.DARK_RED, Colors.translateLegacyCharacters("§4"));
		assertEquals(Colors.DARK_PURPLE, Colors.translateLegacyCharacters("§5"));
		assertEquals(Colors.GOLD, Colors.translateLegacyCharacters("§6"));
		assertEquals(Colors.GRAY, Colors.translateLegacyCharacters("§7"));
		assertEquals(Colors.DARK_GRAY, Colors.translateLegacyCharacters("§8"));
		assertEquals(Colors.BLUE, Colors.translateLegacyCharacters("§9"));

		assertEquals(Colors.GREEN, Colors.translateLegacyCharacters("§a"));
		assertEquals(Colors.AQUA, Colors.translateLegacyCharacters("§b"));
		assertEquals(Colors.RED, Colors.translateLegacyCharacters("§c"));
		assertEquals(Colors.LIGHT_PURPLE, Colors.translateLegacyCharacters("§d"));
		assertEquals(Colors.YELLOW, Colors.translateLegacyCharacters("§e"));
		assertEquals(Colors.WHITE, Colors.translateLegacyCharacters("§f"));

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
