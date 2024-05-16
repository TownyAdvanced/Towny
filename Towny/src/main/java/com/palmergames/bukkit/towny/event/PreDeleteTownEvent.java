package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreDeleteTownEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final Town town;
	private final DeleteTownEvent.Cause cause;
	private final CommandSender sender;

	public PreDeleteTownEvent(@NotNull Town town, @NotNull DeleteTownEvent.Cause cause, @Nullable CommandSender sender) {
		this.town = town;
		this.cause = cause;
		this.sender = sender;
	}

	/**
	 * @return the deleted towns name.
	 */
	public String getTownName() {
		return town.getName();
	}

	/**
	 * @return the deleted town object.
	 */
	public Town getTown() {
		return town;
	}

	@NotNull
	public DeleteTownEvent.Cause getCause() {
		return cause;
	}

	/**
	 * @return The command sender who caused the deletion.
	 */
	@Nullable
	public CommandSender getSender() {
		return sender;
	}

	/**
	 * @return The {@link #getSender()} as a resident.
	 */
	@Nullable
	public Resident getSenderResident() {
		return sender instanceof Player player ? TownyAPI.getInstance().getResident(player) : null;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
