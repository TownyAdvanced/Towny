package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.object.metadata.CustomDataField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TownyObject implements Nameable {
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
	
}
