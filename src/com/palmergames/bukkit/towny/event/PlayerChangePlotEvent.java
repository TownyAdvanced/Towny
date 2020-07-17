package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Author: Chris H (Zren / Shade)
 * Date: 4/15/12
 */
public class PlayerChangePlotEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	private final Player player;
	private final WorldCoord from;
	private final WorldCoord to;
	private final PlayerMoveEvent moveEvent;
	
	@Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}
	
	public PlayerChangePlotEvent(Player player, WorldCoord from, WorldCoord to, PlayerMoveEvent moveEvent) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.from = from;
		this.to = to;
		this.moveEvent = moveEvent;
	}

	public WorldCoord getFrom() {

		return from;
	}

	public PlayerMoveEvent getMoveEvent() {
		
		return moveEvent;
	}
	
	public WorldCoord getTo() {

		return to;
	}
	
	public Player getPlayer() {
		return player;
	}
}
