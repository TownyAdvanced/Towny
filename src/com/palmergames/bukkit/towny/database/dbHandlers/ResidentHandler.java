package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import org.apache.commons.lang.Validate;

public class ResidentHandler implements LoadHandler<Resident> {

	@Override
	public Resident loadString(LoadContext context, String str) {
		return TownyUniverse.getInstance().getResidentMap().get(str);
	}

	@Override
	public Resident loadSQL(LoadContext context, Object result) {
		Validate.isTrue(result instanceof String);
		String data = (String)result;
		return context.fromFileString(data, Resident.class);
	}
}
