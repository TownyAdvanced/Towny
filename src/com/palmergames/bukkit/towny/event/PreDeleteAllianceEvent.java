package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Alliance;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PreDeleteAllianceEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	
	private final String allianceName;
	private final Alliance alliance;
	private boolean isCancelled = false;
	
	public PreDeleteAllianceEvent(Alliance alliance) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.alliance = alliance;
		this.allianceName = alliance.getName();
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		isCancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	/**
	 *
	 * @return the deleted alliance name.
	 */
	public String getAllianceName() {
		return allianceName;
	}

	/**
	 * @return the deleted alliance object.
	 */
	public Alliance getAlliance() {
		return alliance;
	}
}
