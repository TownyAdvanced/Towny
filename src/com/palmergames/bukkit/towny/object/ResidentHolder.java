package com.palmergames.bukkit.towny.object;

import java.util.List;

public interface ResidentHolder {

	List<Resident> getResidents();

	boolean hasResident(String name);

	List<Resident> getOutlaws();
}
