package com.palmergames.bukkit.towny.event.statusscreen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.Colors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import org.jetbrains.annotations.NotNull;

public class StatusScreenEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private List<Component> addedLines = new ArrayList<>();
	private final StatusScreen screen;
	private final CommandSender receiver;
	
	public StatusScreenEvent(@NotNull StatusScreen screen, @NotNull CommandSender receiver) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.screen = screen;
		this.receiver = receiver;
	}
	
	@Override
	public @NotNull HandlerList getHandlers() {
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
		return this.receiver;
	}

	public boolean hasAdditionalLines() {
 		return !addedLines.isEmpty();
 	}
	
	public List<Component> getAdditionalLines() {
		return addedLines;
	}
	
	public void addLines(@NotNull List<String> lines) {
		addedLines.addAll(lines.stream().map(line -> TownyComponents.miniMessage(Colors.translateColorCodes(line))).collect(Collectors.toList()));
	}
	
	public void addLine(@NotNull String line) {
		addedLines.add(TownyComponents.miniMessage(Colors.translateColorCodes(line)));
	}
	
	public void setLines(@NotNull List<String> lines) {
		addedLines.clear();
		addedLines = lines.stream().map(line -> TownyComponents.miniMessage(Colors.translateColorCodes(line))).collect(Collectors.toList());
	}
	
	public void addLines(@NotNull Collection<Component> lines) {
		addedLines.addAll(lines);
	}
	
	public void addLine(@NotNull Component line) {
		addedLines.add(line);
	}
	
	public void setLines(@NotNull Collection<Component> lines) {
		addedLines.clear();
		addedLines = new ArrayList<>(lines);
	}

}
