package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Nation;

public class NationAllyNationInvite implements Invite {

	private final String directSender;
	private final Nation receiver;
	private final Nation sender;

	public NationAllyNationInvite(String directSender, Nation sender, Nation receiver) {
		this.directSender = directSender;
		this.sender = sender;
		this.receiver = receiver;
	}

	@Override
	public String getDirectSender() {
		return directSender;
	}

	@Override
	public Nation getReceiver() {
		return receiver;
	}

	@Override
	public Nation getSender() {
		return sender;
	}

	@Override
	public void accept() throws TownyException {
			Nation receiverNation = getReceiver();
			Nation senderNation = getSender();
			
			receiverNation.addAlly(senderNation);
			senderNation.addAlly(receiverNation);
			
			TownyMessaging.sendPrefixedNationMessage(receiverNation, String.format(TownySettings.getLangString("msg_added_ally"), senderNation.getName()));
			TownyMessaging.sendPrefixedNationMessage(senderNation, String.format(TownySettings.getLangString("msg_accept_ally"), receiverNation.getName()));
			
			receiverNation.deleteReceivedInvite(this);
			senderNation.deleteSentAllyInvite(this);
			
			TownyUniverse.getInstance().getDataSource().saveNation(receiverNation);
			TownyUniverse.getInstance().getDataSource().saveNation(senderNation);
	}

	@Override
	public void decline(boolean fromSender) {
		Nation receiverNation = getReceiver();
		Nation senderNation = getSender();
		
		receiverNation.deleteReceivedInvite(this);
		senderNation.deleteSentAllyInvite(this);
		
		if (!fromSender) {
			TownyMessaging.sendPrefixedNationMessage(senderNation, String.format(TownySettings.getLangString("msg_deny_ally"), TownySettings.getLangString("nation_sing") + ": " + receiverNation.getName()));
		} else {
			TownyMessaging.sendPrefixedNationMessage(receiverNation, String.format(TownySettings.getLangString("nation_revoke_ally"), senderNation.getName()));
		}
	}
}
