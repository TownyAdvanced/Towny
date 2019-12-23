package com.palmergames.bukkit.towny.database.io.json.deserializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.palmergames.bukkit.towny.object.Town;

import java.lang.reflect.Type;

public class TownDeserializer implements JsonDeserializer<Town> {
	@Override
	public Town deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		return null;
	}
}
