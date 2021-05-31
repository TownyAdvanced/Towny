package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;

import java.util.Set;
import java.util.stream.Collectors;

public class NPCCleanupTask extends Thread {
	
	public NPCCleanupTask() {
		super();
	}
	
	@Override
	public void run() {
		for (Resident resident : TownyUniverse.getInstance().getResidents()) {
			if (resident.isNPC() && !resident.hasTown())
				TownyUniverse.getInstance().getDataSource().removeResident(resident);
		}
	}
}
