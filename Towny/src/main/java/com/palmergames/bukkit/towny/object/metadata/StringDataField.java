package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.util.Colors;

public class StringDataField extends CustomDataField<String> {

    public StringDataField(String key) {
        super(key);
    }

	public StringDataField(String key, String value, String label) {
		super(key, value, label);
	}
    
    public StringDataField(String key, String value) {
        super(key, value, null);
    }

	@Override
	public String getTypeID() {
		return typeID();
	}
	
	public static String typeID() {
    	return "towny_stringdf";
	}

	@Override
	public void setValueFromString(String strValue) {
		setValue(strValue);
	}

	@Override
	public String displayFormattedValue() {
		return Colors.White + getValue();
	}

	@Override
	public CustomDataField<String> clone() {
		return new StringDataField(getKey(), getValue(), this.label);
	}
}
