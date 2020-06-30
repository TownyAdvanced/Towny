package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.SQLStringType;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.database.handler.annotations.SQLString;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;

public class NationHandler implements SerializationHandler<Nation> {
	@Override
	public Nation loadString(LoadContext context, String str) {
		try {
			return TownyUniverse.getInstance().getNation(str);
		} catch (NotRegisteredException ignore) {
		}
		return null;
	}

	@Override
	@SQLString(stringType = SQLStringType.VARCHAR, length = 36)
	public String toStoredString(SaveContext context, Nation obj) {
		return obj.getUniqueIdentifier().toString();
	}
}
