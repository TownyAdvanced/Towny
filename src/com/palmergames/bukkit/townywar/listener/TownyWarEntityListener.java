package com.palmergames.bukkit.townywar.listener;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;


import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.townywar.TownyWar;

public class TownyWarEntityListener implements Listener {
	//private Towny plugin;
	
	public TownyWarEntityListener(Towny plugin) {
		//this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent event) {
		for (Block block : event.blockList())
			TownyWar.checkBlock(null, block, event);
	}
}
