package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.SQLStringType;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.database.handler.annotations.SQLString;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Town;

import java.util.UUID;

public class TownHandler implements SerializationHandler<Town> {
	@Override
	public Town loadString(String str) {
		UUID townID = UUID.fromString(str);
		
		try {
			return TownyUniverse.getInstance().getTown(townID);
		} catch (NotRegisteredException e) {
			return null;
		}
	}
	
	@Override
	@SQLString(stringType = SQLStringType.VARCHAR, length = 36)
	public String toStoredString(Town obj) {
		return obj.getUniqueIdentifier().toString();
	}
}
