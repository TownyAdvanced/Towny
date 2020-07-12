package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.object.Town;

import java.util.ArrayList;
import java.util.List;

public class TownListHandler implements SerializationHandler<List<Town>> {
	@Override
	public List<Town> loadString(LoadContext context, String str) {
		List<Town> towns = new ArrayList<>();
		String strCopy;
		strCopy = str.replace("[", "").replace("]", "");
		String[] townNames = strCopy.split(",");

		for (String townName : townNames) {
			Town loadedTown = context.fromStoredString(townName, Town.class);

			if (loadedTown == null) {
				continue;
			}

			towns.add(loadedTown);
		}

		return towns;
	}

	@Override
	public String toStoredString(SaveContext context, List<Town> obj) {
		StringBuilder output = new StringBuilder("[");
		for (Town t : obj) {
			output.append(t.getUniqueIdentifier()).append(",");
		}

		output.append(']');

		return output.toString();
	}
}
