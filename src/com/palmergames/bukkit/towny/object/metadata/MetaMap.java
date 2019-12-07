package com.palmergames.bukkit.towny.object.metadata;

import java.util.HashMap;

public class MetaMap extends HashMap<String, CustomDataField<Object>> {
	
	public String generateStorableString() {
		StringBuilder sb = new StringBuilder();
		
		for (String key : keySet()) {
			sb.append(key).append(";");
		}
		
		return sb.toString();
	}
}
