package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.SQLStringType;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.database.handler.annotations.SQLString;

import java.util.UUID;

public class UUIDHandler implements SerializationHandler<UUID> {

	@Override
	public UUID loadString(String str) {
		return UUID.fromString(str);
	}

	@Override
	@SQLString(stringType = SQLStringType.VARCHAR, length = 36)
	public String toStoredString(UUID obj) {
		return obj.toString();
	}
}
