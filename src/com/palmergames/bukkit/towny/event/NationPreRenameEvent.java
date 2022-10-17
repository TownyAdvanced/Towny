package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Translation;

public class NationPreRenameEvent extends CancellableTownyEvent {

	private final String oldName;
	private final String newName;
	private final Nation nation;

	public NationPreRenameEvent(Nation nation, String newName) {
		this.oldName = nation.getName();
		this.nation = nation;
		this.newName = newName;
		this.setCancelMessage(Translation.of("msg_err_rename_cancelled"));
	}

	/**
	 *
	 * @return the old nation name.
	 */
	public String getOldName() {
		return oldName;
	}
	/**
	 * 
	 * @return the new nation name.
	 */
	public String getNewName() {
		return newName;
	}

	/**
	 *
	 * @return the nation with it's changed name
	 */
	public Nation getNation() {
		return this.nation;
	}
}
