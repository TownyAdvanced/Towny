package com.palmergames.bukkit.towny.event.statusscreen;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class StatusScreenEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private List<String> addedLines = new ArrayList<>();
	
	public StatusScreenEvent() {
		super(!Bukkit.getServer().isPrimaryThread());
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

 	public boolean hasAdditionalLines() {
 		return !addedLines.isEmpty();
 	}
	
	public List<String> getAdditionalLines() {
		return addedLines;
	}
	
	public void addLines(List<String> lines) {
		for (String line : lines)
			addedLines.add(line);
	}
	
	public void setLines(List<String> lines) {
		addedLines.clear();
		addedLines = lines;
	}

}
