package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;

public class TownyObjectFormattedNameEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final TownyObject object;
	private String prefix;
	private String postfix;

	public TownyObjectFormattedNameEvent(TownyObject object, String prefix, String postfix) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.object = object;
		this.prefix = prefix;
		this.postfix = postfix;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getPostfix() {
		return postfix;
	}

	public void setPostfix(String postfix) {
		this.postfix = postfix;
	}

	public TownyObject getTownyObject() {
		return object;
	}

	public boolean isResident() {
		return object instanceof Resident;
	}

	public boolean isTown() {
		return object instanceof Town;
	}

	public boolean isNation() {
		return object instanceof Nation;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
