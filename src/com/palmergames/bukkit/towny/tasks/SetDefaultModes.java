package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.util.BukkitTools;

import java.util.TimerTask;

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
		if (!BukkitTools.isOnline(name))
			return;
		
		//setup default modes
		try {
			TownyUniverse townyUniverse = TownyUniverse.getInstance();
			String modeString = townyUniverse.getPermissionSource().getPlayerPermissionStringNode(name, PermissionNodes.TOWNY_DEFAULT_MODES.getNode());
			if (modeString.isEmpty()) { return; }
			String[] modes = modeString.split(",");
			try {
				townyUniverse.getDataSource().getResident(name).resetModes(modes, notify);
			} catch (NotRegisteredException e) {
				// No resident by this name.
			}
		} catch (NullPointerException ignored) {
			
		}
		

	}

}