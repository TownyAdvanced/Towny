package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public abstract class TownyObject implements Nameable {
	private String name;

	private HashSet<CustomDataField> metadata = null;
	
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

	public String getFormattedName() {

		return TownyFormatter.getFormattedName(this);
	}

	public void addMetaData(CustomDataField md) {
		if (getMetadata() == null)
			metadata = new HashSet<>();

		getMetadata().add(md);
	}

	public void removeMetaData(CustomDataField md) {
		if (!hasMeta())
			return;

		getMetadata().remove(md);

		if (getMetadata().size() == 0)
			this.metadata = null;
	}

	public HashSet<CustomDataField> getMetadata() {
		return metadata;
	}

	public boolean hasMeta() {
		return getMetadata() != null;
	}

	public void setMetadata(String str) {

		if (metadata == null)
			metadata = new HashSet<>();

		String[] objects = str.split(";");
		for (String object : objects) {
			metadata.add(CustomDataField.load(object));
		}
	}
	
}
