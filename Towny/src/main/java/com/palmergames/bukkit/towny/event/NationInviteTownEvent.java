package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.inviteobjects.TownJoinNationInvite;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class NationInviteTownEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final TownJoinNationInvite invite;

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public NationInviteTownEvent(TownJoinNationInvite invite) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.invite = invite;
	}

	/**
	 * @return - Object containing the directsender(Mayor), indirectsender(Town) and receiver of an invite.
	 */
	public TownJoinNationInvite getInvite() {
		return invite;
	}

}