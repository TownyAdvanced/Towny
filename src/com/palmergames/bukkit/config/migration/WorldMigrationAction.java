package com.palmergames.bukkit.config.migration;

import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.util.StringMgmt;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public enum WorldMigrationAction {
	UPDATE_WORLD_BLOCK_IGNORE(((townyWorld, s) -> {
		townyWorld.setPlotManagementIgnoreIds(splitMats(StringMgmt.join(townyWorld.getPlotManagementIgnoreIds(), ",") + s));
	})),
	UPDATE_WORLD_DELETE_MAYOR(((townyWorld, s) -> {
		townyWorld.setPlotManagementMayorDelete(splitMats(StringMgmt.join(townyWorld.getPlotManagementMayorDelete(), ",") + s));
	})),
	UPDATE_WORLD_UNCLAIM_DELETE(((townyWorld, s) -> {
		townyWorld.setPlotManagementDeleteIds(splitMats(StringMgmt.join(townyWorld.getPlotManagementDeleteIds(), ",") + s));
	})),
	UPDATE_WORLD_EXPLOSION_REVERT_ENTITIES(((townyWorld, s) -> {
		townyWorld.setPlotManagementWildRevertEntities(splitMats(StringMgmt.join(townyWorld.getPlotManagementWildRevertEntities(), ",") + s));
	}));
	
	BiConsumer<TownyWorld, String> action;
	
	WorldMigrationAction(BiConsumer<TownyWorld, String> action) {
		this.action = action;
	}
	
	public BiConsumer<TownyWorld, String> getAction() {
		return action;
	}
	
	private static List<String> splitMats(String string) {
		return Arrays.asList(string.split(","));
	}
}
