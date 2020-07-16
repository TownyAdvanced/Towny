package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.inviteobjects.PlayerJoinTownInvite;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class TownInvitePlayerEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final PlayerJoinTownInvite invite;

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public TownInvitePlayerEvent(PlayerJoinTownInvite invite) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.invite = invite;
	}

	/**
	 * @return - Object containing the directsender(Mayor), indirectsender(Town) and receiver of an invite.
	 */
	public PlayerJoinTownInvite getInvite() {
		return invite;
	}

}