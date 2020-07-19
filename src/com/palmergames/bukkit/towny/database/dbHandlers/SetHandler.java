package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

public class SetHandler implements SerializationHandler<Set<?>> {
	
	@Override
	public Set<?> loadString(LoadContext context, String str) {
		String strCopy = str.replace("[", "").replace("]", "");
		String[] objStrings = strCopy.split(",");
		// All raw handlers must return Collection<string>
		Set<String> stringSet = new HashSet<>(objStrings.length);
		Collections.addAll(stringSet, objStrings);
		return stringSet; 
	}

	@Override
	public String toStoredString(SaveContext context, Set<?> obj) {
		StringJoiner joiner = new StringJoiner(",");
		for (Object o : obj) {
			if (o != null) {
				joiner.add(context.toStoredString(o, o.getClass()));
			}
		}

		return "[" + joiner.toString() + "]";
	}
}
