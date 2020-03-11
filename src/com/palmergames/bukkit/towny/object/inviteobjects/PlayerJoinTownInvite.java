package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.TownyInviteReceiver;
import com.palmergames.bukkit.towny.invites.TownyInviteSender;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public class PlayerJoinTownInvite implements Invite {

	public PlayerJoinTownInvite(String directsender, TownyInviteSender sender, TownyInviteReceiver receiver) {
		this.directsender = directsender;
		this.sender = sender;
		this.receiver = receiver;
	}

	private String directsender;
	private TownyInviteReceiver receiver;
	private TownyInviteSender sender;

	@Override
	public String getDirectSender() {
		return directsender;
	}

	@Override
	public TownyInviteReceiver getReceiver() {
		return receiver;
	}

	@Override
	public TownyInviteSender getSender() {
		return sender;
	}

	@Override
	public void accept() throws TownyException {
		Resident resident = (Resident) getReceiver();
		Town town = (Town) getSender();
		TownCommand.townAddResident(town, resident);
		TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_join_town"), resident.getName()));
		resident.deleteReceivedInvite(this);
		town.deleteSentInvite(this);
	}

	@Override
	public void decline(boolean fromSender) {
		Resident resident = (Resident) getReceiver();
		Town town = (Town) getSender();
		resident.deleteReceivedInvite(this);
		town.deleteSentInvite(this);
		if (!fromSender) {
			TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_deny_invite"), resident.getName()));
			TownyMessaging.sendMsg(getReceiver(), TownySettings.getLangString("successful_deny"));
		} else {
			TownyMessaging.sendMsg(resident, String.format(TownySettings.getLangString("town_revoke_invite"), town.getName()));
		}
	}
}
