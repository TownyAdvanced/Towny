package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Author: Chris H (Zren / Shade)
 * Date: 4/15/12
 */
public class PlayerChangePlotEvent extends PlayerEvent {

	private static final HandlerList handlers = new HandlerList();
	private WorldCoord from;
	private WorldCoord to;

	public PlayerChangePlotEvent(Player player, WorldCoord from, WorldCoord to) {

		super(player);
		this.from = from;
		this.to = to;
	}

	public WorldCoord getFrom() {

		return from;
	}

	public WorldCoord getTo() {

		return to;
	}

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}
}
