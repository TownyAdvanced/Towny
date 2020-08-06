package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
<<<<<<< Upstream, based on origin/master
=======
import com.palmergames.bukkit.towny.invites.Invite;
<<<<<<< Upstream, based on origin/master
import com.palmergames.bukkit.towny.invites.InviteReceiver;
import com.palmergames.bukkit.towny.invites.InviteSender;
>>>>>>> bc10475 Refactor things that are useless and/or serve no purpose, and adjust naming.
=======
>>>>>>> 3e8e9e5 town bankruptcy - fixing merge issues
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.command.CommandSender;

<<<<<<< Upstream, based on origin/master
public class NationAllyNationInvite extends AbstractInvite<Nation, Nation> {
=======
public class NationAllyNationInvite implements Invite {

	private final String directSender;
	private final Nation receiver;
	private final Nation sender;
>>>>>>> 3e8e9e5 town bankruptcy - fixing merge issues

<<<<<<< Upstream, based on origin/master
	public NationAllyNationInvite(CommandSender directSender, Nation receiver, Nation sender) {
		super(directSender, receiver, sender);
=======
	public NationAllyNationInvite(String directsender, InviteSender sender, InviteReceiver receiver) {
		this.directsender = directsender;
		this.sender = sender;
		this.receiver = receiver;
	}

<<<<<<< Upstream, based on origin/master
	private String directsender;
	private InviteReceiver receiver;
	private InviteSender sender;

=======
>>>>>>> 3e8e9e5 town bankruptcy - fixing merge issues
	@Override
	public String getDirectSender() {
		return directsender;
	}

	@Override
	public InviteReceiver getReceiver() {
		return receiver;
	}

	@Override
	public InviteSender getSender() {
		return sender;
>>>>>>> bc10475 Refactor things that are useless and/or serve no purpose, and adjust naming.
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
