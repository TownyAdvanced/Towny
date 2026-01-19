package com.palmergames.bukkit.towny.object;

import org.mockbukkit.mockbukkit.MockBukkit;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.EntityLists;
import com.palmergames.bukkit.util.ItemLists;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
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
	void testCompactableCollectionRemoval() {
		AbstractRegistryList.CompactableCollection<EntityType> collection = new AbstractRegistryList.CompactableCollection<>(EntityType.class);
		collection.setNames(List.of("wolf", "monsters"));

		assertTrue(collection.contains(EntityType.WOLF));
		assertTrue(collection.contains(EntityType.ZOMBIE));
		
		collection.remove(EntityType.ZOMBIE);
		assertFalse(collection.contains(EntityType.ZOMBIE));
		
		// Zombie is removed now, so the group gets unwrapped and the size of the backing names must increase
		assertTrue(collection.getNames().size() > 10);
		
		// Test compaction
		assertTrue(collection.add(EntityType.ZOMBIE));
		assertTrue(collection.compact());
		
		assertEquals(2, collection.getNames().size());
		
		assertFalse(collection.compact()); // Nothing to compact
	}
	
	@Test
	void testCompactableCollectionCompact() {
		AbstractRegistryList.CompactableCollection<EntityType> collection = AbstractRegistryList.CompactableCollection.entityTypes();
		collection.setNames(List.of("monsters"));
		
		collection.remove(EntityType.ZOMBIE);
		
		assertTrue(collection.getNames().size() > 10);
		
		collection.add(EntityType.ZOMBIE);
		
		assertTrue(collection.getNames().size() > 10);
		assertTrue(collection.compact());
		
		assertEquals(1, collection.getNames().size());
		assertFalse(collection.compact());
	}
	
	@Test
	void testCompactableDuplicateRemoval() {
		AbstractRegistryList.CompactableCollection<EntityType> collection = AbstractRegistryList.CompactableCollection.entityTypes();
		collection.setNames(List.of("monsters", "zombie"));
		
		assertTrue(collection.remove(EntityType.ZOMBIE));
		assertTrue(collection.getNames().size() > 10);
	}
}
