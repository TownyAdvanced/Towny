package com.palmergames.bukkit.config.migration;

import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.util.StringMgmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public enum WorldMigrationAction {
	UPDATE_WORLD_BLOCK_IGNORE(((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setPlotManagementIgnoreIds(replaceAll(townyWorld.getPlotManagementIgnoreIds().stream()
					.map(mat -> mat.name())
					.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setPlotManagementIgnoreIds(splitMats(StringMgmt.join(townyWorld.getPlotManagementIgnoreIds(), ",") + change.value));
	})),
	UPDATE_WORLD_DELETE_MAYOR(((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setPlotManagementMayorDelete(replaceAll(townyWorld.getPlotManagementMayorDelete().stream()
					.map(mat -> mat.name())
					.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setPlotManagementMayorDelete(splitMats(StringMgmt.join(townyWorld.getPlotManagementMayorDelete(), ",") + change.value));
	})),
	UPDATE_WORLD_UNCLAIM_DELETE(((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setPlotManagementDeleteIds(replaceAll(townyWorld.getPlotManagementDeleteIds().stream()
					.map(mat -> mat.name())
					.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setPlotManagementDeleteIds(splitMats(StringMgmt.join(townyWorld.getPlotManagementDeleteIds(), ",") + change.value));
	})),
	UPDATE_WORLD_EXPLOSION_REVERT_BLOCKS(((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setPlotManagementWildRevertMaterials(replaceAll(townyWorld.getPlotManagementWildRevertBlocks().stream()
					.map(mat -> mat.name())
					.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setPlotManagementWildRevertMaterials(splitMats(StringMgmt.join(townyWorld.getPlotManagementWildRevertBlocks(), ",") + change.value));
	})),
	UPDATE_WORLD_EXPLOSION_REVERT_ENTITIES(((townyWorld, change) -> {
		if (change.type == MigrationType.REPLACE)
			townyWorld.setPlotManagementWildRevertEntities(replaceAll(townyWorld.getPlotManagementWildRevertEntities().stream()
					.map(type -> type.name())
					.collect(Collectors.toList()), change.key, change.value));
		else
			townyWorld.setPlotManagementWildRevertEntities(splitMats(StringMgmt.join(townyWorld.getPlotManagementWildRevertEntities(), ",") + change.value));
	}));
	
	BiConsumer<TownyWorld, Change> action;
	
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
		Collections.replaceAll(toReplace, oldValue, newValue);
		return toReplace;
	}
}
