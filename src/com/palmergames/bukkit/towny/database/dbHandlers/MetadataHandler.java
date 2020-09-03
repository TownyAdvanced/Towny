package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;

import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataHandler implements SerializationHandler<Map<String, CustomDataField<?>>> {
	
	@Override
	public Map<String, CustomDataField<?>> loadString(String str) {
		String[] objects = str.split(";");
		
		Map<String, CustomDataField<?>> metadata = new ConcurrentHashMap<>(objects.length);

		for (String object : objects) {
			CustomDataField<?> cdf = CustomDataField.load(object);
			metadata.put(cdf.getKey(), cdf);
		}
		
		return metadata;
	}

	@Override
	public String toStoredString(Map<String, CustomDataField<?>> metadata) {
		if (metadata.isEmpty())
			return "";
		
		StringJoiner joiner = new StringJoiner("&");
		for (CustomDataField<?> value : metadata.values()) {
			joiner.add(value.toString());
		}
		
		return joiner.toString();
	}
}
