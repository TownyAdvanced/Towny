package com.palmergames.bukkit.towny.object;

import com.google.common.base.Preconditions;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TownyObject extends Loadable implements Nameable, Savable {
	private String name;
	
	private Map<String, CustomDataField<?>> metadata = null;
	
	protected TownyObject(String name) {
		this.name = name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}

	public List<String> getTreeString(int depth) {

		return new ArrayList<>();
	}

	public String getTreeDepth(int depth) {

		char[] fill = new char[depth * 4];
		Arrays.fill(fill, ' ');
		if (depth > 0) {
			fill[0] = '|';
			int offset = (depth - 1) * 4;
			fill[offset] = '+';
			fill[offset + 1] = '-';
			fill[offset + 2] = '-';
		}
		return new String(fill);
	}

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Add a specific metadata to this TownyObject.
	 * Overrides existing metadata of the same key.
	 * Most implementations will save the object after this method is called.
	 * 
	 * @param md CustomDataField to add.
	 */
	public void addMetaData(@NotNull CustomDataField<?> md) {
		this.addMetaData(md, false);
	}

	/**
	 * Add a specific metadata to this TownyObject.
	 * Overrides existing metadata of the same key.
	 * 
	 * @param md CustomDataField to add.
	 * @param save whether to save this object after the metadata is added.
	 */
	// Exists to maintain backwards compatibility
	// DO NOT OVERRIDE THIS METHOD ANYWHERE
	public void addMetaData(@NotNull CustomDataField<?> md, boolean save) {
		Preconditions.checkNotNull(md);
		if (metadata == null)
			metadata = new HashMap<>();

		metadata.put(md.getKey(), md);
		
		if (save) 
			this.save();
	}

	/**
	 * Remove a specific metadata from the TownyObject.
	 * The metadata does not need to be the same instance of the one added,
	 * but must have the same key.
	 * Most implementations will save the TownyObject after removing the metadata.
	 * 
	 * @param md CustomDataField to remove.
	 */
	public void removeMetaData(@NotNull CustomDataField<?> md) {
		this.removeMetaData(md, false);
	}

	/**
	 * Remove a specific metadata from the TownyObject.
	 * The metadata does not need to be the same instance of the one added,
	 * but must have the same key.
	 *
	 * @param md CustomDataField to remove.
	 * @param save whether to save the object or not after the metadata is removed.
	 *             
	 * @return whether the metadata was successfully removed. 
	 */
	// Exists to maintain backwards compatibility
	// DO NOT OVERRIDE THIS METHOD ANYWHERE
	public boolean removeMetaData(@NotNull CustomDataField<?> md, boolean save) {
		Preconditions.checkNotNull(md);
		return removeMetaData(md.getKey(), save);
	}

	/**
	 * Remove a specific metadata from the TownyObject.
	 * Most implementations will save the TownyObject after removing the metadata.
	 *
	 * @param key Key of the data field to remove.
	 * @return whether the metadata was successfully removed.    
	 */
	public boolean removeMetaData(@NotNull String key) {
		return removeMetaData(key, false);
	}

	/**
	 * Remove a specific metadata from the TownyObject.
	 *
	 * @param key Key of the data field to remove.
	 * @param save whether to save the object or not after the metadata is removed.
	 *
	 * @return whether the metadata was successfully removed. 
	 */
	public boolean removeMetaData(@NotNull String key, boolean save) {
		Preconditions.checkNotNull(key);
		
		if (!hasMeta())
			return false;

		final boolean removed = metadata.remove(key) != null;

		if (metadata.isEmpty())
			this.metadata = null;
		
		// Only save if the element was actually removed
		if (save && removed)
			this.save();
		
		return removed;
	}

	/**
	 * A collection of all metadata on the TownyObject.
	 * This collection cannot be modified.
	 * 
	 * Collection reflects current metadata, and is not thread safe.
	 * 
	 * @return an unmodifiable collection of all metadata on the object. 
	 */
	@Unmodifiable
	public Collection<CustomDataField<?>> getMetadata() {
		if (metadata == null || metadata.isEmpty())
			return Collections.emptyList();
		
		return Collections.unmodifiableCollection(metadata.values());
	}

	/**
	 * Fetch the metadata associated with the specific key.
	 * 
	 * @param key Key of the metadata to fetch.
	 *               
	 * @return the metadata associated with the key or {@code null} if none associated.
	 */
	@Nullable
	public CustomDataField<?> getMetadata(@NotNull String key) {
		Preconditions.checkNotNull(key);
		
		if(metadata != null)
			return metadata.get(key);
		
		return null;
	}

	/**
	 * Fetch the metadata associated with the specific key and class.
	 * 
	 * @param <T> The Class.
	 * @param key Key of the metadata to fetch.
	 * @param cdfClass Class of the CustomDataField to fetch.
	 *
	 * @return the specific metadata associated with the key and class or {@code null} if none exist.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public <T extends CustomDataField<?>> T getMetadata(@NotNull String key, @NotNull Class<T> cdfClass) {
		Preconditions.checkNotNull(cdfClass);
		Preconditions.checkNotNull(key);
		
		if(metadata != null) {
			CustomDataField<?> cdf = metadata.get(key);
			if (cdfClass.isInstance(cdf)) {
				return (T) cdf;
			}
		}

		return null;
	}

	/**
	 * 
	 * @return whether this object has metadata or not.
	 */
	public boolean hasMeta() {
		return metadata != null;
	}

	/**
	 * Check whether metadata associated with the key exists.
	 * 
	 * @param key Key of the metadata to check.
	 * @return whether metadata associated with the key exists.
	 */
	public boolean hasMeta(@NotNull String key) {
		Preconditions.checkNotNull(key);
		if (metadata != null)
			return metadata.containsKey(key);
		
		return false;
	}

	/**
	 * Check whether metadata associated with the given key and class exists.
	 * 
	 * @param <T> The Class.
	 * @param key Key of the metadata to check
	 * @param cdfClass Class extending CustomDataField to check.
	 * 
	 * @return whether metadata associated with the key and class exists.
	 */
	public <T extends CustomDataField<?>> boolean hasMeta(@NotNull String key, @NotNull Class<T> cdfClass) {
		Preconditions.checkNotNull(cdfClass);
		Preconditions.checkNotNull(key);

		if(metadata != null) {
			CustomDataField<?> cdf = metadata.get(key);
			return cdfClass.isInstance(cdf);
		}

		return false;
	}

	/**
	 * An internal method used to determine if an object exists in the TownyUniverse's maps.
	 *  
	 * @return true if this TownyObject exists.
	 */
	@ApiStatus.Internal
	public abstract boolean exists();

}
