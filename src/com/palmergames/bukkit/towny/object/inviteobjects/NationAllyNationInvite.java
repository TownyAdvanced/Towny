package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.NationAcceptAllyRequestEvent;
import com.palmergames.bukkit.towny.event.NationDenyAllyRequestEvent;
import com.palmergames.bukkit.towny.event.NationPreAcceptAllyRequestEvent;
import com.palmergames.bukkit.towny.event.NationPreDenyAllyRequestEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class NationAllyNationInvite extends AbstractInvite<Nation, Nation> {

	public NationAllyNationInvite(CommandSender directSender, Nation receiver, Nation sender) {
		super(directSender, receiver, sender);
	}

	@Override
	public void accept() throws TownyException {
		Nation receiverNation = getReceiver();
		Nation senderNation = getSender();
		
		receiverNation.addAlly(senderNation);
		senderNation.addAlly(receiverNation);
		
		TownyMessaging.sendPrefixedNationMessage(receiverNation, Translation.of("msg_added_ally", senderNation.getName()));
		TownyMessaging.sendPrefixedNationMessage(senderNation, Translation.of("msg_accept_ally", receiverNation.getName()));
			
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
			TownyMessaging.sendPrefixedNationMessage(senderNation, Translation.of("msg_deny_ally", Translation.of("nation_sing") + ": " + receiverNation.getName()));
		} else {
			TownyMessaging.sendPrefixedNationMessage(receiverNation, Translation.of("nation_revoke_ally", senderNation.getName()));
		}
	}
}
