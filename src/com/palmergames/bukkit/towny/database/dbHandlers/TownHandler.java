package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.SQLData;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyInviter;

import java.util.UUID;

public class TownHandler implements SerializationHandler<Town> {
	@Override
	public Town loadString(LoadContext context, String str) {
		UUID townID = UUID.fromString(str);
		
		try {
			return TownyUniverse.getInstance().getTown(townID);
		} catch (NotRegisteredException e) {
			return null;
		}
	}

	@Override
	public Town loadSQL(LoadContext context, Object result) {
		return null;
	}

	@Override
	public String getFileString(SaveContext context, Town obj) {
		return obj.getUniqueIdentifier().toString();
	}

	@Override
	public SQLData getSQL(SaveContext context, Town obj) {
		return null;
	}
}
