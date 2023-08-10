package com.palmergames.bukkit.towny.test;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BonusBlockPurchaseTests {
	
	static Town town;
	
	@BeforeAll
	static void init() {
		MockBukkit.getOrCreateMock();
		TownySettings.loadDefaultConfig();
		town = new Town("test");
		
		// No limit on bought townblocks
		TownySettings.getConfig().set(ConfigNodes.TOWN_MAX_PURCHASED_BLOCKS.getRoot(), Integer.MAX_VALUE);
		TownySettings.getConfig().set(ConfigNodes.TOWN_MAX_PURCHASED_BLOCKS_USES_TOWN_LEVELS.getRoot(), false);
	}
	
	@BeforeEach
	void reset() {
		TownySettings.getConfig().set(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK_INCREASE.getRoot(), 1);
		TownySettings.getConfig().set(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCKS_MAXIMUM.getRoot(), -1);
	}
	
	@Test
	void testBuy1000() {
		double cost = MoneyUtil.returnPurchasedBlocksCost(0, 1000, town);
		double expected = 1000 * 25;
		assertEquals(expected, cost);
	}
	
	@Test
	void testBuy2147483647() {
		double cost = MoneyUtil.returnPurchasedBlocksCost(0, Integer.MAX_VALUE, town);
		double expected = Integer.MAX_VALUE * 25D;
		assertEquals(expected, cost);
	}
	
	@Test
	void testBuy100Exponential() {
		TownySettings.getConfig().set(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK_INCREASE.getRoot(), 1.20D);
		
		double cost = MoneyUtil.returnPurchasedBlocksCost(0, 100, town);
		double expected = Math.round(25 * Math.pow(1.2, 100));
		assertEquals(expected, cost);
	}
	
	@Test
	void testBuy100ExponentialWithMaxPrice() {
		TownySettings.getConfig().set(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK_INCREASE.getRoot(), 1.20D);
		TownySettings.getConfig().set(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCKS_MAXIMUM.getRoot(), 200);

		double cost = MoneyUtil.returnPurchasedBlocksCost(0, 100, town);
		
		/* After purchasing 12 townblocks we go above the 200 limit:
		 * 25 * Math.pow(1.2, 11) = ~186
		 * 25 * Math.pow(1.2, 12) = ~223
		 * So to stay under the max price of 200, we need to use 11 instead of 12, since if we did 12 we'd be above it.
		 */
		double expected = Math.round(25 * Math.pow(1.2, 12 - 1) + (100 - 12 + 1) * 200);
		assertEquals(expected, cost);
	}
	
	@Test
	void testBuyJustEnough() {
		TownySettings.getConfig().set(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCK_INCREASE.getRoot(), 1.20D);
		TownySettings.getConfig().set(ConfigNodes.ECO_PRICE_PURCHASED_BONUS_TOWNBLOCKS_MAXIMUM.getRoot(), 200);

		double cost = MoneyUtil.returnPurchasedBlocksCost(0, 11, town);
		// 11 is not enough to reach our max price
		double expected = Math.round(25 * Math.pow(1.2, 11));
		
		assertEquals(expected, cost);
		
		// but 12 is
		expected = Math.round(25 * Math.pow(1.2, 11) + 200);
		cost = MoneyUtil.returnPurchasedBlocksCost(0, 12, town);
		assertEquals(expected, cost);
	}
}
