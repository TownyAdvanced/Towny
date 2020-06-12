package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Government;

public class GovernmentTagChangeEvent extends TagChangeEvent {
	private final Government government;
	
	public GovernmentTagChangeEvent(String newTag, Government government) {
		super(newTag);
		this.government = government;
	}

<<<<<<< Upstream, based on origin/master
	public Government getGovernment() {
=======
	public Government getTerritory() {
>>>>>>> 75334a7 Cleanup/Territory renameRemove unused imports
		return government;
	}
}
