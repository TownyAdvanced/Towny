package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.database.Saveable;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class QueuedObject {
	private Saveable savableObj;
	private Map<String, String> insertionMap;
	
	public QueuedObject(@NotNull Saveable obj, @Nullable Map<String, String> insertionMap) {
		Validate.notNull(obj);
		this.savableObj = obj;
		this.insertionMap = insertionMap;
	}
	
	public UUID getUUID() {
		return this.savableObj.getUniqueIdentifier();
	}
	
	public Saveable getObject() {
		return this.savableObj;
	}
	
	public boolean isUpdate() {
		return insertionMap != null;
	}
	
	public Map<String, String> getInsertionMap() {
		return insertionMap;
	}
}
