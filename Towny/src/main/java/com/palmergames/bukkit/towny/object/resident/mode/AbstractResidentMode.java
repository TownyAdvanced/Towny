package com.palmergames.bukkit.towny.object.resident.mode;

import com.palmergames.bukkit.towny.object.Resident;

public abstract class AbstractResidentMode {

	final String name;
	final String permissionNode;

	public AbstractResidentMode(String name, String permissionNode) {
		this.name = name;
		this.permissionNode = permissionNode;
	}

	/**
	 * Get the name of the resident mode.
	 *
	 * @return the name of the resident mode.
	 */
	protected String name() {
		return name;
	}

	/**
	 * Get the permission node required to be able to toggle this resident mode.
	 *
	 * @return the permission node required.
	 */
	protected String permissionNode() {
		return permissionNode;
	}

	/**
	 * Toggle the resident mode on or off.
	 *
	 * @param resident Resident to toggle mode on or off for.
	 */
	protected abstract void toggle(Resident resident);
}
