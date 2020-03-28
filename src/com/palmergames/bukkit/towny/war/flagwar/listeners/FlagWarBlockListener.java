package com.palmergames.bukkit.towny.war.flagwar.listeners;

import org.bukkit.block.Block;
//import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
//import org.bukkit.event.block.BlockPlaceEvent;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;

public class FlagWarBlockListener implements Listener {

	//private Towny plugin;

	public FlagWarBlockListener(Towny plugin) {

		//this.plugin = plugin;
	}

	/* TODO Remove This Commented Code
	 * For Testing purposes only.
	 *
	 *
	 * @EventHandler(priority = EventPriority.LOWEST)
	 * public void onBlockPlace(BlockPlaceEvent event) {
	 * Player player = event.getPlayer();
	 * Block block = event.getBlockPlaced();
	 * 
	 * if (block == null)
	 * return;
	 * 
	 * if (block.getType() == FlagWarConfig.getFlagBaseMaterial()) {
	 * int topY = block.getWorld().getHighestBlockYAt(block.getX(),
	 * block.getZ()) - 1;
	 * if (block.getY() >= topY) {
	 * CellAttackEvent cellAttackEvent = new CellAttackEvent(player, block);
	 * this.plugin.getServer().getPluginManager().callEvent(cellAttackEvent);
	 * if (cellAttackEvent.isCancelled()) {
	 * event.setBuild(false);
	 * event.setCancelled(true);
	 * }
	 * }
	 * }
	 * }
	 */

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

	
	// TODO: Need to check if a immutable block is being moved with a sticky piston.
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {

	}
}
