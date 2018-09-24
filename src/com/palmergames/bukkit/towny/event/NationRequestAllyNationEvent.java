package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.inviteobjects.NationAllyNationInvite;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class NationRequestAllyNationEvent extends Event implements Cancellable{

	private static final HandlerList handlers = new HandlerList();
	private NationAllyNationInvite invite;
	public boolean cancelled = false;

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public NationRequestAllyNationEvent(NationAllyNationInvite invite) {
		this.invite = invite;
	}

	/**
	 * @return - Object containing the directsender(Mayor), indirectsender(Town) and receiver of an invite.
	 */
	public NationAllyNationInvite getInvite() {
		return invite;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean b) {
		this.cancelled = b;
	}
}