package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.status.CustomStatusField;
import com.palmergames.bukkit.towny.object.status.CustomStatusFieldType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TownyObject {
	private String name;

	private Map<String, CustomStatusField> extraStatusFields = new HashMap<>(); // Emperor-Koala
	
	protected TownyObject(String name) {
		this.name = name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

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

	public Map<String, CustomStatusField> getExtraStatusFields() {
		return extraStatusFields;
	}
	
	public void addExtraStatusField(String plugin, String key, CustomStatusField field) throws AlreadyRegisteredException {
		String namespaced = plugin + "_" + key;
		if (extraStatusFields.containsKey(namespaced))
			throw new AlreadyRegisteredException("Field from plugin " + plugin + " already registered with key: " + key);
		
		extraStatusFields.put(key, field);
	}
	
	public void removeExtraStatusField(String plugin, String key) {
		String namespaced = plugin + "_" + key;
		extraStatusFields.remove(namespaced);
	}

	public void setExtraStatusFields(String str) {
		String[] objects = str.split(";");
		for (String obj : objects) {
			if (obj == null || obj.isEmpty()) continue;
			String[] kvp = obj.split(":");
			String[] pk = kvp[0].split("_");
			String[] val = kvp[1].split(",");
			CustomStatusField csf = null;
			CustomStatusFieldType type = CustomStatusFieldType.values()[Integer.parseInt(val[0])];
			switch (type) {
				case IntegerField:
					csf = new CustomStatusField<>(val[1], type, Integer.parseInt(val[2]));
					break;
				case StringField:
					csf = new CustomStatusField<>(val[1], type, val[2]);
					break;
				case BalanceField:
				case DoubleField:
					csf = new CustomStatusField<>(val[1], type, Double.parseDouble(val[2]));
					break;
				case BooleanField:
					csf = new CustomStatusField<>(val[1], type, Boolean.parseBoolean(val[2]));
					break;
			}
			
			try {
				addExtraStatusField(pk[0], pk[1], csf);
			} catch (AlreadyRegisteredException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	public boolean hasExtraStatusFields() {
		return extraStatusFields.size() > 0;
	}
}
