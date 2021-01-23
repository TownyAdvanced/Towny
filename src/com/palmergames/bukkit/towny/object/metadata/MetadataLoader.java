package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.TownyObject;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataLoader {
	
	private static final MetadataLoader instance = new MetadataLoader();
	
	public static MetadataLoader getInstance() {
		return instance;
	}
	
	Map<String, DataFieldDeserializer<?>> deserializerMap = new HashMap<>();
	ArrayList<TownyObject> storedMetadata = new ArrayList<>();
	
	
	private MetadataLoader() {
		
		// Register Towny Datafields
		deserializerMap.put(IntegerDataField.typeID(), TownyCDFDeserializer.INTEGER_DF);
		deserializerMap.put(StringDataField.typeID(), TownyCDFDeserializer.STRING_DF);
		deserializerMap.put(BooleanDataField.typeID(), TownyCDFDeserializer.BOOLEAN_DF);
		deserializerMap.put(DecimalDataField.typeID(), TownyCDFDeserializer.DECIMAL_DF);
		deserializerMap.put(LongDataField.typeID(), TownyCDFDeserializer.LONG_DF);
	}

	/**
	 * Register a deserializer for a specific custom CustomDataField class.
	 * @param typeID type id of the CustomDataField class to deserialize.
	 * @param deserializer actual deserializer for the class.
	 *                        
	 * @return whether or not the deserializer was registered.
	 */
	public boolean registerDeserializer(String typeID, DataFieldDeserializer<?> deserializer) {
		if (deserializerMap.containsKey(typeID))
			return false;
		
		deserializerMap.put(typeID, deserializer);
		return true;
	}

	public void deserializeMetadata(TownyObject object, String serializedMetadata) {
		initialDeserialization(object, serializedMetadata);
		storedMetadata.add(object);
	}
	
	private void initialDeserialization(TownyObject object, String serializedMetadata) {
		if (serializedMetadata == null || serializedMetadata.isEmpty())
			return;
		
		// Immediately deserialize all metadata.
		// Legacy Metadata will be parsed to actual metadata classes.
		// Modern metadata will be parsed to RawDataFields

		Collection<CustomDataField<?>> fields = Collections.emptyList();
		try {
			fields = DataFieldIO.deserializeMeta(serializedMetadata);
		} catch (IOException e) {
			// Unsure if logger is loaded at this point
			System.out.println("[Towny] Error loading metadata for towny object " + object.getClass().getName()
				+ object.getName() + "!");
			e.printStackTrace();
		}
		
		if (!fields.isEmpty()) {
			for (CustomDataField<?> cdf : fields) {
				object.addMetaData(cdf, false);
			}
		}
	}
	
	public void scheduleDeserialization() {
		Bukkit.getScheduler().runTask(Towny.getPlugin(), this::runDeserialization);
	}
	
	private void runDeserialization() {
		if (storedMetadata.isEmpty())
			return;

		List<CustomDataField<?>> deserializedFields = new ArrayList<>();
		for (TownyObject tObj : storedMetadata) {
			// Convert all RawDataFields to actual CustomDataField classes.
			for (CustomDataField<?> cdf : tObj.getMetadata()) {
				if (!(cdf instanceof RawDataField))
					continue;
				
				RawDataField rdf = (RawDataField) cdf;
				
				final String typeID = rdf.getTypeID();
				DataFieldDeserializer<?> deserializer = deserializerMap.get(typeID);
				
				if (deserializer == null)
					continue;
				
				CustomDataField<?> deserializedCDF = deserializer.deserialize(rdf.getKey(), rdf.getValue());
				
				if (deserializedCDF == null)
					continue;
				
				if (rdf.hasLabel())
					deserializedCDF.setLabel(rdf.getLabel());
				
				deserializedFields.add(deserializedCDF);
			}
			
			if (!deserializedFields.isEmpty()) {
				for (CustomDataField<?> cdf : deserializedFields) {
					// Will override the metadata
					tObj.addMetaData(cdf, false);
				}
				deserializedFields.clear();
			}
		}
		
		storedMetadata.clear();
		// Reduce memory alloc after load.
		storedMetadata.trimToSize();
	}
	
	
	
	
	
}
