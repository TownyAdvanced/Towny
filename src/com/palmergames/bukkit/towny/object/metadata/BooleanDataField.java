package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.util.Colors;

public class BooleanDataField extends CustomDataField<Boolean> {
    
    public BooleanDataField(String key, Boolean value) {
        super(key, value);
    }
    
    public BooleanDataField(String key, Boolean value, String label) {
    	super(key, value, label);
	}
    
    public BooleanDataField(String key) {
        // Initialized to false
        super(key, false);
    }

	@Override
	public String getTypeID() {
		return typeID();
	}
	
	public static String typeID() {
    	return "towny_booldf";
	}

	@Override
	public void setValueFromString(String strValue) {
		setValue(Boolean.parseBoolean(strValue));
	}

	@Override
	public String displayFormattedValue() {
		boolean val = getValue();
		return (val ? Colors.LightGreen : Colors.Red) + val;
	}

	@Override
	public CustomDataField<Boolean> clone() {
		return new BooleanDataField(getKey(), getValue(), this.label);
	}
}
