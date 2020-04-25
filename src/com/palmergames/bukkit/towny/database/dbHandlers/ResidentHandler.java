package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;

import java.util.UUID;

public class ResidentHandler implements SerializationHandler<Resident> {

	@Override
	public String toStoredString(SaveContext context, Resident obj) {
		return obj.getUniqueIdentifier().toString();
	}

	@Override
	public Resident loadString(LoadContext context, String str) {
		UUID id = UUID.fromString(str);
		
		try {
			return TownyUniverse.getInstance().getResident(id);
		} catch (NotRegisteredException ignore) {
			return null;
		}
	}
}
