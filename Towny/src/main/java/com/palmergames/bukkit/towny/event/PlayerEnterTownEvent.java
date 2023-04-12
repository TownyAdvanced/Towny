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
 * @deprecated since 0.98.4.9 use PlayerEntersIntoTownBorderEvent instead.
 */
@Deprecated
@Warning(reason = "Use the PlayerEntersIntoTownBorderEvent instead")
public class PlayerEnterTownEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final Town enteredtown;
	private final PlayerMoveEvent pme;
	private final WorldCoord from;
	private final WorldCoord to;
	private final Player player;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public PlayerEnterTownEvent(Player player,WorldCoord to, WorldCoord from, Town enteredtown, PlayerMoveEvent pme) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.enteredtown = enteredtown;
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
	 * @deprecated since 0.98.4.4 use {@link #getEnteredTown()} instead.
	 * @return
	 */
	@Deprecated
	public Town getEnteredtown() {
		return getEnteredTown();
	}

	public Town getEnteredTown() {
		return enteredtown;
	}

	public WorldCoord getFrom() {
		return from;
	}

	public WorldCoord getTo() {
		return to;
	}
}
