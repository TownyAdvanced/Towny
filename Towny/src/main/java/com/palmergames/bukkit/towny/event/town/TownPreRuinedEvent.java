package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An event which is fired before Towny puts a Town into a ruined status. If
 * this event is cancelled, Towny will move on to deleting the Town.
 * 
 * @author LlmDl
 * @since 0.98.2.6
 */
public class TownPreRuinedEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Town town;
	private final DeleteTownEvent.Cause cause;
	private final CommandSender sender;

	public TownPreRuinedEvent(Town town, DeleteTownEvent.Cause cause, CommandSender sender) {
		this.town = town;
		this.cause = cause;
		this.sender = sender;
		this.setCancelMessage("");
	}

	public Town getTown() {
		return town;
	}

	/**
	 * @return The deletion cause that will put the town into a ruined state.
	 */
	@NotNull
	public DeleteTownEvent.Cause getCause() {
		return cause;
	}

	/**
	 * @return The command sender who is causing the town to go into a ruined state.
	 */
	@Nullable
	public CommandSender getSender() {
		return sender;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
