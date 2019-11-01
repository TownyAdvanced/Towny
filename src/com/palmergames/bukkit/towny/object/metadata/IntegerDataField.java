package com.palmergames.bukkit.towny.object.metadata;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class IntegerDataField extends CustomDataField<Integer> {
	
	// Initializes default value to zero.
	public IntegerDataField(String key) {
		super(key, CustomDataFieldType.IntegerField);
	}
	
	// Allow for initialization with default value provided.
	public IntegerDataField(String key, Integer value) {
		super(key, CustomDataFieldType.IntegerField, value);
	}
}
