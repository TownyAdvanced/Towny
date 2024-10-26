package com.palmergames.bukkit.towny.object.resident.mode;

import com.palmergames.bukkit.towny.object.Resident;

public abstract class AbstractResidentMode {

	String name;
	String permissionNode;

	public AbstractResidentMode(String name, String permissionNode) {
		this.name = name;
		this.permissionNode = permissionNode;
	}

	protected String name() {
		return name;
	}

	protected String permissionNode() {
		return permissionNode;
	}

	protected abstract void toggle(Resident resident);
}
