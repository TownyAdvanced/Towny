package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.TownyInviteReceiver;
import com.palmergames.bukkit.towny.invites.TownyInviteSender;

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
}
