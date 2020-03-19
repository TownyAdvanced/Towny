package com.palmergames.bukkit.towny.utils.loadHandlers;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;

public class TownBlockLoadHandler implements LoadHandler<TownBlock> {

	@Override
	public TownBlock load(String str) {
		
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

	public TownyWorld getWorld(String name) throws NotRegisteredException {

		TownyWorld world = TownyUniverse.getInstance().getWorldMap().get(name.toLowerCase());

		if (world == null)
			throw new NotRegisteredException("World not registered!");

		return world;
	}
}
