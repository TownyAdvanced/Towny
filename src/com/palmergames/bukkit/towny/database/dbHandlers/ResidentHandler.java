package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.SQLData;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import org.apache.commons.lang.Validate;

import java.util.UUID;

public class ResidentHandler implements SerializationHandler<Resident> {

	@Override
	public Resident loadString(LoadContext context, String str) {
		try {
			return TownyUniverse.getInstance().getResident(UUID.fromString(str));
		} catch (NotRegisteredException ignore) {
		}
		return null;
	}

	@Override
	public Resident loadSQL(LoadContext context, Object result) {
		Validate.isTrue(result instanceof String, "SQL Object not expected type: " + result);
		String data = (String)result;
		return context.fromFileString(data, Resident.class);
	}

	@Override
	public String getFileString(SaveContext context, Resident obj) {
		return obj.getUniqueIdentifier().toString();
	}

	@Override
	public SQLData getSQL(SaveContext context, Resident obj) {
		return null;
	}
}
