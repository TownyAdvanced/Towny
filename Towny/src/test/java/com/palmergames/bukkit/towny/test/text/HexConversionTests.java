package com.palmergames.bukkit.towny.test.text;

import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HexConversionTests {
	
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
}
