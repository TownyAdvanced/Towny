package com.palmergames.bukkit.towny.tasks;

import java.util.Collection;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.CellBorder;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.DrawSmokeTaskFactory;

public class DrawSmokeTask extends TownyTimerTask{

	public DrawSmokeTask(Towny plugin) {

		super(plugin);
	}

	@Override
	public void run() {
		
		Collection<? extends Player> players = BukkitTools.getOnlinePlayers();
		
		for (Player player: players) {
			if (plugin.hasPlayerMode(player, "constantplotborder")) {
				WorldCoord wc = new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player.getLocation()));
				CellBorder cellBorder = BorderUtil.getPlotBorder(wc);
				cellBorder.runBorderedOnSurface(1, 2, DrawSmokeTaskFactory.sendToPlayer(player));
			}
		}
	}	
}
