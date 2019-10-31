package com.palmergames.bukkit.towny.object.metadata;

import org.bukkit.configuration.serialization.SerializableAs;

import java.util.Map;

public class IntegerDataField extends CustomDataField<Integer> {
	
	// Initializes default value to zero.
	public IntegerDataField(String key)
	{
		super(key, CustomDataFieldType.IntegerField);
	}
	
	// Allow for initialization with default value provided.
	public IntegerDataField(String key, Integer value) {
		super(key, CustomDataFieldType.IntegerField, value);
	}
	
	public static IntegerDataField deserialize(Map<String, Object> args) {
		String key = (String)args.get("key");
		Integer value = (Integer) args.get("value");
		
		return new IntegerDataField(key, value);
		
	} 
}
