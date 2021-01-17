package com.palmergames.bukkit.towny.object;

import com.palmergames.annotations.Unmodifiable;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TownyObject implements Nameable, Savable {
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

	public void addMetaData(@NotNull CustomDataField<?> md) {
		Validate.notNull(md);
		if (metadata == null)
			metadata = new HashMap<>();
		
		metadata.put(md.getKey(), md);
	}

	public void removeMetaData(@NotNull CustomDataField<?> md) {
		Validate.notNull(md);
		if (!hasMeta())
			return;
		
		metadata.remove(md.getKey());
		
		if (metadata.isEmpty())
			this.metadata = null;
	}
	
	@Unmodifiable
	public Collection<CustomDataField<?>> getMetadata() {
		if (metadata == null || metadata.isEmpty())
			return Collections.emptyList();
		
		return Collections.unmodifiableCollection(metadata.values());
	}
	
	@Nullable
	public CustomDataField<?> getMetadata(@NotNull String key) {
		Validate.notNull(key);
		
		if(metadata != null)
			return metadata.get(key);
		
		return null;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T extends CustomDataField<?>> T getMetadata(@NotNull String key, @NotNull Class<T> cdfClass) {
		Validate.notNull(cdfClass);
		Validate.notNull(key);
		
		if(metadata != null) {
			CustomDataField<?> cdf = metadata.get(key);
			if (cdfClass.isInstance(cdf)) {
				return (T) cdf;
			}
		}

		return null;
	}

	public boolean hasMeta() {
		return metadata != null;
	}

	public boolean hasMeta(@NotNull String key) {
		Validate.notNull(key);
		if (metadata != null)
			return metadata.containsKey(key);
		
		return false;
	}
	
	public <T extends CustomDataField<?>> boolean hasMeta(@NotNull String key, @NotNull Class<T> cdfClass) {
		Validate.notNull(cdfClass);
		Validate.notNull(key);

		if(metadata != null) {
			CustomDataField<?> cdf = metadata.get(key);
			return cdfClass.isInstance(cdf);
		}

		return false;
	}
	
}
