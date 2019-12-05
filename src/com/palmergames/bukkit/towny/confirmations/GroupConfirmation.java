package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.object.PlotGroup;
import org.bukkit.entity.Player;

public class GroupConfirmation {
	private PlotGroup group;
	private Player player;
	
	public GroupConfirmation(PlotGroup group, Player player) {
		this.group = group;
		this.player = player;
	}

	public PlotGroup getGroup() {
		return group;
	}

	public Player getPlayer() {
		return player;
	}
}
