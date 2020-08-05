package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.SQLStringType;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.database.handler.annotations.SQLString;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;

import java.util.UUID;

public class ResidentHandler implements SerializationHandler<Resident> {

	@Override
	@SQLString(stringType = SQLStringType.VARCHAR, length = 36)
	public String toStoredString(Resident obj) {
		return obj.getUniqueIdentifier().toString();
	}

	@Override
	public Resident loadString(String str) {
		UUID id = UUID.fromString(str);
		
		try {
			return TownyUniverse.getInstance().getResident(id);
		} catch (NotRegisteredException e) {
			e.printStackTrace();
			return null;
		}
	}
}
