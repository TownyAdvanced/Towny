package com.palmergames.bukkit.towny.event.statusscreen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.palmergames.bukkit.util.Colors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import org.jetbrains.annotations.NotNull;

public class StatusScreenEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final StatusScreen screen;
	private int addedLineCount = 0;
	
	public StatusScreenEvent(StatusScreen screen) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.screen = screen;
	}
	
	@Override
	@NotNull
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

	@Deprecated
	public boolean hasAdditionalLines() {
 		return false;
 	}
	
	@Deprecated 
	public List<Component> getAdditionalLines() {
		return new ArrayList<>(0);
	}
	
	// String methods
	public void addLines(List<String> lines) {
		for (String line : lines)
			addLine(line);
	}
	
	public void addLine(String line) {
		this.screen.addComponentOf(getNextKey(), LegacyComponentSerializer.legacySection().deserialize(Colors.translateColorCodes(line)));
	}
	
	public void addLine(String key, String line) {
		this.screen.addComponentOf(key, line);
	}
	
	@Deprecated
	public void setLines(List<String> lines) {
		addLines(lines);
	}
	
	// Component methods
	public void addLines(Collection<Component> lines) {
		for (Component line : lines)
			addLine(line);
	}
	
	public void addLine(Component line) {
		this.screen.addComponentOf(getNextKey(), line);
	}

	public void addLine(String key, Component line) {
		this.screen.addComponentOf(key, line);
	}

	private String getNextKey() {
		return String.format("eventAddedLine-%d", ++addedLineCount);
	}
}
