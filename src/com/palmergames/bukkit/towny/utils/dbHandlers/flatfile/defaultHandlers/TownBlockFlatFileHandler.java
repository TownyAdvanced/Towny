package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileDatabaseHandler;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadContext;

public class TownBlockFlatFileHandler implements FlatFileDatabaseHandler<TownBlock> {

	@Override
	public TownBlock load(FlatFileLoadContext context, String str) {
		
		String[] townBlockElements = str.split(",");
		try {
			TownyWorld world = getWorld(townBlockElements[0]);

			try {
				int x = Integer.parseInt(townBlockElements[1]);
				int z = Integer.parseInt(townBlockElements[2]);
				return world.getTownBlock(x, z);
			} catch (NumberFormatException e) {
				TownyMessaging.sendErrorMsg("[Warning] homeBlock tried to load invalid location.");
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg("[Warning] homeBlock tried to load invalid TownBlock.");
			}

		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg("[Warning] homeBlock tried to load invalid world.");
		}
		
		return null;
	}

	private TownyWorld getWorld(String name) throws NotRegisteredException {

		TownyWorld world = TownyUniverse.getInstance().getWorldMap().get(name.toLowerCase());

		if (world == null)
			throw new NotRegisteredException("World not registered!");

		return world;
	}

	@Override
	public String save(TownBlock object) {
		return object.getWorld() + "," + object.getX() + "," + object.getZ();
	}
}
