package com.palmergames.bukkit.towny.object.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DataFieldIO {
	
	public static String serializeCDFs(Collection<CustomDataField<?>> cdfs) {
		if (cdfs.isEmpty())
			return "";
		
		JsonArray array = new JsonArray();
		for (CustomDataField<?> cdf : cdfs) {
			JsonArray serializedArray = serializeCDF(cdf);
			array.add(serializedArray);
		}
		
		return array.toString();
	}
	
	private static JsonArray serializeCDF(CustomDataField<?> cdf) {
		JsonArray array = new JsonArray();
		array.add(cdf.getTypeID());
		array.add(cdf.getKey());
		array.add(cdf.serializeValueToString());
		if (cdf.hasLabel())
			array.add(cdf.getLabel());
		else
			array.add(JsonNull.INSTANCE);
		
		return array;
	}
	
	private static JsonArray convertToArray(String metadata) throws IOException {
		try {
			JsonElement element = new JsonParser().parse(metadata);

			if (!element.isJsonArray())
				throw new IOException("Metadata cannot be read as a JSON Array!");

			return element.getAsJsonArray();
		} catch (JsonSyntaxException jse) {
			// Just throw an IOException for everything
			throw new IOException(jse.getMessage(), jse.getCause());
		}
	}
	
	public static Collection<CustomDataField<?>> deserializeMeta(String metadata, Map<String, DataFieldDeserializer<?>> deserializerMap) throws IOException {
		JsonArray array = convertToArray(metadata);
		List<CustomDataField<?>> cdfList = new ArrayList<>(array.size());
		for (JsonElement element : array) {
			if (!element.isJsonArray())
				continue;
			
			JsonArray cdfArray = element.getAsJsonArray();
			if (cdfArray.size() < 2)
				continue;
			
			String typeID = cdfArray.get(0).getAsString();
			CustomDataField<?> cdf = deserializeCDF(cdfArray, deserializerMap.get(typeID));
			if (cdf != null)
				cdfList.add(cdf);
		}
		
		return cdfList;
	}
	
	private static <T extends CustomDataField<?>> T deserializeCDF(JsonArray array, DataFieldDeserializer<T> deserializer) {
		if (deserializer == null)
			return null;
		
		final String key = array.get(1).getAsString();
		final String value = array.get(2).isJsonNull() ? null : array.get(2).getAsString();
		final String label = array.get(3).isJsonNull() ? null : array.get(3).getAsString();
		
		T cdf = deserializer.deserialize(key, value);
		
		if (cdf != null && label != null)
			cdf.setLabel(label);
		
		return cdf;
	}
	
}
