package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.SerializationHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class ListHandler implements SerializationHandler<List<?>> {
	
	@Override
	public String toStoredString(List<?> obj) {
		StringJoiner joiner = new StringJoiner(",");
		for (Object o : obj) {
			if (o != null) {
				joiner.add(serialize(o));
			}
		}
		
		return "[" + joiner.toString() + "]";
	}

	@Override
	public List<?> loadString(String str) {
		String strCopy = str.replace("[", "").replace("]", "");
		String[] objStrings = strCopy.split(",");
		// All raw handlers must return collection<string>
		List<String> stringList = new ArrayList<>(objStrings.length);
		Collections.addAll(stringList, objStrings);
		return stringList;
	}
}
