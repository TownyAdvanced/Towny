package com.palmergames.bukkit.towny.war.flagwar.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.palmergames.bukkit.towny.war.flagwar.FlagWar;

public class FlagWarEntityListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent event) {

		for (Block block : event.blockList())
			FlagWar.checkBlock(null, block, event);
	}
}
