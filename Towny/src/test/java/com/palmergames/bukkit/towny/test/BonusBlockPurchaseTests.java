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
		double expected = Math.round(25 * Math.pow(1.2, 12) + (100 - 12) * 200);
		assertEquals(expected, cost); 
	}
}
