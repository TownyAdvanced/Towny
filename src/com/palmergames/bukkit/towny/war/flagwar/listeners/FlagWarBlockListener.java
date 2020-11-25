package com.palmergames.bukkit.towny.war.flagwar.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import com.palmergames.bukkit.towny.war.flagwar.FlagWarConfig;

public class FlagWarBlockListener implements Listener {
	
	private Towny plugin;
	
	public FlagWarBlockListener(Towny plugin) {	

		this.plugin = plugin;	
	}

	@EventHandler (priority=EventPriority.HIGH)
	public void onFlagWarFlagPlace(TownyBuildEvent event) {
		if (event.getTownBlock() == null)
			return;
		
		if (!(FlagWarConfig.isAllowingAttacks() && event.getMaterial() == FlagWarConfig.getFlagBaseMaterial()))
			return;
		Player player = event.getPlayer();
		Block block = player.getWorld().getBlockAt(event.getLocation());
		WorldCoord worldCoord = new WorldCoord(block.getWorld().getName(), Coord.parseCoord(block));
		
		if (plugin.getCache(player).getStatus() == TownBlockStatus.ENEMY) 
			try {
				if (FlagWar.callAttackCellEvent(plugin, player, block, worldCoord))
					event.setCancelled(false);
			} catch (TownyException e) {
				event.setMessage(e.getMessage());
			}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockBreak(BlockBreakEvent event) {

		FlagWar.checkBlock(event.getPlayer(), event.getBlock(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockBurn(BlockBurnEvent event) {

		FlagWar.checkBlock(null, event.getBlock(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {

		for (Block block : event.getBlocks())
			FlagWar.checkBlock(null, block, event);
	}

	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {

		if (event.isSticky()) {
			
			for (Block block : event.getBlocks())
				FlagWar.checkBlock(null, block, event);
		}

	}
}
