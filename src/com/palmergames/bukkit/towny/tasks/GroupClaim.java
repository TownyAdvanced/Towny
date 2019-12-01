package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.entity.Player;

/**
 * @author Suneet Tipirneni (Siris)
 */
public class GroupClaim extends Thread {
	Towny plugin;
	private volatile Player player;
	private volatile Resident resident;
	private volatile PlotGroup group;
	
	
}
