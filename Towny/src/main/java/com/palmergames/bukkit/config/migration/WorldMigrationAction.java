package com.palmergames.bukkit.config.migration;

import com.palmergames.bukkit.towny.object.TownyWorld;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public enum WorldMigrationAction {
	UPDATE_WORLD_BLOCK_IGNORE(((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setPlotManagementIgnoreIds(replaceAll(townyWorld.getPlotManagementIgnoreIds().stream()
					.map(WorldMigrationAction::matName)
					.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setPlotManagementIgnoreIds(splitMats(townyWorld.getPlotManagementIgnoreIds().stream().map(type -> type.getKey().toString()).collect(Collectors.joining(",")) + change.value));
	})),
	UPDATE_WORLD_DELETE_MAYOR(((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setPlotManagementMayorDelete(replaceAll(townyWorld.getPlotManagementMayorDelete().stream()
					.map(WorldMigrationAction::matName)
					.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setPlotManagementMayorDelete(splitMats(townyWorld.getPlotManagementMayorDelete().stream().map(type -> type.getKey().toString()).collect(Collectors.joining(",")) + change.value));
	})),
	UPDATE_WORLD_UNCLAIM_DELETE(((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setPlotManagementDeleteIds(replaceAll(townyWorld.getPlotManagementDeleteIds().stream()
					.map(WorldMigrationAction::matName)
					.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setPlotManagementDeleteIds(splitMats(townyWorld.getPlotManagementDeleteIds().stream().map(type -> type.getKey().toString()).collect(Collectors.joining(",")) + change.value));
	})),
	UPDATE_WILDERNESS_IGNORE_MATS(((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setUnclaimedZoneIgnore(replaceAll(townyWorld.getUnclaimedZoneIgnoreMaterials().stream()
					.map(WorldMigrationAction::matName)
					.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setUnclaimedZoneIgnore(splitMats(townyWorld.getUnclaimedZoneIgnoreMaterials().stream().map(type -> type.getKey().toString()).collect(Collectors.joining(",")) + change.value));
	})),
	UPDATE_WORLD_EXPLOSION_REVERT_BLOCKS(((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setPlotManagementWildRevertMaterials(replaceAll(townyWorld.getPlotManagementWildRevertBlocks().stream()
					.map(WorldMigrationAction::matName)
					.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setPlotManagementWildRevertMaterials(splitMats(townyWorld.getPlotManagementWildRevertBlocks().stream().map(type -> type.getKey().toString()).collect(Collectors.joining(",")) + change.value));
	})),
	UPDATE_WORLD_EXPLOSION_REVERT_ENTITIES(((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setPlotManagementWildRevertEntities(replaceAll(townyWorld.getPlotManagementWildRevertEntities().stream()
					.map(WorldMigrationAction::entityName)
					.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setPlotManagementWildRevertEntities(splitMats(townyWorld.getPlotManagementWildRevertEntities().stream().map(type -> type.getKey().toString()).collect(Collectors.joining(",")) + change.value));
	})),
	UPDATE_WORLD_UNCLAIM_REVERT_ENTITIES((((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setUnclaimDeleteEntityTypes(replaceAll(townyWorld.getUnclaimDeleteEntityTypes().stream()
				.map(WorldMigrationAction::entityName)
				.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setUnclaimDeleteEntityTypes(splitMats(townyWorld.getUnclaimDeleteEntityTypes().stream().map(type -> type.getKey().toString()).collect(Collectors.joining(",")) + change.value));
	})));
	
	final BiConsumer<TownyWorld, Change> action;
	
	WorldMigrationAction(BiConsumer<TownyWorld, Change> action) {
		this.action = action;
	}
	
	public BiConsumer<TownyWorld, Change> getAction() {
		return action;
	}
	
	private static List<String> splitMats(String string) {
		return Arrays.asList(string.split(","));
	}
	
	private static List<String> replaceAll(List<String> list, String oldValue, String newValue) {
		List<String> toReplace = new ArrayList<>(list);
		
		ListIterator<String> iterator = toReplace.listIterator();
		while (iterator.hasNext()) {
			if (iterator.next().equalsIgnoreCase(oldValue))
				iterator.set(newValue);
		}

		return toReplace;
	}
	
	private static String matName(Material material) {
		return material.getKey().getKey(); // Material.DIRT -> minecraft:dirt -> dirt
	}
	
	private static String entityName(EntityType entity) {
		return entity.getKey().getKey();
	}
}
