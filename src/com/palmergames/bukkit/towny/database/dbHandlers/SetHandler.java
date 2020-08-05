package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.SerializationHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

public class SetHandler implements SerializationHandler<Set<?>> {
	
	@Override
	public Set<?> loadString(String str) {
		String strCopy = str.replace("[", "").replace("]", "");
		String[] objStrings = strCopy.split(",");
		// All raw handlers must return Collection<string>
		Set<String> stringSet = new HashSet<>(objStrings.length);
		Collections.addAll(stringSet, objStrings);
		return stringSet; 
	}

	@Override
	public String toStoredString(Set<?> obj) {
		StringJoiner joiner = new StringJoiner(",");
		for (Object o : obj) {
			if (o != null) {
				joiner.add(serialize(o));
			}
		}

		return "[" + joiner.toString() + "]";
	}
}
