package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.TownyObject;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MetadataLoader {
	
	private static class ObjectMetadata {
		private final TownyObject object;
		private final String serializedMetadata;
		
		private ObjectMetadata(TownyObject obj, String metadata) {
			this.object = obj;
			this.serializedMetadata = metadata;
		}
	}
	
	private static final MetadataLoader instance = new MetadataLoader();
	
	public static MetadataLoader getInstance() {
		return instance;
	}
	
	
	Map<String, DataFieldDeserializer<?>> deserializerMap = new HashMap<>();
	ArrayList<ObjectMetadata> storedMetadata = new ArrayList<>();
	
	
	private MetadataLoader() {
		
		// Register Towny Datafields
		deserializerMap.put(IntegerDataField.typeID(), TownyCDFDeserializer.INTEGER_DF);
		deserializerMap.put(StringDataField.typeID(), TownyCDFDeserializer.STRING_DF);
		deserializerMap.put(BooleanDataField.typeID(), TownyCDFDeserializer.BOOLEAN_DF);
		deserializerMap.put(DecimalDataField.typeID(), TownyCDFDeserializer.DECIMAL_DF);
		deserializerMap.put(LongDataField.typeID(), TownyCDFDeserializer.LONG_DF);
	}
	
	
	public boolean registerDeserializer(String typeID, DataFieldDeserializer<?> deserializer) {
		if (deserializerMap.containsKey(typeID))
			return false;
		
		deserializerMap.put(typeID, deserializer);
		return true;
	}

	public void deserializeMetadata(TownyObject object, String serializedMetadata) {
		storedMetadata.add(new ObjectMetadata(object, serializedMetadata));
	}
	
	public void scheduleDeserialization() {
		Bukkit.getScheduler().runTask(Towny.getPlugin(), this::runDeserialization);
	}
	
	private void runDeserialization() {
		if (storedMetadata.isEmpty())
			return;
		
		for (ObjectMetadata storedMeta : storedMetadata) {
			try {
				Collection<CustomDataField<?>> fields = DataFieldIO.deserializeMeta(
					storedMeta.serializedMetadata, deserializerMap
				);

				for (CustomDataField<?> cdf : fields) {
					storedMeta.object.addMetaData(cdf);
				}
			} catch (IOException ex) {
				// TODO Throw Error
			}
		}
		storedMetadata.clear();
		// Reduce memory alloc after load.
		storedMetadata.trimToSize();
	}
	
	
	
	
	
}
