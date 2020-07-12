package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.util.StringMgmt;
import org.apache.commons.lang.StringUtils;

public class WorldCoordHandler implements SerializationHandler<WorldCoord> {

	@Override
	public WorldCoord loadString(LoadContext context, String str) {
		String elements = StringUtils.substringBetween(str, "{", "}");
		String[] tokens = elements.split(";");
		
		String worldName = tokens[0];
		int x = Integer.parseInt(tokens[1]);
		int z = Integer.parseInt(tokens[2]);
		
		return new WorldCoord(worldName, x, z);
	}

	@Override
	public String toStoredString(SaveContext context, WorldCoord obj) {
		return StringMgmt.objectNotationStr(
			obj.getWorldName(),
			obj.getX() + "",
			obj.getZ() + ""
		);
	}
}
