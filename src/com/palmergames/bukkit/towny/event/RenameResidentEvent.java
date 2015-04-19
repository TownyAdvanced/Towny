package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Resident;

public class RenameResidentEvent extends Event{

	private static final HandlerList handlers = new HandlerList();
	    
	private String oldName;
	private Resident resident;

	@Override
	public HandlerList getHandlers() {
	    	
		return handlers;
	}
	    
	public static HandlerList getHandlerList() {

		return handlers;
	}

	public RenameResidentEvent(String oldName, Resident resident) {
		this.oldName = oldName;
		this.resident = resident;
	}

	/**
	 *
	 * @return the old resident name.
	 */
	public String getOldName() {
		return oldName;
	}
	    
	/**
	 *
	 * @return the town with it's changed name
	 */
	public Resident getResident() {
		return this.resident;
	}
}