package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
<<<<<<< Upstream, based on origin/master
=======
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteReceiver;
import com.palmergames.bukkit.towny.invites.InviteSender;
>>>>>>> bc10475 Refactor things that are useless and/or serve no purpose, and adjust naming.
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.command.CommandSender;

public class PlayerJoinTownInvite extends AbstractInvite<Town, Resident> {

<<<<<<< Upstream, based on origin/master
	public PlayerJoinTownInvite(CommandSender directSender, Resident receiver, Town sender) {
		super(directSender, receiver, sender);
=======
	public PlayerJoinTownInvite(String directSender, InviteSender sender, InviteReceiver receiver) {
		this.directSender = directSender;
		this.sender = sender;
		this.receiver = receiver;
>>>>>>> bc10475 Refactor things that are useless and/or serve no purpose, and adjust naming.
	}

<<<<<<< Upstream, based on origin/master
=======
	private String directSender;
	private InviteReceiver receiver;
	private InviteSender sender;

	@Override
	public String getDirectSender() {
		return directSender;
	}

	@Override
	public InviteReceiver getReceiver() {
		return receiver;
	}

	@Override
	public InviteSender getSender() {
		return sender;
	}
	
>>>>>>> bc10475 Refactor things that are useless and/or serve no purpose, and adjust naming.
	@Override
	public void accept() throws TownyException {
		Resident resident = getReceiver();
		Town town = getSender();
		
		TownCommand.townAddResident(town, resident);
		TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_join_town", resident.getName()));
		
		resident.deleteReceivedInvite(this);
		town.deleteSentInvite(this);
	}

	@Override
	public void decline(boolean fromSender) {
		Resident resident = getReceiver();
		Town town = getSender();
		
		resident.deleteReceivedInvite(this);
		town.deleteSentInvite(this);
		
		if (!fromSender) {
			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_deny_invite", resident.getName()));
			TownyMessaging.sendMsg(getReceiver(), Translation.of("successful_deny"));
		} else {
			TownyMessaging.sendMsg(resident, Translation.of("town_revoke_invite", town.getName()));
		}
	}
}
