package com.palmergames.bukkit.towny.object.resident.mode;

import com.palmergames.bukkit.towny.object.Resident;

public class GenericResidentMode extends AbstractResidentMode {

	public GenericResidentMode(String name, String permissionNode) {
		super(name, permissionNode);
	}

	@Override
	protected void toggle(Resident resident) {
		if (ResidentModeHandler.hasMode(resident, this))
			ResidentModeHandler.removeMode(resident, this);
		else
			ResidentModeHandler.addMode(resident, this);
	}
}
