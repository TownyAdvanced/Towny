package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownyWorld;

import java.util.UUID;

public class TownyWorldHandler implements SerializationHandler<TownyWorld> {

	@Override
	public String toStoredString(TownyWorld obj) {
		return obj.getUniqueIdentifier().toString();
	}

	@Override
	public TownyWorld loadString(String str) {
		try {
			UUID id = UUID.fromString(str);
			return TownyUniverse.getInstance().getWorld(id);
		} catch (NotRegisteredException e) {
			return null;
		}
	}
}
