package com.palmergames.bukkit.towny.object.metadata;

public abstract class CustomDataField<T> {
	private CustomDataFieldType type;
	private T value;
	private String key;
	
	public CustomDataField(String key, CustomDataFieldType type, T value)
	{
		this.type = type;
		this.setValue(value);
		this.key = key;
	}

	public CustomDataField(String key, CustomDataFieldType type)
	{
		this.type = type;
		this.value = null;
		this.key = key;
	}

	public CustomDataFieldType getType() {
		return type;
	}

	public T getValue() {
		
		return value;
	}

	public void setValue(T value) {
		// TODO: Save to yml
		
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	@Override
	public String toString() {
		String out = "";
		
		// Type
		out += type.getValue().toString();
		
		// Key
		out += "," + getKey();
		
		// Value
		out += "," + getValue();
		
		return out;
	}
	
	public static CustomDataField load(String str) {
		String[] tokens = str.split(",");
		CustomDataFieldType type = CustomDataFieldType.values()[Integer.parseInt(tokens[0])];
		String key = tokens[1];
		CustomDataField field = null;
		
		switch (type) {
			case IntegerField:
				Integer intValue = Integer.parseInt(tokens[2]);
				field = new IntegerDataField(key, intValue);
				break;
			case StringField:
				field = new StringDataField(key, tokens[2]);
			case BooleanField:
				field = new BooleanDataField(key, Boolean.parseBoolean(tokens[2]));
				break;
			case DecimalField:
				field = new DecimalDataField(key, Double.parseDouble(tokens[2]));
		}
		
		return field;
	}
	
	@Override
	public boolean equals(Object rhs) {
		if (rhs instanceof CustomDataField)
			return ((CustomDataField) rhs).getKey().equals(this.getKey());
		
		return false;
	}
	
	@Override
	public int hashCode() {
		// Use the key as a unique id
		return getKey().hashCode();
	}
}
