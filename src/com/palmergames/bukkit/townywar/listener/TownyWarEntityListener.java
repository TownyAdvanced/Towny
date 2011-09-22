package com.palmergames.bukkit.townywar.listener;

import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;


import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.townywar.TownyWar;

public class TownyWarEntityListener extends EntityListener {
	//private Towny plugin;
	
	public TownyWarEntityListener(Towny plugin) {
		//this.plugin = plugin;
	}
	
	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		for (Block block : event.blockList())
			TownyWar.checkBlock(null, block, event);
	}
}
