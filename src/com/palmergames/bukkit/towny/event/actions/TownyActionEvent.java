package com.palmergames.bukkit.towny.event.actions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.TownBlock;

/**
 * Used by the TownyBuildEvent, TownyDestroyEvent, TownySwitchEvent
 * and TownyItemuseEvent. Part of the API which lets Towny's war and
 * other plugins modify Towny's plot-permission-decision outcomes.
 * 
 * @author LlmDl
 */
public abstract class TownyActionEvent extends CancellableTownyEvent {
	protected final Player player;
	protected final Location loc;
	protected final Material mat;
	protected final TownBlock townblock;
	protected boolean suppressMessage;

	public TownyActionEvent(Player player, Location loc, Material mat, TownBlock townblock, boolean cancelled) {
		this.player = player;
		this.loc = loc;
		this.mat = mat;
		this.townblock = townblock;
		this.suppressMessage = false;
		setCancelled(cancelled);
	}

	public boolean isMessageSupressed() {
		return getCancelMessage() == "";
	}

	public void supressMessage(boolean suppressMessage) {
		setCancelMessage("");
	}

	/**
	 * @return Material of the block being built.
	 */
	public Material getMaterial() {
		return mat;
	}

	/**
	 * @return Location of the block being built.
	 */
	public Location getLocation() {
		return loc;
	}

	/**
	 * @return player involved in the build event.
	 */
	public Player getPlayer() {
		return player;
	}
	
	/**
	 * The {@link com.palmergames.bukkit.towny.object.TownBlock} this action occured in,
	 * or null if in the wilderness.
	 * @return TownBlock or null. 
	 */
	@Nullable
	public TownBlock getTownBlock() {
		return townblock;
	}

	/**
	 * Did this action occur in the wilderness?
	 * 
	 * @return return true if this was in the wilderness.
	 */
	public boolean isInWilderness() {
		return townblock == null;
	}
	
	/**
	 * Did this action occur inside of a town's townblock?
	 * 
	 * @return true if this has a townblock.
	 */
	public boolean hasTownBlock() {
		return townblock != null;
	}

	/**
	 * @return cancellation message shown to players when their build attempt is cancelled or null.
	 * @deprecated since 0.98.4.0, use {@link #getCancelMessage()} instead.
	 */
	@Deprecated
	public String getMessage() {
		return getCancelMessage();
	}

	/**
	 * @param message Message shown to players when their build attempts is cancelled.
	 * @deprecated since 0.98.4.0 use {@link #setCancelMessage(String)}	instead.
	 */
	@Deprecated
	public void setMessage(String message) {
		if (message.equals(""))
			this.supressMessage(true);
		else
			setCancelMessage(message);
	}
	
}
