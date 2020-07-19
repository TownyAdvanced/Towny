package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SaveHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class ListHandler implements LoadHandler<List<?>>, SaveHandler<List<?>> {
	
	@Override
	public String toStoredString(SaveContext context, List<?> obj) {
		StringJoiner joiner = new StringJoiner(",");
		for (Object o : obj) {
			if (o != null) {
				joiner.add(context.toStoredString(o, o.getClass()));
			}
		}
		
		return "[" + joiner.toString() + "]";
	}

	@Override
	public List<?> loadString(LoadContext context, String str) {
		String strCopy = str.replace("[", "").replace("]", "");
		String[] objStrings = strCopy.split(",");
		// All raw handlers must return collection<string>
		List<String> stringList = new ArrayList<>(objStrings.length);
		Collections.addAll(stringList, objStrings);
		return stringList;
	}
}
