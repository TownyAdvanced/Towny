package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Bukkit;
import org.bukkit.Warning;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * @deprecated since 0.98.4.9 use PlayerExitsFromTownBorderEvent instead.
 */
@Deprecated
@Warning(reason = "Use the PlayerExitsFromTownBorderEvent instead")
public class PlayerLeaveTownEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final Town lefttown;
	private final PlayerMoveEvent pme;
	private final WorldCoord from;
	private final Player player;
	private final WorldCoord to;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public PlayerLeaveTownEvent(Player player, WorldCoord to, WorldCoord from, Town lefttown, PlayerMoveEvent pme) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.lefttown = lefttown;
		this.player = player;
		this.from = from;
		this.pme = pme;
		this.to = to;
	}

	public Player getPlayer() {
		return player;
	}

	public PlayerMoveEvent getPlayerMoveEvent() {
		return pme;
	}

	/**
	 * @deprecated since 0.98.4.5 use {@link #getLeftTown()} instead
	 */
	@Deprecated
	public Town getLefttown() {
	    return getLeftTown();
	}
	
	public Town getLeftTown() {
		return lefttown;
	}

	public WorldCoord getFrom() {
		return from;
	}

	public WorldCoord getTo() {
		return to;
	}
}
