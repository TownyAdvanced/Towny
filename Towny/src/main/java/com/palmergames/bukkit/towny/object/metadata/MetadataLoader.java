package com.palmergames.bukkit.towny.object.metadata;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.LoadedMetadataEvent;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.util.BukkitTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class MetadataLoader {
	
	private static final MetadataLoader INSTANCE = new MetadataLoader();
	
	public static MetadataLoader getInstance() {
		return INSTANCE;
	}
	
	private final Map<String, DataFieldDeserializer<?>> deserializerMap = new HashMap<>();
	private final ArrayList<TownyObject> storedMetadata = new ArrayList<>();
	
	private MetadataLoader() {
		
		// Register Towny Datafields
		deserializerMap.put(IntegerDataField.typeID(), TownyCDFDeserializer.INTEGER_DF);
		deserializerMap.put(StringDataField.typeID(), TownyCDFDeserializer.STRING_DF);
		deserializerMap.put(BooleanDataField.typeID(), TownyCDFDeserializer.BOOLEAN_DF);
		deserializerMap.put(DecimalDataField.typeID(), TownyCDFDeserializer.DECIMAL_DF);
		deserializerMap.put(LongDataField.typeID(), TownyCDFDeserializer.LONG_DF);
		deserializerMap.put(ByteDataField.typeID(), TownyCDFDeserializer.BYTE_DF);
		deserializerMap.put(LocationDataField.typeID(), TownyCDFDeserializer.LOCATION_DF);
		deserializerMap.put(ListDataField.typeID(), TownyCDFDeserializer.LIST_DF);
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
			Towny.getPlugin().getLogger().log(Level.WARNING, "Error loading metadata for towny object " + object.getClass().getName() + object.getName() + "!", e);
		}
		
		if (!fields.isEmpty()) {
			boolean hasCustomTypes = false;
			for (CustomDataField<?> cdf : fields) {
				// If the rawdatafield is a Towny meta type, convert it immediately.
				if (cdf instanceof RawDataField) {
					cdf = convertRawMetadata((RawDataField) cdf);
				}
				
				// CDF is null due to a bad conversion
				if (cdf == null)
					continue;
				
				// Did not convert, so it has to be a custom meta type
				if (cdf instanceof RawDataField || (cdf instanceof ListDataField listField && listField.getValue().stream().anyMatch(field -> field instanceof RawDataField)))
					hasCustomTypes = true;
					
				object.addMetaData(cdf, false);
			}
			// If metadata has a custom type, store it to be converted at the first tick
			if (hasCustomTypes)
				storedMetadata.add(object);
		}
	}
	
	public void scheduleDeserialization() {
		Towny.getPlugin().getScheduler().run(this::runDeserialization);
	}
	
	private void runDeserialization() {
		if (storedMetadata.isEmpty())
			return;

		for (TownyObject tObj : storedMetadata) {
			final List<CustomDataField<?>> deserializedFields = deserializeStoredMeta(tObj.getMetadata());

			for (CustomDataField<?> cdf : deserializedFields) {
				// Will override the metadata
				tObj.addMetaData(cdf, false);
			}
		}
		
		storedMetadata.clear();
		// Reduce memory alloc after load.
		storedMetadata.trimToSize();
		
		// Call event
		BukkitTools.fireEvent(new LoadedMetadataEvent());
	}
	
	private List<CustomDataField<?>> deserializeStoredMeta(Collection<CustomDataField<?>> meta) {
		final List<CustomDataField<?>> deserializedFields = new ArrayList<>();
		
		// Convert all RawDataFields to actual CustomDataField classes.
		for (CustomDataField<?> cdf : meta) {
			if (cdf instanceof ListDataField listField) {
				//noinspection ErrorNotRethrown
				try {
					listField.setValue(deserializeStoredMeta(listField.getValue()));
				} catch (StackOverflowError ignored) {}
			}

			if (!(cdf instanceof RawDataField raw))
				continue;

			CustomDataField<?> convertedCDF = convertRawMetadata(raw);

			if (convertedCDF == null ||
				convertedCDF instanceof RawDataField)
				continue;

			deserializedFields.add(convertedCDF);
		}
		
		return deserializedFields;
	}
	
	private CustomDataField<?> convertRawMetadata(RawDataField rdf) {
		final String typeID = rdf.getTypeID();
		DataFieldDeserializer<?> deserializer = deserializerMap.get(typeID);

		// If there is no deserializer, just return the raw data field.
		if (deserializer == null)
			return rdf;

		CustomDataField<?> deserializedCDF = deserializer.deserialize(rdf.getKey(), rdf.getValue());

		// The CDF did not deserialize properly, return null to indicate bad conversion.
		if (deserializedCDF == null)
			return null;

		if (rdf.hasLabel())
			deserializedCDF.setLabel(rdf.getLabel());
		
		return deserializedCDF;
	}
	
	
	
	
	
}
