package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.database.Saveable;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class QueuedObject {
	private final Saveable savableObj;
	private final boolean isUpdate;
	
	public QueuedObject(@NotNull Saveable obj, boolean isUpdate) {
		Validate.notNull(obj);
		this.savableObj = obj;
		this.isUpdate = isUpdate;
	}
	
	public UUID getUUID() {
		return this.savableObj.getUniqueIdentifier();
	}
	
	public Saveable getObject() {
		return this.savableObj;
	}
	
	public boolean isUpdate() {
		return isUpdate;
	}
}
