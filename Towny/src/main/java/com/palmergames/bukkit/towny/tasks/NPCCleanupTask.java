package com.palmergames.bukkit.towny.tasks;

import java.util.ArrayList;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;

public class NPCCleanupTask implements Runnable {
	
	public NPCCleanupTask() {
		super();
	}
	
	@Override
	public void run() {
		for (Resident resident : new ArrayList<>(TownyUniverse.getInstance().getResidents())) {
			if (resident.isNPC() && !resident.hasTown())
				TownyUniverse.getInstance().getDataSource().removeResident(resident);
		}
	}
}
