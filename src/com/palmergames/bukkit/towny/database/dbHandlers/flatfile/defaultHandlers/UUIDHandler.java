package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadContext;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadHandler;

import java.util.UUID;

public class UUIDHandler implements LoadHandler<UUID> {

	@Override
	public UUID loadString(LoadContext context, String str) {
		return UUID.fromString(str);
	}

	@Override
	public UUID loadSQL(Object result) {
		return null;
	}
}
