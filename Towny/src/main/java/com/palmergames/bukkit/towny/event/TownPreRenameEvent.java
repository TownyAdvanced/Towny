package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.Warning;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is no longer called.
 * @deprecated since 0.99.6.4 use {@link com.palmergames.bukkit.towny.event.town.TownPreRenameEvent} instead.
 */
@Deprecated
@Warning(reason = "Event is no longer called. Event has been moved to the com.palmergames.bukkit.towny.event.town package.")
public class TownPreRenameEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final String oldName;
	private final String newName;
	private final Town town;

	public TownPreRenameEvent(Town town, String newName) {
		this.oldName = town.getName();
		this.town = town;
		this.newName = newName;
		setCancelMessage(Translation.of("msg_err_rename_cancelled"));
	}

	/**
	 *
	 * @return the old town name.
	 */
	public String getOldName() {
		return oldName;
	}
	/**
	 * 
	 * @return the new town name.
	 */
	public String getNewName() {
		return newName;
	}

	/**
	 *
	 * @return the town with it's changed name
	 */
	public Town getTown() {
		return this.town;
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
