package com.palmergames.bukkit.config.migration;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.palmergames.bukkit.util.Version;

import java.lang.reflect.Type;

class VersionDeserializer implements JsonDeserializer<Version> {
	@Override
	public Version deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		return new Version(jsonElement.getAsString());
	}
}
