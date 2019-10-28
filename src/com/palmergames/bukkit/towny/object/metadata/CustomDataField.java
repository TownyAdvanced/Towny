package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.towny.object.TownBlock;

public abstract class CustomDataField<T> {
	private CustomDataFieldType type;
	private T value;
	private String key;
	private TownBlock parentBlock;
	
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
}
