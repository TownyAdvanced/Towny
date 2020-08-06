package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.NationCommand;
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
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

<<<<<<< Upstream, based on origin/master
public class TownJoinNationInvite extends AbstractInvite<Nation, Town> {
	
	public TownJoinNationInvite(CommandSender directSender, Town receiver, Nation sender) {
		super(directSender, receiver, sender);
=======
public class TownJoinNationInvite implements Invite {

<<<<<<< Upstream, based on origin/master
	public TownJoinNationInvite(String directsender, InviteSender sender, InviteReceiver receiver) {
		this.directsender = directsender;
=======
	private final String directSender;
	private final Town receiver;
	private final Nation sender;

	public TownJoinNationInvite(String directSender, Nation sender, Town receiver) {
		this.directSender = directSender;
>>>>>>> 3e8e9e5 town bankruptcy - fixing merge issues
		this.sender = sender;
		this.receiver = receiver;
	}

	private String directsender;
	private InviteReceiver receiver;
	private InviteSender sender;

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
			TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_deny_invite", town.getName()));
		} else {
			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("nation_revoke_invite", nation.getName()));
		}
	}
}
