package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.Map;

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
		
		switch (type) {
			case IntegerField:
				out += "0";
				break;
			case StringField:
				out += "1";
				break;
		}
		
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
				break;
		}
		
		return field;
	}
}
