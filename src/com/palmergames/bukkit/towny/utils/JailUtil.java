package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.resident.ResidentJailEvent;
import com.palmergames.bukkit.towny.event.resident.ResidentUnjailEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.JailReason;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.util.BookFactory;
import com.palmergames.bukkit.util.Colors;

public class JailUtil {
	
	/**
	 * Jails a resident.
	 * 
	 * @param resident Resident being jailed.
	 * @param town Town resident is being jailed by.
	 * @param jail Jail resident is being jailed into.
	 * @param cell JailCell to spawn to.
	 * @param hours Hours resident is jailed for.
	 * @param reason JailReason resident is jailed for.
	 * @param jailer CommandSender who did the jailing or null. (For Ticket #4096.)
	 */
	public static void jailResident(Resident resident, Jail jail, int cell, int hours, JailReason reason, CommandSender jailer) {
		sendJailedBookToResident(resident.getPlayer(), reason);
		
		switch(reason) {
		case MAYOR:
			
		case OUTLAW_DEATH:
			hours = reason.getHours();
		case PRISONER_OF_WAR:
			hours = reason.getHours();
			TownyMessaging.sendTitleMessageToResident(resident, "You have been jailed", "Run to the wilderness or wait for a jailbreak.");
			break;
		}
		
		resident.setJail(jail);
		resident.setJailed(true);
		if (hours > 10000)
			hours = 10000;
		resident.setJailHours(hours);
		
		TownyMessaging.sendMsg(resident, Translation.of("msg_you've_been_jailed_for_x_days", hours)); //TODO: new lang string here.
		TownyMessaging.sendMsg(resident, Translation.of("msg_you_have_been_sent_to_jail"));

		teleportToJail(resident);
		Bukkit.getPluginManager().callEvent(new ResidentJailEvent(resident));
	
	}

	/**
	 * Unjails a resident.
	 * 
	 * @param resident Resident being unjailed.
	 * @param reason UnJailReason the resident is unjailed for.
	 */
	public static void unJailResident(Resident resident, UnJailReason reason) {
		
		Jail jail = resident.getJail();
		String jailNumber = "Placeholder"; // TODO: replace placeholder/figure out how jails will be named/cells will be named.
		Town town = null;
		switch (reason) {
		case ESCAPE:
			try {
				town = resident.getTown();
			} catch (NotRegisteredException ignored) {}
			
			// First show a message to the resident, either by broadcasting to the resident's town or just the resident (if they have no town.)
			if (town != null)
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_player_escaped_jail_into_wilderness", resident.getName(), jail.getWildName()));
			else 
				TownyMessaging.sendMsg(resident, Translation.of("msg_you_have_been_freed_from_jail"));
			
			// Second, show a message to the town which has just had a prisoner escape.
			if (!resident.hasTown() || (town != null && !town.equals(jail.getTown())))
				TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translation.of("msg_player_escaped_jail_into_wilderness", resident.getName(), jail.getWildName()));
			break;

		case BAIL:
			if (resident.getPlayer().isOnline())
				teleportAwayFromJail(resident);
			
			TownyMessaging.sendGlobalMessage(Colors.Red + resident.getName() + Translation.of("msg_has_paid_bail")); // TODO: Change this from a global message to a different one.

			break;
		case PARDONED:
			break;
		case SENTENCE_SERVED:
			break;
		case LEFT_TOWN:
			town = TownyAPI.getInstance().getResidentTownOrNull(resident);
			assert town != null;
			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_player_escaped_jail_by_leaving_town", resident.getName()));
			break;
		case JAIL_DELETED:
			if (resident.getPlayer().isOnline())
				teleportAwayFromJail(resident);

			// Send a message to the resident and town.
			TownyMessaging.sendMsg(resident, Translation.of("msg_you_have_been_freed_from_jail"));
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translation.of("msg_player_has_been_freed_from_jail_number", resident.getName(), jailNumber));
			break;
		case JAILBREAK:
			break;
		}

		resident.setJailed(false);
		resident.save();
		
		Bukkit.getPluginManager().callEvent(new ResidentUnjailEvent(resident));
	}


	/**
	 * A wonderful little handbook to help out the sorry jailed person.
	 * 
	 * @param player Player who will receive a book.
	 * @param reason JailReason the player is in jail for.
	 */
	private static void sendJailedBookToResident(Player player, JailReason reason) {
		
		/*
		 * A nice little book for the not so nice person in jail.
		 */
		String pages = "Looks like you've been jailed, for the reason " + reason.getCause();
		pages += "That's too bad huh. Well what can you do about it? Here's some helpful tips for you while you serve your sentence.\n\n";
		pages += "You have been jailed for " + reason.getHours() + " hours. By serving your sentence you will become free.\n\n";
		if (TownySettings.JailDeniesTownLeave())
			pages += "While you're jailed you won't be able to leave your town to escape jail.\n";
		else
			pages += "You can escape from jail by leaving your town using /town leave.\n";
		if (TownySettings.isAllowingBail()) {
			pages += "You can also pay your bail using '/res jail paybail' to be freed from jail.";
			double cost = TownySettings.getBailAmount();
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident.isMayor())
				cost = TownySettings.getBailAmountMayor();
			if (resident.isKing())
				cost = TownySettings.getBailAmountKing();
			pages += "Bail will cost: " + TownyEconomyHandler.getFormattedBalance(cost) + "\n\n";
		}
		pages += "If you must persist and make efforts to escape, if you make it to the wilderness you will also earn your freedom.";
		pages += "But don't die before you reach the wilderness or you'll end up right back in jail.";
		if (TownySettings.JailAllowsEnderPearls())
			pages += "Luckily, enderpearls are allowed to be used to help you escape, now you've just got to get a hold of some.";
		pages +="\n\n";
		if (reason.equals(JailReason.PRISONER_OF_WAR))
			pages += "As a prisoner of war you will be freed if your countrymen can reduce the jail plot's HP to 0, or if the town you are jailed in is removed from the war.";
		
		/*
		 * Send the book off to the BookFactory to be made.
		 */
		player.getInventory().addItem(new ItemStack(BookFactory.makeBook("So you've been jailed :(", "Towny Jailco.", pages)));
	}

	public static void createJailPlot(TownBlock townBlock, Town town, Location location) throws TownyException {
		UUID uuid = UUID.randomUUID();
		List<Location> jailSpawns = new ArrayList<Location>(1);
		jailSpawns.add(location);
		Jail jail = new Jail(uuid, town, townBlock, jailSpawns);
		TownyUniverse.getInstance().registerJail(jail);
		jail.save();
		town.addJail(jail);
		townBlock.setJail(jail);
	}
	
	private static void teleportAwayFromJail(Resident resident) {
		// Send a pardoned player to the world spawn, or their town's spawn if they have a town with a spawn.
		Location loc = Bukkit.getWorld(resident.getPlayer().getWorld().getName()).getSpawnLocation();
		try {
			loc = resident.getTown().getSpawn();
		} catch (TownyException e) {}

		// Use teleport warmup
		TownyMessaging.sendMsg(resident, Translation.of("msg_town_spawn_warmup", TownySettings.getTeleportWarmupTime()));
		TownyAPI.getInstance().jailTeleport(resident.getPlayer(), loc);

	}
	
	private static void teleportToJail(Resident resident) {
		// Send a pardoned player to the world spawn, or their town's spawn if they have a town with a spawn.
		Location loc = resident.getJail().getJailCellLocations().get(resident.getJailCell());

		// Use teleport warmup
		TownyMessaging.sendMsg(resident, Translation.of("msg_town_spawn_warmup", TownySettings.getTeleportWarmupTime()));
		TownyAPI.getInstance().jailTeleport(resident.getPlayer(), loc);
	}

}
