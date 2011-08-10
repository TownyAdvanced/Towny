package com.palmergames.bukkit.towny.object;

import java.util.List;

public interface ResidentList {
	public List<Resident> getResidents();
	public boolean hasResident(String name);
}
