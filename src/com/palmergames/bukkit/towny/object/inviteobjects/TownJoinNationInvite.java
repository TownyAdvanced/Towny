package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.TownyInviteReceiver;
import com.palmergames.bukkit.towny.invites.TownyInviteSender;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

import java.util.ArrayList;
import java.util.List;

public class TownJoinNationInvite implements Invite {

	public TownJoinNationInvite(String directsender, TownyInviteSender sender, TownyInviteReceiver receiver) {
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
		Town town = (Town) getReceiver();
		List<Town> towns = new ArrayList<>();
		towns.add(town);
		Nation nation = (Nation) getSender();

		if(TownySettings.getWarSiegeEnabled()
			&& TownySettings.getWarSiegeTownNeutralityEnabled()
			&& (town.isNeutral() || town.getDesiredNeutralityValue())) {
			//Player can only get here in some edge-case scenario/attempted exploit. So no attempted exception or messaging.
		} else {
			NationCommand.nationAdd(nation, towns);
		}
		// Message handled in nationAdd()
		town.deleteReceivedInvite(this);
		nation.deleteSentInvite(this);
	}

	@Override
	public void decline(boolean fromSender) {
		Town town = (Town) getReceiver();
		Nation nation = (Nation) getSender();
		town.deleteReceivedInvite(this);
		nation.deleteSentInvite(this);
		if (!fromSender) {
			TownyMessaging.sendPrefixedNationMessage(nation, String.format(TownySettings.getLangString("msg_deny_invite"), town.getName()));
		} else {
			TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("nation_revoke_invite"), nation.getName()));
		}
	}
}
