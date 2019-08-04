package com.palmergames.bukkit.towny.invites;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.inviteobjects.NationAllyNationInvite;
import com.palmergames.bukkit.towny.object.inviteobjects.PlayerJoinTownInvite;
import com.palmergames.bukkit.towny.object.inviteobjects.TownJoinNationInvite;
import com.palmergames.bukkit.util.ChatTools;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author - Articdive
 */
public class InviteHandler {
	private static Towny plugin;
	private static ListMultimap<Town, Resident> towntoresidentinvites = ArrayListMultimap.create();
	private static ListMultimap<Nation, Town> nationtotowninvites = ArrayListMultimap.create();
	private static ListMultimap<Nation, Nation> nationtonationinvites = ArrayListMultimap.create();

	public static void initialize(Towny plugin) {

		InviteHandler.plugin = plugin;
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
				TownyMessaging.sendTownMessage(town, ChatTools.color(String.format(TownySettings.getLangString("msg_join_town"), resident.getName())));
				getTowntoresidentinvites().remove(town, resident);
				resident.deleteReceivedInvite(invite);
				town.deleteSentInvite(invite);
				return;
			}
		}
		if (receiver instanceof Town) {
			if (sender instanceof Nation) { // Has to be true!
				Town town = (Town) invite.getReceiver();
				List<Town> towns = new ArrayList<Town>();
				towns.add(town);
				Nation nation = (Nation) invite.getSender();
				NationCommand.nationAdd(nation, towns);
				// Message handled in nationAdd()
				getNationtotowninvites().remove(nation, town);
				town.deleteReceivedInvite(invite);
				nation.deleteSentInvite(invite);
				return;
			}
			// Nation invited Town
		}
		if (receiver instanceof Nation) {
			if (sender instanceof Nation) { // Has to be true!
				Nation receivernation = (Nation) invite.getReceiver();
				Nation sendernation = (Nation) invite.getSender();
				receivernation.addAlly(sendernation);
				sendernation.addAlly(receivernation);
				TownyMessaging.sendNationMessage(receivernation, String.format(TownySettings.getLangString("msg_added_ally"), sendernation.getName()));
				TownyMessaging.sendNationMessage(sendernation, String.format(TownySettings.getLangString("msg_accept_ally"), receivernation.getName()));
				getNationtonationinvites().remove(sendernation, receivernation);
				receivernation.deleteReceivedInvite(invite);
				sendernation.deleteSentAllyInvite(invite);
				return;
			}
			// Nation invited other Nation to ally
		}
		throw new InvalidObjectException("Invite not valid!"); // I throw this as a backup (failsafe)
		// It shouldn't be possible for this exception to happen via normally using Towny
	}

	public static void declineInvite(Invite invite, boolean fromSender) throws InvalidObjectException {
		TownyInviteSender sender = invite.getSender();
		TownyInviteReceiver receiver = invite.getReceiver();
		if (receiver instanceof Resident) {
			// Town invited Resident
			if (sender instanceof Town) { // Has to be true!
				Resident resident = (Resident) invite.getReceiver();
				Town town = (Town) invite.getSender();
				getTowntoresidentinvites().remove(town, resident);
				resident.deleteReceivedInvite(invite);
				town.deleteSentInvite(invite);
				if (!fromSender) {
					TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_deny_invite"), resident.getName()));
					TownyMessaging.sendMessage(invite.getReceiver(), TownySettings.getLangString("successful_deny"));
				} else {
					TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("town_revoke_invite"), town.getName()));
				}
				return;
			}
		}
		if (receiver instanceof Town) {
			if (sender instanceof Nation) { // Has to be true!
				Town town = (Town) invite.getReceiver();
				Nation nation = (Nation) invite.getSender();
				getNationtotowninvites().remove(nation, town);
				town.deleteReceivedInvite(invite);
				nation.deleteSentInvite(invite);
				if (!fromSender) {
					TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_deny_invite"), town.getName()));
				} else {
					TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("nation_revoke_invite"), nation.getName()));
				}
				return;
			}
			// Nation invited Town
		}
		if (receiver instanceof Nation) {
			if (sender instanceof Nation) { // Has to be true!
				Nation receivernation = (Nation) invite.getReceiver();
				Nation sendernation = (Nation) invite.getSender();
				getNationtonationinvites().remove(sendernation, receivernation);
				receivernation.deleteReceivedInvite(invite);
				sendernation.deleteSentAllyInvite(invite);
				if (!fromSender) {
					TownyMessaging.sendNationMessage(sendernation, String.format(TownySettings.getLangString("msg_deny_ally"), TownySettings.getLangString("nation_sing") + ": " + receivernation.getName()));
				} else {
					TownyMessaging.sendNationMessage(receivernation, String.format(TownySettings.getLangString("nation_revoke_ally"), sendernation.getName()));
				}

				return;
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

	public static void addInviteToList(PlayerJoinTownInvite invite) {
		towntoresidentinvites.put((Town) invite.getSender(), (Resident) invite.getReceiver());
	}

	public static void addInviteToList(TownJoinNationInvite invite) {
		nationtotowninvites.put((Nation) invite.getSender(), (Town) invite.getReceiver());
	}

	public static void addInviteToList(NationAllyNationInvite invite) {
		nationtonationinvites.put((Nation) invite.getSender(), (Nation) invite.getReceiver());
	}

	public static int getReceivedInvitesAmount(TownyInviteReceiver receiver) {
		List<Invite> invites = receiver.getReceivedInvites();
		return invites.size();
	}

	public static int getSentInvitesAmount(TownyInviteSender sender) {
		List<Invite> invites = sender.getSentInvites();
		return invites.size();
	}

	public static int getSentAllyRequestsAmount(TownyAllySender sender) {
		List<Invite> invites = sender.getSentAllyInvites();
		return invites.size();
	}

	public static int getSentAllyRequestsMaxAmount(TownyAllySender sender) {
		int amount = 0;
		if (sender instanceof Nation) {
			if (TownySettings.getMaximumRequestsSentNation() == 0){
				amount = 100;
			} else {
				amount = TownySettings.getMaximumRequestsSentNation();
			}
		}
		return amount;
	}

	public static int getReceivedInvitesMaxAmount(TownyInviteReceiver receiver) {

		int amount = 0;
		if (receiver instanceof Resident) {
			if (TownySettings.getMaximumInvitesReceivedResident() == 0) {
				amount = 100;
			} else {
				amount = TownySettings.getMaximumInvitesReceivedResident();
			}
		}
		if (receiver instanceof Town) {
			if (TownySettings.getMaximumInvitesReceivedTown() == 0) {
				amount = 100;
			} else {
				amount = TownySettings.getMaximumInvitesReceivedTown();
			}
		}
		if (receiver instanceof Nation) {
			if (TownySettings.getMaximumRequestsReceivedNation() == 0) {
				amount = 100;
			} else {
				amount = TownySettings.getMaximumRequestsReceivedNation();
			}
		}
		return amount;
	}

	public static int getSentInvitesMaxAmount(TownyInviteSender sender) {
		int amount = 0;
		if (sender instanceof Town) {
			if (TownySettings.getMaximumInvitesSentTown() == 0) {
				amount = 100;
			} else {
				amount = TownySettings.getMaximumInvitesSentTown();
			}
		}
		if (sender instanceof Nation) {
			if (TownySettings.getMaximumInvitesSentNation() == 0) {
				amount = 100;
			} else {
				amount = TownySettings.getMaximumInvitesSentNation();
			}
		}
		return amount;
	}

}
