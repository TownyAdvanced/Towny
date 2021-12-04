package com.palmergames.bukkit.towny.event.statusscreen;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;

public class StatusScreenEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private List<String> addedLines = new ArrayList<>();
	private StatusScreen screen;
	
	public StatusScreenEvent(StatusScreen screen) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.screen = screen;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	/**
	 * @return {@link StatusScreen} object which will be shown to the CommandSender.
	 */
	public StatusScreen getStatusScreen() {
		return screen;
	}

 	/**
	 * @return the CommandSender who is going to see the StatusScreen
	 */
	public CommandSender getCommandSender(){
		return screen.getCommandSender();
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
