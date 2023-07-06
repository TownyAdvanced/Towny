package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Translatable;
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
			
		TownyMessaging.sendPrefixedNationMessage(receiverNation, Translatable.of("msg_added_ally", senderNation.getName()));
		TownyMessaging.sendPrefixedNationMessage(senderNation, Translatable.of("msg_accept_ally", receiverNation.getName()));
			
		receiverNation.deleteReceivedInvite(this);
		senderNation.deleteSentAllyInvite(this);
			
		receiverNation.save();
		senderNation.save();
	}

	@Override
	public void decline(boolean fromSender) {
		Nation receiverNation = getReceiver();
		Nation senderNation = getSender();
		
		receiverNation.deleteReceivedInvite(this);
		senderNation.deleteSentAllyInvite(this);
		
		if (!fromSender) {
			TownyMessaging.sendPrefixedNationMessage(senderNation, Translatable.of("msg_deny_ally", Translatable.of("nation_sing").append(": ").append(receiverNation.getName())));
		} else {
			TownyMessaging.sendPrefixedNationMessage(receiverNation, Translatable.of("nation_revoke_ally", senderNation.getName()));
		}
	}
}
