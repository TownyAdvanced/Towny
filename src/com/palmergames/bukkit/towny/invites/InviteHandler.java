package com.palmergames.bukkit.towny.invites;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author - Articdive
 */
public class InviteHandler {
	@SuppressWarnings("unused")
	private static Towny plugin;
	
	private static final List<Invite> activeInvites = new ArrayList<>();

	public static void initialize(Towny plugin) {

		InviteHandler.plugin = plugin;
	}

	public static void acceptInvite(Invite invite) throws InvalidObjectException, TownyException {
		if (activeInvites.contains(invite)) {
			invite.accept();
			activeInvites.remove(invite);
			return;
		}
		throw new InvalidObjectException("Invite not valid!"); // I throw this as a backup (failsafe)
		// It shouldn't be possible for this exception to happen via normally using Towny
	}

	public static void declineInvite(Invite invite, boolean fromSender) throws InvalidObjectException {
		if (activeInvites.contains(invite)) {
			invite.decline(fromSender);
			activeInvites.remove(invite);
			return;
		}
		throw new InvalidObjectException("Invite not valid!"); // I throw this as a backup (failsafe)
		// It shouldn't be possible for this exception to happen via normally using Towny
	}
	
	public static void addInvite(Invite invite) {
		activeInvites.add(invite);
	}
	
	public static List<Invite> getActiveInvites() {
		return Collections.unmodifiableList(activeInvites);
	}
	
	public static boolean inviteIsActive(Invite invite) {
		for (Invite activeInvite : activeInvites) {
			if (activeInvite.getReceiver().equals(invite.getReceiver()) && activeInvite.getSender().equals(invite.getSender()))
				return true;
		}
		return false;
	}
	
	public static boolean inviteIsActive(TownyInviteSender sender, TownyInviteReceiver receiver) {
		for (Invite activeInvite : activeInvites) {
			if (activeInvite.getReceiver().equals(receiver) && activeInvite.getSender().equals(sender))
				return true;
		}
		return false;
	}

	public static int getReceivedInvitesAmount(TownyInviteReceiver receiver) {
		List<Invite> invites = receiver.getReceivedInvites();
		return invites.size();
	}

	public static int getSentInvitesAmount(TownyInviteSender sender) {
		List<Invite> invites = sender.getSentInvites();
		return invites.size();
	}

	public static int getSentAllyRequestsAmount(Nation sender) {
		List<Invite> invites = sender.getSentAllyInvites();
		return invites.size();
	}

	public static int getSentAllyRequestsMaxAmount(Nation sender) {
		int amount = 0;
		if (sender != null) {
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
