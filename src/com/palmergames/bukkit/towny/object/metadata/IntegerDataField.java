package com.palmergames.bukkit.towny.object.metadata;

public class IntegerDataField extends CustomDataField<Integer> {
    
    // Initializes default value to zero.
    public IntegerDataField(String key) {
        super(key, CustomDataFieldType.IntegerField);
    }

	public IntegerDataField(String key, Integer value, String label) {
		super(key, CustomDataFieldType.IntegerField, value, label);
	}
    
    // Allow for initialization with default value provided.
    public IntegerDataField(String key, Integer value) {
        super(key, CustomDataFieldType.IntegerField, value);
    }
}
