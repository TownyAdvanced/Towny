package com.palmergames.bukkit.towny.tasks;

import java.util.Arrays;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;

/**
 * @author ElgarL
 *
 */
public class SetDefaultModes extends TownyTimerTask {
	protected Towny plugin;
	protected Player player;
	protected boolean notify;

	public SetDefaultModes(TownyUniverse universe, Player player, boolean notify) {
		super(universe);
		this.plugin = universe.getPlugin();
		this.player = player;
		this.notify = notify;
	}

	@Override
	public void run() {
		
		// Is the player still available
		if (!Arrays.asList(universe.getOnlinePlayers()).contains(this.player))
			return;
		
		//setup default modes
        String[] modes = TownyUniverse.getPermissionSource().getPlayerPermissionStringNode(player.getName(), PermissionNodes.TOWNY_DEFAULT_MODES.getNode()).split(",");
        plugin.setPlayerMode(player, modes, notify);
	}

}