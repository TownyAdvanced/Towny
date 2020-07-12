package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SaveHandler;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import org.apache.commons.lang.StringUtils;

import java.util.StringJoiner;
import java.util.UUID;

public class TownBlockHandler implements LoadHandler<TownBlock>, SaveHandler<TownBlock> {
	
	@Override
	public String toStoredString(SaveContext context, TownBlock obj) {

		StringJoiner joiner = new StringJoiner(";");
		joiner.add(context.toStoredString(obj.getWorld(), TownyWorld.class));
		joiner.add(obj.getX() + "");
		joiner.add(obj.getZ() + "");
		
		return "{" + joiner.toString() + "}";
	}

	@Override
	public TownBlock loadString(LoadContext context, String str) {
		
		String elements = StringUtils.substringBetween(str, "{", "}");
		String[] townBlockElements = elements.split(";");
		try {
			TownyWorld world = getWorld(UUID.fromString(townBlockElements[0]));

			try {
				int x = Integer.parseInt(townBlockElements[1]);
				int z = Integer.parseInt(townBlockElements[2]);
				return world.getTownBlock(x, z);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				TownyMessaging.sendErrorMsg("[Warning] homeBlock tried to loadString invalid location.");
			} catch (NotRegisteredException e) {
				e.printStackTrace();
				TownyMessaging.sendErrorMsg("[Warning] homeBlock tried to loadString invalid TownBlock.");
			}

		} catch (NotRegisteredException e) {
			e.printStackTrace();
			TownyMessaging.sendErrorMsg("[Warning] homeBlock tried to loadString invalid world.");
		}
		
		return null;
	}

	private TownyWorld getWorld(UUID uniqueIdentifier) throws NotRegisteredException {

		TownyWorld world = TownyUniverse.getInstance().getWorld(uniqueIdentifier);

		if (world == null)
			throw new NotRegisteredException("World not registered!");

		return world;
	}
}
