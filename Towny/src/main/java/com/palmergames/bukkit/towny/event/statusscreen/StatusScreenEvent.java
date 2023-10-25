package com.palmergames.bukkit.towny.event.statusscreen;

import java.util.Collection;
import java.util.List;

import net.kyori.adventure.text.Component;
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

	// String methods

	/**
	 * Adds all lines to the status screen, with unique keys.
	 * <br>
	 * Lines added via this event are added on new lines, in order to add components on the same line use {@link StatusScreen#addComponentOf(String, String)}.
	 * 
	 * @param lines The line(s) to add.
	 * @see #addLine(String, String)
	 * @see StatusScreen#addComponentOf(String, String) 
	 */
	public void addLines(List<String> lines) {
		for (String line : lines)
			addLine(line);
	}
	
	/**
	 * Adds the line to the status screen, with a unique key.
	 * <br>
	 * Lines added via this event are added on new lines, in order to add components on the same line use {@link StatusScreen#addComponentOf(String, String)}.
	 * 
	 * @param line The line to add
	 * @see #addLine(String, String)    
	 * @see StatusScreen#addComponentOf(String, String)    
	 */
	public void addLine(String line) {
		addLine(getNextKey(), line);
	}
	
	/**
	 * Adds the line to the status screen, with a unique key.
	 * <br>
	 * Lines added via this event are added on new lines, in order to add components on the same line use {@link StatusScreen#addComponentOf(String, String)}.
	 * 
	 * @param key The key/name of the component to add.
	 * @param line The line to add.
	 */
	public void addLine(String key, String line) {
		this.screen.addComponentOf(key, "\n" + line);
	}
	
	// Component methods

	/**
	 * Adds all lines to the status screen, with unique keys.
	 * <br>
	 * Lines added via this event are added on new lines, in order to add components on the same line use {@link StatusScreen#addComponentOf(String, Component)}.
	 * 
	 * @param lines The line(s) to add.
	 * @see #addLine(String, Component)
	 * @see StatusScreen#addComponentOf(String, Component)
	 */
	public void addLines(Collection<Component> lines) {
		for (Component line : lines)
			addLine(line);
	}
	
	/**
	 * Adds the line to the status screen, with a unique key.
	 * <br>
	 * Lines added via this event are added on new lines, in order to add components on the same line use {@link StatusScreen#addComponentOf(String, Component)}.
	 * 
	 * @param line The line to add.
	 * @see #addLine(String, Component)
	 * @see StatusScreen#addComponentOf(String, Component)
	 */
	public void addLine(Component line) {
		addLine(getNextKey(), line);
	}

	/**
	 * Adds the line to the status screen, with the specified key
	 * <br>
	 * Lines added via this event are added on new lines, in order to add components on the same line use {@link StatusScreen#addComponentOf(String, Component)}.
	 * 
	 * @param key Key/name of the component to add.
	 * @param line The line to add.
	 * @see StatusScreen#addComponentOf(String, Component)
	 */
	public void addLine(String key, Component line) {
		this.screen.addComponentOf(key, Component.newline().append(line));
	}

	private String getNextKey() {
		return String.format("eventAddedLine-%d", ++addedLineCount);
	}
}
