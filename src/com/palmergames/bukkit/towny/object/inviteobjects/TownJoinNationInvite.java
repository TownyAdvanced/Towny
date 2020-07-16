package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

import java.util.ArrayList;
import java.util.List;

public class TownJoinNationInvite implements Invite {

	private final String directSender;
	private final Town receiver;
	private final Nation sender;

	public TownJoinNationInvite(String directSender, Nation sender, Town receiver) {
		this.directSender = directSender;
		this.sender = sender;
		this.receiver = receiver;
	}
	
	@Override
	public String getDirectSender() {
		return directSender;
	}

	@Override
	public Town getReceiver() {
		return receiver;
	}

	@Override
	public Nation getSender() {
		return sender;
	}

	@Override
	public void accept() throws TownyException {
		Town town = getReceiver();
		List<Town> towns = new ArrayList<>();
		towns.add(town);
		Nation nation = getSender();
		NationCommand.nationAdd(nation, towns);
		// Message handled in nationAdd()
		town.deleteReceivedInvite(this);
		nation.deleteSentInvite(this);
	}

	@Override
	public void decline(boolean fromSender) {
		Town town = getReceiver();
		Nation nation = getSender();
		town.deleteReceivedInvite(this);
		nation.deleteSentInvite(this);
		if (!fromSender) {
			TownyMessaging.sendPrefixedNationMessage(nation, String.format(TownySettings.getLangString("msg_deny_invite"), town.getName()));
		} else {
			TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("nation_revoke_invite"), nation.getName()));
		}
	}
}
