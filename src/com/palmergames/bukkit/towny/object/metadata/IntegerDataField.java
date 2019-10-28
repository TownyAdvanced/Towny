package com.palmergames.bukkit.towny.object.metadata;

public class IntegerDataField extends CustomDataField<Integer> {
	
	// Initializes default value to zero.
	public IntegerDataField()
	{
		super(CustomDataFieldType.IntegerField, 0);
	}
	
	// Allow for initialization with default value provided.
	public IntegerDataField(Integer value) {
		super(CustomDataFieldType.IntegerField, value);
	}
}
