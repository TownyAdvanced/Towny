package com.palmergames.bukkit.towny.object.metadata;

public abstract class CustomDataField<T> {
	private CustomDataFieldType type;
	private T value;
	
	public CustomDataField(CustomDataFieldType type, T value)
	{
		this.type = type;
		this.setValue(value);
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
}
