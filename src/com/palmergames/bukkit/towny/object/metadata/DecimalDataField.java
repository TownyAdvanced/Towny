package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.util.Colors;

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

	@Override
	protected String getTypeID() {
		return typeID();
	}
	
	public static String typeID() {
    	return "towny_decdf";
	}

	@Override
	public void setValueFromString(String strValue) {
    	setValue(Double.parseDouble(strValue));
	}

	@Override
	public boolean canParseFromString(String str) {
		try {
			Double.parseDouble(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	@Override
	public String displayFormattedValue() {
		double val = getValue();
		return (val <= 0 ? Colors.Red : Colors.LightGreen) + val;
	}

	@Override
	public CustomDataField<Double> clone() {
		return new DecimalDataField(getKey(), getValue(), getLabel());
	}
}
