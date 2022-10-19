package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

public class TownPreRenameEvent extends CancellableTownyEvent {

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
}
