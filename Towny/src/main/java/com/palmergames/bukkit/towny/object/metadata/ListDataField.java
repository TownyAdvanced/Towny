package com.palmergames.bukkit.towny.object.metadata;

import com.google.gson.JsonArray;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListDataField extends CustomDataField<List<CustomDataField<?>>> {
	public ListDataField(String key, List<CustomDataField<?>> value, String label) {
		super(key, value, label);
	}

	public ListDataField(String key, List<CustomDataField<?>> value) {
		super(key, value);
	}
	
	public ListDataField(String key) {
		super(key, new ArrayList<>());
	}

	@Override
	public @NotNull String getTypeID() {
		return typeID();
	}

	public static String typeID() {
		return "towny_listdf";
	}

	@Override
	public void setValueFromString(String strValue) {
		getValue().clear();
		
		try {
			getValue().addAll(DataFieldIO.deserializeMeta(strValue));
		} catch (IOException ignored) {}
	}
	
	@Override
	public String serializeValueToString() {
		if (this.value.isEmpty())
			return new JsonArray().toString();
		
		return DataFieldIO.serializeCDFs(this.value);
	}

	@Override
	protected String displayFormattedValue() {
		return getValue().toString();
	}

	@Override
	public @NotNull CustomDataField<List<CustomDataField<?>>> clone() {
		return new ListDataField(key, getValue().stream().map(CustomDataField::clone).collect(Collectors.toList()), label);
	}
}
