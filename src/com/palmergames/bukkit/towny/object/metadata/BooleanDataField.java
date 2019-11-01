package com.palmergames.bukkit.towny.object.metadata;

public class BooleanDataField extends CustomDataField<Boolean> {
	
	public BooleanDataField(String key, Boolean value) {
		super(key, CustomDataFieldType.BooleanField, value);
	}
	
	public BooleanDataField(String key) {
		// Initialized to false
		super(key, CustomDataFieldType.BooleanField, false);
	}
}
