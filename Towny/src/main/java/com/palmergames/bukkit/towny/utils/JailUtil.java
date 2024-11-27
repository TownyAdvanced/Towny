package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.palmergames.bukkit.towny.object.Position;
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
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.JailReason;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.util.BookFactory;
import com.palmergames.bukkit.util.BukkitTools;

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
	public static void jailResident(Resident resident, Jail jail, int cell, int hours, JailReason reason, CommandSender jailer){
		if (TownySettings.isAllowingBail() && TownyEconomyHandler.isActive()) {
			double bail = TownySettings.getBailAmount();
			if (resident.isMayor())
				bail = resident.isKing() ? TownySettings.getBailAmountKing() : TownySettings.getBailAmountMayor();
			jailResidentWithBail(resident, jail, cell, hours, bail, reason, jailer);
		} else
			jailResidentWithBail(resident, jail, cell, hours, 0.0, reason, jailer);
	}

	/**
	 * Jails a resident.
	 * 
	 * @param resident Resident being jailed.
	 * @param jail Jail resident is being jailed into.
	 * @param cell JailCell to spawn to.
	 * @param hours Hours resident is jailed for.
	 * @param bail Bail amount to be paid to unjail.
	 * @param reason JailReason resident is jailed for.
	 * @param jailer CommandSender of who did the jailing or null.
	 */
	public static void jailResidentWithBail(Resident resident, Jail jail, int cell, int hours, double bail, JailReason reason, CommandSender jailer) {
		
		// Set senderName
		String senderName = jailer instanceof Player ? (jailer).getName() : "Admin";

		// Fire cancellable event.
		ResidentPreJailEvent event = new ResidentPreJailEvent(resident, jail, cell, hours, bail, reason);
		if (BukkitTools.isEventCancelled(event)) {
			TownyMessaging.sendErrorMsg(jailer, event.getCancelMessage());
			return;
		}

		// Resident should always be online at this point.
		Player jailedPlayer = resident.getPlayer();
		if (jailedPlayer == null) {
			TownyMessaging.sendErrorMsg(jailer, Translatable.of("msg_player_is_not_online", resident.getName()));
			return;
		}

		// Give players an informative book.
		if (TownySettings.isJailBookEnabled())
			sendJailedBookToResident(jailedPlayer, reason, hours, bail);

		// Do per-jail-reason operations here.
		switch(reason) {
		case MAYOR:
			// Mayor-initiated Jailings can use a teleport warmup. Prevent logging out.
			if (TownySettings.doesJailingPreventLoggingOut())
				addJailedPlayerToLogOutMap(resident);
			break;
		case OUTLAW_DEATH:
		case PRISONER_OF_WAR:
		default:
		}

		String jailName = jail.hasName() ? jail.getName() : Translatable.of("jail_sing").toString();
		// Send feedback message to arresting town
		if (TownySettings.isAllowingBail() && bail > 0 && TownyEconomyHandler.isActive())
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_player_has_been_sent_to_jail_into_cell_number_x_for_x_hours_by_x_for_x_bail", resident.getName(), jailName, cell, hours, bail, senderName));
		else
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_player_has_been_sent_to_jail_into_cell_number_x_for_x_hours_by_x", resident.getName(), jailName, cell, hours, senderName));

		// Set the jail, cells, hours, bail, and add resident to the Universe's jailed resident map.
		resident.setJail(jail);
		resident.setJailCell(Math.max(0, cell - 1));
		resident.setJailHours(hours);
		resident.setJailBailCost(bail);
		resident.save();
		TownyUniverse.getInstance().getJailedResidentMap().add(resident);

		Translator translator = Translator.locale(jailedPlayer);
		// Tell the resident how long they've been jailed for and provide bail information if allowing bail and using economy
		TownyMessaging.sendMsg(jailedPlayer, translator.of("msg_you've_been_jailed_for_x_hours", hours));
		if (TownySettings.isAllowingBail() && bail > 0 && TownyEconomyHandler.isActive())
			TownyMessaging.sendMsg(jailedPlayer, translator.of("msg_you_have_been_jailed_your_bail_is_x", bail));

		// Inform resident their ability to escape or wait for jailbreak
		TownyMessaging.sendTitleMessageToResident(resident, translator.of("msg_you_have_been_jailed"), translator.of("msg_run_to_the_wilderness_or_wait_for_a_jailbreak"));
		
		// Teleport them (if possible.)
		teleportToJail(resident);
		
		// Call ResidentJailEvent.
		BukkitTools.fireEvent(new ResidentJailEvent(resident, reason, jailer instanceof Player player ? player : null));

		if (TownySettings.showBailTitle())
			Towny.getPlugin().getScheduler().runLater(() -> showBailTitleMessage(resident, translator), 80L);
	}

	public static void showBailTitleMessage(Resident resident, Translator translator) {
		if (!resident.isOnline() || !resident.isJailed() || resident.getJailBailCost() <= 0)
			return;
		TownyMessaging.sendTitleMessageToResident(resident,
				translator.of("titlemsg_pay_your_bail_title", TownyEconomyHandler.getFormattedBalance(resident.getJailBailCost())),
				translator.of("titlemsg_pay_your_bail_subtitle"),
				200); // 10 seconds
		Towny.getPlugin().getScheduler().runLater(() -> showBailTitleMessage(resident, translator), 200L);
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
		case ESCAPE -> {
			town = resident.getTownOrNull();
			
			// First show a message to the resident, either by broadcasting to the resident's town or just the resident (if they have no town.)
			if (town != null)
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_player_escaped_jail_into_wilderness", resident.getName(), jail.getWildName()));
			else 
				TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_been_freed_from_jail"));
			
			// Second, show a message to the town which has just had a prisoner escape.
			if (town != null && !town.getUUID().equals(jail.getTown().getUUID()))
				TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_player_escaped_jail_into_wilderness", resident.getName(), jail.getWildName()));
			unJailResident(resident);
		}
		case LEFT_TOWN -> {
			town = resident.getTownOrNull();
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_been_freed_from_jail"));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_player_escaped_jail_by_leaving_town", resident.getName()));
			unJailResident(resident);
		}
		case BAIL -> {
			unjailAndTeleportAwayFromJail(resident);
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_paid_bail"));
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_has_paid_bail", resident.getName()));
		}
		case SENTENCE_SERVED -> {
			unjailAndTeleportAwayFromJail(resident);
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_served_your_sentence_and_are_free"));
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_x_has_served_their_sentence_and_is_free", resident.getName()));
		}
		case OUT_OF_SPACE -> {
			unjailAndTeleportAwayFromJail(resident);
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_were_released_as_town_ran_out_of_slots"));
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_x_has_been_released_as_jail_slots_ran_out", resident.getName()));
		} 
		case INSUFFICIENT_FUNDS -> {
			unjailAndTeleportAwayFromJail(resident);
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_were_released_as_town_ran_out_of_upkeep_funds"));
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_x_has_been_released_ran_out_of_upkeep_funds", resident.getName()));
		}
		case PARDONED, JAIL_DELETED, ADMIN -> {
			unjailAndTeleportAwayFromJail(resident);
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_been_freed_from_jail"));
			TownyMessaging.sendPrefixedTownMessage(jail.getTown(), Translatable.of("msg_x_has_been_freed_from_x", resident.getName(), jailName));
		}
		case JAILBREAK -> {
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_been_freed_via_jailbreak"));
			unJailResident(resident);
		}
		}
		BukkitTools.fireEvent(new ResidentUnjailEvent(resident, reason));
	}

	public static void unJailResident(Resident resident) {
		TownyUniverse.getInstance().getJailedResidentMap().remove(resident);
		resident.setJailCell(0);
		resident.setJailHours(0);
		resident.setJail(null);
		resident.setJailBailCost(0.00);
		resident.save();
	}

	/**
	 * A wonderful little handbook to help out the sorry jailed person.
	 * 
	 * @param player Player who will receive a book.
	 * @param reason JailReason the player is in jail for.
	 */
	private static void sendJailedBookToResident(Player player, JailReason reason, int hours, double cost) {
		final Translator translator = Translator.locale(player);

		// A nice little book for the not so nice person in jail.
		String pages = getJailBookPages(player, reason, hours, cost, translator);

		// Send the book off to the BookFactory to be made.
		ItemStack jailBook = new ItemStack(BookFactory.makeBook(translator.of("msg_jailed_title"), translator.of("msg_jailed_author"), pages));

		// Run one tick later in case the player died being put in jail.
		Towny.getPlugin().getScheduler().runLater(player, () -> player.getInventory().addItem(jailBook), 1L);
	}

	private static String getJailBookPages(Player player, JailReason reason, int hours, double cost, Translator translator) {
		String pages = translator.of("msg_jailed_handbook_1", translator.of(reason.getCause()));
		pages += translator.of("msg_jailed_handbook_2") + "\n\n";
		pages += translator.of("msg_jailed_handbook_3", hours) + "\n\n";
		pages += TownySettings.JailDeniesTownLeave() ? translator.of("msg_jailed_handbook_4_cant") : translator.of("msg_jailed_handbook_4_can") + "\n";
		if (TownySettings.isAllowingBail() && TownyEconomyHandler.isActive()) {
			pages += translator.of("msg_jailed_handbook_bail_1");
			if (reason == JailReason.PRISONER_OF_WAR) {
				Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
				if (resident.isMayor())
					cost = resident.isKing() ? TownySettings.getBailAmountKing() : TownySettings.getBailAmountMayor();
			}
			pages += translator.of("msg_jailed_handbook_bail_2", TownyEconomyHandler.getFormattedBalance(cost)) + "\n\n";
		}
		pages += translator.of("msg_jailed_handbook_5");
		pages += translator.of("msg_jailed_handbook_6");
		if (TownySettings.JailAllowsTeleportItems())
			pages += translator.of("msg_jailed_teleport");
		pages += "\n\n";
		if (reason.equals(JailReason.PRISONER_OF_WAR))
			pages += translator.of("msg_jailed_war_prisoner");

		return pages;
	}

	public static void createJailPlot(TownBlock townBlock, Town town, Location location) throws TownyException {
		UUID uuid = UUID.randomUUID();
		List<Position> jailSpawns = new ArrayList<>(1);
		jailSpawns.add(Position.ofLocation(location));
		Jail jail = new Jail(uuid, town, townBlock, jailSpawns);
		TownyUniverse.getInstance().registerJail(jail);
		jail.save();
		town.addJail(jail);
		townBlock.setJail(jail);
	}
	
	private static void unjailAndTeleportAwayFromJail(Resident resident) {
		// Don't teleport a player who isn't online.
		if (!resident.isOnline()) return;
		// Don't teleport a player if the config is set to not allow it, but unjail them.
		if (!TownySettings.doesUnjailingTeleportPlayer()) {
			unJailResident(resident);
			return;
		}

		// Players were exploiting a back-to-body tp command in between being unjailed and their teleport out of jail,
		// allowing them to pay bail, tp to their body, then have towny tp them to their town spawn.
		// Being unjailed means that Towny doesn't block the player using the blocked-jails-commands.
		// This delay is used to unjail them 10 ticks earlier than their teleport by Towny, so that they are
		// blocked from using any denied-while-jailed commands until right before they tp.
		long delay = Math.max(0, ((TownySettings.getTeleportWarmupTime() * 20L) - 10L));
		Towny.getPlugin().getScheduler().runLater(resident.getPlayer(), () -> unJailResident(resident), delay);

		// Set up for a teleport out of jail.
		SpawnUtil.jailAwayTeleport(resident);
	}
	
	private static void teleportToJail(Resident resident) {
		// Send a player to their jail cell.
		TownyMessaging.sendMsg(resident, Translatable.of("msg_you_are_being_sent_to_jail"));
		SpawnUtil.jailTeleport(resident);
	}

	private static void addJailedPlayerToLogOutMap(Resident resident) {
		queuedJailedResidents.add(resident);
		TownyMessaging.sendMsg(resident, Translatable.of("msg_do_not_log_out_while_waiting_to_be_teleported"));
		Towny.getPlugin().getScheduler().runLater(() -> queuedJailedResidents.remove(resident), TownySettings.getTeleportWarmupTime() * 20L);
		
	}

	public static boolean isQueuedToBeJailed(Resident resident) {
		return queuedJailedResidents.contains(resident);
	}
	
	public static void maxJailedUnjail(Town town) {
		// Grab jailedResidents list from town
		Stream<Resident> jailedResidents = town.getJailedResidents().stream();
		Resident unjailedresident = TownySettings.getMaxJailedNewJailBehavior() == 1
				// Setting 1 gets the jailed player with lowest JailHours
				? jailedResidents.min(Comparator.comparingInt(Resident::getJailHours)).get()
				// Setting 2 gets the jailed player with lowest set Bail
				: jailedResidents.min(Comparator.comparingDouble(Resident::getJailBailCost)).get();
		unJailResident(unjailedresident, UnJailReason.OUT_OF_SPACE);
	}
}
