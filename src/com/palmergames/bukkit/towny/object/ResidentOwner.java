package com.palmergames.bukkit.towny.object;

import java.util.List;

public interface ResidentOwner {

	List<Resident> getResidents();

	boolean hasResident(String name);

	List<Resident> getOutlaws();
}
