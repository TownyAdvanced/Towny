package com.palmergames.bukkit.towny.invites;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author - Articdive
 */
public class InviteHandler {
	private static Towny plugin;
	private static TownyUniverse universe;
	private static ListMultimap<Town, Resident> towntoresidentinvites = ArrayListMultimap.create();
	private static ListMultimap<Nation, Town> nationtotowninvites = ArrayListMultimap.create();
	private static ListMultimap<Nation, Nation> nationtonationinvites = ArrayListMultimap.create();

	public static void initialize(Towny plugin) {

		InviteHandler.plugin = plugin;
		universe = plugin.getTownyUniverse();
	}

	public static void acceptInvite(Invite invite) throws InvalidObjectException, TownyException {
		TownyInviteSender sender = invite.getSender();
		TownyInviteReceiver receiver = invite.getReceiver();
		if (receiver instanceof Resident) {
			// Town invited Resident
			if (sender instanceof Town) { // Has to be true!
				Resident resident = (Resident) invite.getReceiver();
				Town town = (Town) invite.getSender();
				TownCommand.townAddResident(town, resident);
			}
		}
		if (receiver instanceof Town) {
			if (sender instanceof Nation) { // Has to be true!
				Town town = (Town) invite.getReceiver();
				List<Town> towns = new ArrayList();
				towns.add(town);
				Nation nation = (Nation) invite.getSender();
				NationCommand.nationAdd(nation, towns);
			}
			// Nation invited Town
		}
		if (receiver instanceof Nation) {
			if (sender instanceof Nation) { // Has to be true!
				Nation receivernation = (Nation) invite.getReceiver();
				Nation sendernation = (Nation) invite.getSender();
				receivernation.addAlly(sendernation);
				sendernation.addAlly(receivernation);
			}
			// Nation invited other Nation to ally
		}
		throw new InvalidObjectException("Invite not valid!"); // I throw this as a backup (failsafe)
		// It shouldn't be possible for this exception to happen via normally using Towny
	}

	public static void declineInvite(Invite invite) throws InvalidObjectException, TownyException {
		TownyInviteSender sender = invite.getSender();
		TownyInviteReceiver receiver = invite.getReceiver();
		if (receiver instanceof Resident) {
			// Town invited Resident
			if (sender instanceof Town) { // Has to be true!
				Resident resident = (Resident) invite.getReceiver();
				Town town = (Town) invite.getSender();
				resident.deleteReceivedInvite(invite);
				town.deleteSentInvite(invite);
			}
		}
		if (receiver instanceof Town) {
			if (sender instanceof Nation) { // Has to be true!
				Town town = (Town) invite.getReceiver();
				Nation nation = (Nation) invite.getSender();
				town.deleteReceivedInvite(invite);
				nation.deleteSentInvite(invite);

			}
			// Nation invited Town
		}
		if (receiver instanceof Nation) {
			if (sender instanceof Nation) { // Has to be true!
				Nation receivernation = (Nation) invite.getReceiver();
				Nation sendernation = (Nation) invite.getSender();
				receivernation.deleteReceivedInvite(invite);
				sendernation.deleteSentInvite(invite);
			}
			// Nation invited other Nation to ally
		}
		throw new InvalidObjectException("Invite not valid!"); // I throw this as a backup (failsafe)
		// It shouldn't be possible for this exception to happen via normally using Towny
	}

	public static ListMultimap<Nation, Nation> getNationtonationinvites() {
		return nationtonationinvites;
	}

	public static ListMultimap<Nation, Town> getNationtotowninvites() {
		return nationtotowninvites;
	}

	public static ListMultimap<Town, Resident> getTowntoresidentinvites() {
		return towntoresidentinvites;
	}
}
