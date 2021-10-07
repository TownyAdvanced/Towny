package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.resident.ResidentJailEvent;
import com.palmergames.bukkit.towny.event.resident.ResidentPreJailEvent;
import com.palmergames.bukkit.towny.event.resident.ResidentUnjailEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.JailReason;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.util.BookFactory;

public class JailUtil {

	private static List<Resident> queuedJailedResidents = new ArrayList<Resident>();
	
	/**
	 * Jails a resident.
	 * 
	 * @param resident Resident being jailed.
	 * @param jail Jail resident is being jailed into.
	 * @param cell JailCell to spawn to.
	 * @param hours Hours resident is jailed for.
	 * @param reason JailReason resident is jailed for.
	 * @param jailer CommandSender of who did the jailing or null.
	 */
	public static void jailResident(Resident resident, Jail jail, int cell, int hours, JailReason reason, CommandSender jailer) {
		
		// Set senderName
		String senderName = jailer instanceof Player ? (jailer).getName() : "Admin";

		// Stop mayors from setting incredibly high hours.
		if (hours > 10000)
			hours = 10000;

		// Fire cancellable event.
		ResidentPreJailEvent event = new ResidentPreJailEvent(resident, jail, cell, hours, reason);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			TownyMessaging.sendErrorMsg(jailer, event.getCancelMessage());
			return;
		}
		
		// Set players an informative book.
		sendJailedBookToResident(resident.getPlayer(), reason);

		// Send feedback messages. 
		switch(reason) {
		case MAYOR:
			Object jailName = jail.hasName() ? jail.getName() : Translatable.of("jail_sing");
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_player_has_been_sent_to_jail_into_cell_number_x_for_x_hours_by_x", resident.getName(), jailName, cell+1, hours, senderName));
			if (TownySettings.doesJailingPreventLoggingOut())
				addJailedPlayerToLogOutMap(resident);
			break;
		case OUTLAW_DEATH:
		case PRISONER_OF_WAR:
			TownyMessaging.sendTitleMessageToResident(resident, Translatable.of("msg_you_have_been_jailed").forLocale(resident), Translatable.of("msg_run_to_the_wilderness_or_wait_for_a_jailbreak").forLocale(resident));
			break;
		}
		
		// Set the jail, cells, hours, and add resident to the Universe's jailed resident map.
		resident.setJail(jail);
		resident.setJailCell(cell);
		resident.setJailHours(hours);
		resident.save();
		TownyUniverse.getInstance().getJailedResidentMap().add(resident);
		
		// Tell the resident how long they've been jailed for.
		TownyMessaging.sendMsg(resident, Translatable.of("msg_you've_been_jailed_for_x_hours", hours));

		// Teleport them (if possible.)
		teleportToJail(resident);
		
		// Call ResidentJailEvent.
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
		String jailName = jail.hasName() ? jail.getName() : ", cell unknown.";
		Town town = null;
		switch (reason) {
		case ESCAPE:
			town = resident.getTownOrNull();
			
			// First show a message to the resident, either by broadcasting to the resident's town or just the resident (if they have no town.)
			if (town != null)
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_player_escaped_jail_into_wilderness", resident.getName(), jail.getWildName()));
			else 
				TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_been_freed_from_jail"));
			
			// Second, show a message to the town which has just had a prisoner escape.
			if (town != null && !town.getUUID().equals(jail.getTown().getUUID()))
				TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_player_escaped_jail_into_wilderness", resident.getName(), jail.getWildName()));
			break;

		case BAIL:
			teleportAwayFromJail(resident);
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_paid_bail"));
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_has_paid_bail", resident.getName()));

			break;
		case SENTENCE_SERVED:
			teleportAwayFromJail(resident);
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_served_your_sentence_and_are_free"));
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_x_has_served_their_sentence_and_is_free", resident.getName()));
			break;
		case LEFT_TOWN:
			town = resident.getTownOrNull();
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_been_freed_from_jail"));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_player_escaped_jail_by_leaving_town", resident.getName()));
			break;
		case PARDONED:
		case JAIL_DELETED:
		case ADMIN:
			teleportAwayFromJail(resident);
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_been_freed_from_jail"));
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_x_has_been_freed_from_x", resident.getName(), jailName));
			break;
		case JAILBREAK:
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_been_freed_via_jailbreak"));			
			break;
		}

		TownyUniverse.getInstance().getJailedResidentMap().remove(resident);
		resident.setJailCell(0);
		resident.setJailHours(0);
		resident.setJail(null);
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
		String pages = Translation.of("msg_jailed_handbook_1", reason.getCause());
		pages += Translation.of("msg_jailed_handbook_2");
		pages += Translation.of("msg_jailed_handbook_3", reason.getHours());
		pages += TownySettings.JailDeniesTownLeave() ? Translation.of("msg_jailed_handbook_4_cant") : Translation.of("msg_jailed_handbook_4_can");
		if (TownySettings.isAllowingBail() && TownyEconomyHandler.isActive()) {
			pages += Translation.of("msg_jailed_handbook_bail_1");
			double cost = TownySettings.getBailAmount();
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident.isMayor())
				cost = TownySettings.getBailAmountMayor();
			if (resident.isKing())
				cost = TownySettings.getBailAmountKing();
			pages += Translation.of("msg_jailed_handbook_bail_2", TownyEconomyHandler.getFormattedBalance(cost));
		}
		pages += Translation.of("msg_jailed_handbook_5");
		pages += Translation.of("msg_jailed_handbook_6");
		if (TownySettings.JailAllowsTeleportItems())
			pages += Translation.of("msg_jailed_teleport");
		pages += "\n\n";
		if (reason.equals(JailReason.PRISONER_OF_WAR))
			pages += Translation.of("msg_jailed_war_prisoner");
		
		/*
		 * Send the book off to the BookFactory to be made.
		 */
		player.getInventory().addItem(new ItemStack(BookFactory.makeBook(Translation.of("msg_jailed_title"), Translation.of("msg_jailed_author"), pages)));
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
		// Don't teleport a player who isn't online.
		if (!resident.isOnline()) return;
		SpawnUtil.jailAwayTeleport(resident);
	}
	
	private static void teleportToJail(Resident resident) {
		TownyMessaging.sendMsg(resident, Translatable.of("msg_you_are_being_sent_to_jail"));
		SpawnUtil.jailTeleport(resident);
	}

	private static void addJailedPlayerToLogOutMap(Resident resident) {
		queuedJailedResidents.add(resident);
		TownyMessaging.sendMsg(resident, Translatable.of("msg_do_not_log_out_while_waiting_to_be_teleported"));
		Bukkit.getScheduler().scheduleSyncDelayedTask(Towny.getPlugin(), () -> queuedJailedResidents.remove(resident), TownySettings.getTeleportWarmupTime() + 20);
		
	}

	public static boolean isQueuedToBeJailed(Resident resident) {
		return queuedJailedResidents.contains(resident);
	}
	
}
