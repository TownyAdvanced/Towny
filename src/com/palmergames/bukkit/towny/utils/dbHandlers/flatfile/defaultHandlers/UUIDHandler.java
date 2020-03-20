package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadHandler;

import java.util.UUID;

public class UUIDHandler implements FlatFileLoadHandler<UUID> {

	@Override
	public UUID load(FlatFileLoadContext context, String str) {
		return UUID.fromString(str);
	}
}
