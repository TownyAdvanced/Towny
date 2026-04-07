package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.util.ItemLists;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RegistryListTests {
	
	@Test
	void testListAdd() {
		ItemLists itemList = ItemLists.newBuilder().add("stone").build();
		assertEquals(1, itemList.tagged().size());
	}
	
	@Test
	void testMissingListAddDoesntAddEverything() {
		ItemLists itemList = ItemLists.newBuilder().add("materialthatdefinitelyexists").build();
		assertEquals(0, itemList.tagged().size());
	}
	
	@Test
	void testNothingAddedToTheBuilderGivesEverything() {
		assertTrue(ItemLists.newBuilder().build().tagged().size() > 100);
	}
}
