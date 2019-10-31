package com.palmergames.bukkit.towny.object.metadata;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.Map;

public enum CustomDataFieldType {
	IntegerField(0), StringField(1);
	
	public Integer value;
	
	CustomDataFieldType(Integer type) {
		this.value = type;
	}

	public Integer getValue() {
		return value;
	}
}
