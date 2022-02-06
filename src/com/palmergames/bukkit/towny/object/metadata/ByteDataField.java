package com.palmergames.bukkit.towny.object.metadata;

import org.jetbrains.annotations.NotNull;

public class ByteDataField extends CustomDataField<Byte> {
	
	public ByteDataField(String key) {
		super(key);
	}

	public ByteDataField(String key, byte value, String label) {
		super(key, value, label);
	}

	public ByteDataField(String key, byte value) {
		super(key, value);
	}

	@Override
	public @NotNull String getTypeID() {
		return typeID();
	}
	
	public static String typeID() {
		return "towny_bytedf";
	}

	@Override
	public void setValueFromString(String strValue) {
		setValue(Byte.parseByte(strValue));
	}

	@Override
	public boolean canParseFromString(String str) {
		try {
			Byte.parseByte(str);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	@Override
	public String displayFormattedValue() {
		return String.valueOf(getValue());
	}

	@Override
	public @NotNull CustomDataField<Byte> clone() {
		return new ByteDataField(getKey(), getValue(), this.label);
	}
}
