package com.palmergames.bukkit.towny.object.status;

public class CustomStatusField<T> {
	
	private String label;
	private CustomStatusFieldType type;
	private T value;
	
	public CustomStatusField(String label, CustomStatusFieldType type, T value) {
		this.label = label;
		this.type = type;
		this.value = value;
	}
	
	public CustomStatusField(String label, CustomStatusFieldType type) {
		this(label, type, null);
	}
	
	public CustomStatusField(CustomStatusFieldType type) {
		this(null, type, null);
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public CustomStatusFieldType getType() {
		return type;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	@Override
	public String toString() {
		String out = "";

		// Type
		out += type.getValue().toString();

		// Label
		out += "," + getLabel();

		// Value
		out += "," + getValue();

		return out;
	}
}
