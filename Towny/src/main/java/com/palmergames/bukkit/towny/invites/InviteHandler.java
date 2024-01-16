package com.palmergames.bukkit.towny.invites;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author - Articdive
 */
public class InviteHandler {
	@SuppressWarnings("unused")
	private static Towny plugin;
	
	private static final Map<Invite, Long> ACTIVE_INVITES = new ConcurrentHashMap<>();

	public static void initialize(Towny plugin) {

		InviteHandler.plugin = plugin;
	}

	public static void acceptInvite(Invite invite) throws InvalidObjectException, TownyException {
		if (ACTIVE_INVITES.containsKey(invite)) {
			invite.accept();
			removeInvite(invite);
			return;
		}
		throw new InvalidObjectException("Invite not valid!"); // I throw this as a backup (failsafe)
		// It shouldn't be possible for this exception to happen via normally using Towny
	}

	public static void declineInvite(Invite invite, boolean fromSender) throws InvalidObjectException {
		if (ACTIVE_INVITES.containsKey(invite)) {
			invite.decline(fromSender);
			removeInvite(invite);
			return;
		}
		throw new InvalidObjectException("Invite not valid!"); // I throw this as a backup (failsafe)
		// It shouldn't be possible for this exception to happen via normally using Towny
	}
	
	public static void addInvite(Invite invite) {
		ACTIVE_INVITES.put(invite, System.currentTimeMillis());
	}
	
	public static void removeInvite(Invite invite) {
		ACTIVE_INVITES.remove(invite);
	}
	
	private static long getInviteTime(Invite invite) {
		return ACTIVE_INVITES.getOrDefault(invite, 0L); 
	}
	
	public static void searchForExpiredInvites() {
		final long time = TownySettings.getInviteExpirationTime() * 1000;
		for (Invite activeInvite : new ArrayList<>(getActiveInvites())) {
			if (getInviteTime(activeInvite) + time < System.currentTimeMillis()) {
				// This is a nation to nation ally invite.
				if (activeInvite.getReceiver() instanceof Nation receiver && activeInvite.getSender() instanceof Nation sender) {
					receiver.deleteReceivedInvite(activeInvite);
					sender.deleteSentAllyInvite(activeInvite);
				// This is a town to resident or nation to town invite.
				} else {
					activeInvite.getReceiver().deleteReceivedInvite(activeInvite);
					activeInvite.getSender().deleteSentInvite(activeInvite);
				}
				removeInvite(activeInvite);
			}
		}
	}
	
	public static Collection<Invite> getActiveInvites() {
		return Collections.unmodifiableSet(ACTIVE_INVITES.keySet());
	}
	
	public static boolean inviteIsActive(Invite invite) {
		for (Invite activeInvite : ACTIVE_INVITES.keySet()) {
			if (activeInvite.getReceiver().equals(invite.getReceiver()) && activeInvite.getSender().equals(invite.getSender()))
				return true;
		}
		return false;
	}
	
	public static boolean inviteIsActive(InviteSender sender, InviteReceiver receiver) {
		for (Invite activeInvite : ACTIVE_INVITES.keySet()) {
			if (activeInvite.getReceiver().equals(receiver) && activeInvite.getSender().equals(sender))
				return true;
		}
		return false;
	}

	public static List<Invite> getActiveInvitesFor(InviteSender sender, InviteReceiver receiver) {
		return ACTIVE_INVITES.keySet().stream()
				.filter(activeInvite -> activeInvite.getReceiver().equals(receiver) && activeInvite.getSender().equals(sender))
				.collect(Collectors.toList());
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

	public static int getReceivedInvitesMaxAmount(InviteReceiver receiver) {

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

	public static int getSentInvitesMaxAmount(InviteSender sender) {
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
