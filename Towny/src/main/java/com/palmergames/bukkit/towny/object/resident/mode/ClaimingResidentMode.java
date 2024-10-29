package com.palmergames.bukkit.towny.object.resident.mode;

import java.util.HashSet;

import com.palmergames.bukkit.towny.object.Resident;

public class ClaimingResidentMode extends AbstractResidentMode {

	public ClaimingResidentMode(String name, String permissionNode) {
		super(name, permissionNode);
	}

	@Override
	protected void toggle(Resident resident) {
		if (ResidentModeHandler.hasMode(resident, this))
			ResidentModeHandler.removeMode(resident, this);
		else {
			for (AbstractResidentMode mode : new HashSet<>(ResidentModeHandler.getResidentModes(resident))) {
				if (mode instanceof ClaimingResidentMode)
					ResidentModeHandler.removeMode(resident, mode);
			}
			ResidentModeHandler.addMode(resident, this);
		}
	}
}
