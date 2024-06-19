package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreDeleteNationEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Nation nation;
	private final DeleteNationEvent.Cause cause;
	private final CommandSender sender;
	
	public PreDeleteNationEvent(@NotNull Nation nation, @NotNull DeleteNationEvent.Cause cause, @Nullable CommandSender sender) {
		this.nation = nation;
		this.cause = cause;
		this.sender = sender;
	}

	/**
	 *
	 * @return the deleted nation name.
	 */
	public String getNationName() {
		return nation.getName();
	}

	/**
	 * @return the deleted nation object.
	 */
	public Nation getNation() {
		return nation;
	}

	@NotNull
	public DeleteNationEvent.Cause getCause() {
		return cause;
	}

	/**
	 * @return The command sender who caused the deletion
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
