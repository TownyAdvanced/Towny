package com.palmergames.bukkit.towny.object;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.EntityLists;
import com.palmergames.bukkit.util.ItemLists;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.bukkit.Material.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class RegistryListTests {
	
	@BeforeAll
	static void mock() {
		MockBukkit.getOrCreateMock();
		TownySettings.loadDefaultConfig();
	}
	
	@Test
	void testListAdd() {
		ItemLists itemList = ItemLists.newBuilder().add("stone").build();
		assertEquals(1, itemList.tagged().size());
	}
	
	@Test
	void testCustomRegistryListBuilderClassNames() {
		EntityLists built = TownySettings.constructRegistryList(EntityLists.newBuilder(), Tag.REGISTRY_ENTITY_TYPES, Arrays.asList("c:Animals"), EntityType::getEntityClass);
		assertEquals(EntityLists.ANIMALS.tagged(), built.tagged());

		Set<Material> expected = new HashSet<>(Set.of(WEEPING_VINES, NETHER_WART, FROSTED_ICE, SWEET_BERRY_BUSH, CACTUS, COCOA, CAVE_VINES, PITCHER_CROP, BEETROOTS, SUGAR_CANE, CHORUS_FLOWER, FIRE, BAMBOO, POTATOES, MELON_STEM, CARROTS, TORCHFLOWER_CROP, WHEAT, MANGROVE_PROPAGULE, KELP, TWISTING_VINES, PUMPKIN_STEM));
		List<String> input = new ArrayList<>(Arrays.asList("c:org.bukkit.block.data.Ageable"));
		
		ItemLists list = TownySettings.constructRegistryList(ItemLists.newBuilder(), Tag.REGISTRY_BLOCKS, input, type -> type.data);
		assertEquals(expected, list.tagged());
		
		expected.remove(WEEPING_VINES);
		input.add("-weeping_vines");
		list = TownySettings.constructRegistryList(ItemLists.newBuilder(), Tag.REGISTRY_BLOCKS, input, type -> type.data);
		assertEquals(expected, list.tagged());
	}
	
	@Test
	void testMinimalEntityCollection() {
		List<String> input = List.of("wolf", "monsters");
		List<EntityType> inputAsEntityType = new ArrayList<>(TownySettings.constructRegistryList(EntityLists.newBuilder(), Tag.REGISTRY_ENTITY_TYPES, Arrays.asList("+wolf", "c:Monster"), EntityType::getEntityClass).tagged);
		
		Collection<String> minimal = BukkitTools.toMinimalEntityCollection(inputAsEntityType);
		
		assertEquals(input.size(), minimal.size());
		assertTrue(input.containsAll(minimal));
	}
}
