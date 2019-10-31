package com.palmergames.bukkit.towny.object.metadata;

import org.bukkit.configuration.serialization.SerializableAs;

import java.util.Map;

public class StringDataField extends CustomDataField<String> {

	public StringDataField(String key) {
		super(key, CustomDataFieldType.StringField);
	}
	
	public StringDataField(String key, String value) {
		super(key, CustomDataFieldType.StringField, value);
	}

	public static StringDataField deserialize(Map<String, Object> args) {
		String key = (String)args.get("key");
		String value = (String) args.get("value");

		return new StringDataField(key, value);
	}
}
