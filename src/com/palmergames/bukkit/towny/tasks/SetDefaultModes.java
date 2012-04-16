package com.palmergames.bukkit.towny.tasks;

import java.util.TimerTask;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;

/**
 * @author ElgarL
 * 
 */
public class SetDefaultModes extends TimerTask {

	protected String name;
	protected boolean notify;

	public SetDefaultModes(String name, boolean notify) {

		this.name = name;
		this.notify = notify;
	}

	@Override
	public void run() {

		// Is the player still available
		if (!TownyUniverse.getPlugin().isOnline(name))
			return;

		//setup default modes
		String[] modes = TownyUniverse.getPermissionSource().getPlayerPermissionStringNode(name, PermissionNodes.TOWNY_DEFAULT_MODES.getNode()).split(",");
		try {
			TownyUniverse.getDataSource().getResident(name).setModes(modes, notify);
		} catch (NotRegisteredException e) {
			// No resident by this name.
		}
	}

}