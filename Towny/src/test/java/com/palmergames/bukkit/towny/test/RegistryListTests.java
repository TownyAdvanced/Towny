package com.palmergames.bukkit.towny.test;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.palmergames.bukkit.util.ItemLists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RegistryListTests {
	
	@BeforeAll
	static void mock() {
		MockBukkit.mock();
	}
	
	@Test
	void testListAdd() {
		ItemLists itemList = ItemLists.newBuilder().add("stone").build();
		assertEquals(1, itemList.tagged().size());
	}
}
