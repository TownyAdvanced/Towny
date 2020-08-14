package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.Saveable;
import com.palmergames.bukkit.towny.database.handler.annotations.PrimaryKey;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class TownyObject implements Nameable, Saveable {
	private String name;
	private boolean changed;
	
	@PrimaryKey
	private UUID uniqueIdentifier;
	
	private transient HashSet<CustomDataField<?>> metadata = null;
	
	public TownyObject(UUID id) {
		this.uniqueIdentifier = id;
	}
	
	public TownyObject(UUID id, String name) {
		this(id);
		this.name = name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	@NotNull
	public String getName() {
		if (name == null) {
			return "";
		}
		return name;
	}
	
	// Certain towny objects will override this method
	public void rename(String newName) {
		setName(newName);
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

	public void addMetaData(CustomDataField<?> md) {
		if (metadata == null)
			metadata = new HashMap<>();
		
		metadata.put(md.getKey(), md);
	}

	public void removeMetaData(CustomDataField<?> md) {
		if (!hasMeta())
			return;
		
		metadata.remove(md.getKey());
		
		if (metadata.isEmpty())
			this.metadata = null;
	}
	
	public Collection<CustomDataField<?>> getMetadata() {
		if (metadata == null || metadata.isEmpty())
			return Collections.emptyList();
		
		return Collections.unmodifiableCollection(metadata.values());
	}
	
	public CustomDataField<?> getMetadata(String key) {
		if(metadata != null)
			return metadata.get(key);
		
		return null;
	}

	public boolean hasMeta() {
		return metadata != null;
	}

	public boolean hasMeta(String key) {
		if (metadata != null)
			return metadata.containsKey(key);
		
		return false;
	}

	public void setMetadata(String str) {
		String[] objects = str.split(";");

		if (metadata == null)
			metadata = new HashMap<>(objects.length);
		
		for (String object : objects) {
			CustomDataField<?> cdf = CustomDataField.load(object);
			metadata.put(cdf.getKey(), cdf);
		}
	}

	/**
	 * Code-reduction method for TownyObjects to save
	 */
	public void save() {
		TownyUniverse.getInstance().getDatabaseHandler().save(this);
	}

	@Override
	public final @NotNull UUID getUniqueIdentifier() {
		return uniqueIdentifier;
	}
	
	public void setUniqueIdentifier(UUID uuid) {
		this.uniqueIdentifier = uuid;
	}

	public boolean hasUniqueIdentifier() {
		return uniqueIdentifier != null;
	}
	
	@Override
	public int hashCode() {
		return uniqueIdentifier.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TownyObject that = (TownyObject) o;
		return uniqueIdentifier.equals(that.uniqueIdentifier);
	}

	@Override
	public boolean isChanged() {
		return changed;
	}
	
	@Override
	public void setChanged(boolean changed) {
		this.changed = changed;
	}
}
