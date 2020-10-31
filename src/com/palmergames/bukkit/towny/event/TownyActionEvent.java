package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Event;

/**
 * 
 * @author LlmDl
 */
public abstract class TownyActionEvent extends Event{

	public abstract void setCancelled(boolean b);

	public abstract void setMessage(String blockErrMsg);

	public abstract boolean isCancelled();

	public abstract String getMessage();
	
}
