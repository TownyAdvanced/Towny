package com.palmergames.bukkit.towny.object;

import org.jetbrains.annotations.NotNull;

public final class TownBlockTypeHandler {

	public static TownBlockType getTypeInternal(@NotNull String input) {
		try {
			int id = Integer.parseInt(input);
			return TownBlockType.lookup(id);
		} catch (NumberFormatException e) {
			// We're dealing with someone who was on a version using the modern TownBlockTypes.
			TownBlockType type = TownBlockType.lookup(input);
			return type != null ? type : TownBlockType.RESIDENTIAL;
		}
	}
}
