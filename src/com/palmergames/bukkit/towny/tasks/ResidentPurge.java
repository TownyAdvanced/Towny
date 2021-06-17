package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

/**
 * @author ElgarL
 * 
 */
public class ResidentPurge extends Thread {

	final Towny plugin;
	private final CommandSender sender;
	final long deleteTime;
	final boolean townless;

	/**
	 * @param plugin reference to Towny
	 * @param sender reference to CommandSender
	 * @param deleteTime time at which resident is purged (long)
	 * @param townless if resident should be 'Townless'
	 */
	public ResidentPurge(Towny plugin, CommandSender sender, long deleteTime, boolean townless) {

		super();
		this.plugin = plugin;
		this.deleteTime = deleteTime;
		this.setPriority(NORM_PRIORITY);
		this.townless = townless;
		this.sender = sender;
	}

	@Override
	public void run() {

		ResidentUtil.purgeResidents(sender, new ArrayList<>(TownyUniverse.getInstance().getResidents()), deleteTime, townless);
	}

}
