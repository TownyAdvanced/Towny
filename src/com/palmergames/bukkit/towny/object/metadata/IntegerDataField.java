package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.util.Colors;

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

	@Override
	protected String getTypeID() {
		return typeID();
	}
	
	public static String typeID() {
		return "towny_intdf";
	}

	@Override
	public void setValueFromString(String strValue) {
		setValue(Integer.parseInt(strValue));
	}

	@Override
	public boolean canParseFromString(String str) {
		try {
			Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	@Override
	public String displayFormattedValue() {
		int val = getValue();
		return (val <= 0 ? Colors.Red : Colors.LightGreen) + val;
	}

	@Override
	public CustomDataField<Integer> clone() {
		return new IntegerDataField(getKey(), getValue(), this.label);
	}
}
