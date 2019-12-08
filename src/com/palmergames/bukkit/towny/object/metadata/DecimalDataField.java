package com.palmergames.bukkit.towny.object.metadata;

public class DecimalDataField extends CustomDataField<Double> {
    public DecimalDataField(String key, Double value) {
        super(key, CustomDataFieldType.DecimalField, value);
    }

	public DecimalDataField(String key, Double value, String label) {
		super(key, CustomDataFieldType.DecimalField, value, label);
	}

    public DecimalDataField(String key) {
        super(key, CustomDataFieldType.DecimalField, 0.0);
    }
}
