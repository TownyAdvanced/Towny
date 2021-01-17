package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.util.Colors;

public class LongDataField extends CustomDataField<Long> {

	public LongDataField(String key, Long value) {
		super(key, CustomDataFieldType.LongField, value);
	}

	public LongDataField(String key, Long value, String label) {
		super(key, CustomDataFieldType.LongField, value, label);
	}
	
	public LongDataField(String key) {
		this(key, 0L);
	}

	@Override
	protected String getTypeID() {
		return typeID();
	}
	
	public static String typeID() {
		return "towny_longdf";
	}

	@Override
	public void setValueFromString(String strValue) {
		setValue(Long.parseLong(strValue));
	}

	@Override
	public boolean canParseFromString(String str) {
		try {
			Long.parseLong(str);
		}
		catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	@Override
	public String displayFormattedValue() {
		long lval = getValue();
		return (lval <= 0 ? Colors.Red : Colors.LightGreen) + lval;
	}

	@Override
	public CustomDataField<Long> clone() {
		return new LongDataField(getKey(), getValue(), this.label);
	}
}
