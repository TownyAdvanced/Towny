package com.palmergames.bukkit.towny.object.metadata;

/**
 * An interface which allows objects to support the Towny metadata system.
 * @author Suneet Tipirneni (Siris)
 */
public interface Metadatable {

	/**
	 * The Set of metadata for a given object.
	 * @return The HashSet of {@link CustomDataField} objects (meta) associated with
	 * the conforming class.
	 */
	MetaMap getMetadata();

	boolean hasMeta();
	/**
	 * Loads the metadata for the given string representation.
	 * @param str The serialized version of the meta.
	 */
	void setMetadata(String str);

	/**
	 * Adds meta to the already existing meta set on the object.
	 * @param md The field to be added.
	 */
	void addMetaData(CustomDataField<Object> md);

	/**
	 * Removes meta from the existing meta set.
	 * @param md The field to be removed.
	 */
	void removeMetaData(CustomDataField<Object> md);
	
}
