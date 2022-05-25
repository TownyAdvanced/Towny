package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Alliance;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.command.CommandSender;

public class AllianceNationInvite extends AbstractInvite<Alliance, Nation> {

	public AllianceNationInvite(CommandSender directSender, Nation receiver, Alliance sender) {
		super(directSender, receiver, sender);
	}

	@Override
	public void accept() throws TownyException {
		Nation receiverNation = getReceiver();
		Alliance senderAlliance = getSender();

		receiverNation.setAlliance(senderAlliance);

		TownyMessaging.sendPrefixedAllianceMessage(senderAlliance, Translatable.of("msg_nation_has_joined_alliance", receiverNation.getName()));

		receiverNation.deleteReceivedInvite(this);
		senderAlliance.deleteSentAllianceInvite(this);

		receiverNation.save();
		senderAlliance.save();
	}

	@Override
	public void decline(boolean fromSender) {
		Nation receiverNation = getReceiver();
		Alliance senderAlliance = getSender();
		
		receiverNation.deleteReceivedInvite(this);
		senderAlliance.deleteSentAllianceInvite(this);
		
		if (!fromSender) {
			TownyMessaging.sendPrefixedAllianceMessage(senderAlliance, Translatable.of("msg_deny_ally", Translatable.of("nation_sing").append(": ").append(receiverNation.getName())));
		} else {
			TownyMessaging.sendPrefixedNationMessage(receiverNation, Translatable.of("nation_revoke_ally", senderAlliance.getName()));
		}
	}
}
