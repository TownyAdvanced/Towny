package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.SQLStringType;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.database.handler.annotations.SQLString;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;

public class NationHandler implements SerializationHandler<Nation> {
	@Override
	public Nation loadString(String str) {
		try {
			return TownyUniverse.getInstance().getNation(str);
		} catch (NotRegisteredException ignore) {
		}
		return null;
	}

	@Override
	@SQLString(stringType = SQLStringType.VARCHAR, length = 36)
	public String toStoredString(Nation obj) {
		return obj.getUniqueIdentifier().toString();
	}
}
