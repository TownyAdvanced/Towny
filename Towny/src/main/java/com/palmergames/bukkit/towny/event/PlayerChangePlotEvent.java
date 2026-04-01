package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.TownyUniverse;

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
	private boolean showPlotNotifications;
	
	@Override
    public HandlerList getHandlers() {

        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}
	
	public PlayerChangePlotEvent(Player player, WorldCoord from, WorldCoord to) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.from = from;
		this.to = to;
		this.showPlotNotifications = TownySettings.getShowTownNotifications() && TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_RECEIVES_PLOT_NOTIFICATIONS.getNode());
	}

	public WorldCoord getFrom() {

		return from;
	}

	/**
	 * @deprecated This event no longer includes the delegate PlayerMoveEvent. Use {@link #getFrom()} and {@link #getTo()} instead.
	 * @throws UnsupportedOperationException always, do not call.
	 */
	@Deprecated(since = "0.102.0.13", forRemoval = true)
	public PlayerMoveEvent getMoveEvent() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("This event no longer includes the delegate PlayerMoveEvent.");
	}
	
	public WorldCoord getTo() {
		return to;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public TownyWorld getTownyWorldFrom() {
		return from.getTownyWorld();
	}
	
	public TownyWorld getTownyWorldTo() {
		return to.getTownyWorld();
	}

	/**
	 * @return will Towny show plot notifications to the player?
	 */
	public boolean isShowingPlotNotifications() {
		return showPlotNotifications;
	}

	/**
	 * @param showPlotNotifications determines if Towny will show plot notifications to the player.
	 */
	public void setShowingPlotNotifications(boolean showPlotNotifications) {
		this.showPlotNotifications = showPlotNotifications;
	}
}
