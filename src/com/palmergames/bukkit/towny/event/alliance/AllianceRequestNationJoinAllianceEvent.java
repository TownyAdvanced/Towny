package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.inviteobjects.AllianceNationInvite;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class AllianceRequestNationJoinAllianceEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final AllianceNationInvite invite;

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public AllianceRequestNationJoinAllianceEvent(AllianceNationInvite invite) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.invite = invite;
	}

	/**
	 * @return - Object containing the directsender(Mayor), indirectsender(Town) and receiver of an invite.
	 */
	public AllianceNationInvite getInvite() {
		return invite;
	}

}