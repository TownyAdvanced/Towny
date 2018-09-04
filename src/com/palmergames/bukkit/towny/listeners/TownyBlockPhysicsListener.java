package com.palmergames.bukkit.towny.listeners;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.regen.NeedsPlaceholder;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.block.BlockLocation;
import com.palmergames.bukkit.util.BukkitTools;


public class TownyBlockPhysicsListener implements Listener {
	
	private final Towny plugin;

	public TownyBlockPhysicsListener(Towny instance) {

		plugin = instance;
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		/*
		  Exit if the regen timer is zero.
		 */
		if (TownySettings.getRegenDelay() <= 0)
			return;

		//long start = System.currentTimeMillis();

		Block block = event.getBlock();

		if (block == null)
			return;

		BlockLocation blockLocation = new BlockLocation(block.getLocation());

		// if this is a placeholder remove it, as it's no longer needed.
		if (TownyRegenAPI.isPlaceholder(block)) {
			TownyRegenAPI.removePlaceholder(block);
			BukkitTools.setTypeId(block, 0, false);
		}

		if (TownyRegenAPI.hasProtectionRegenTask(blockLocation)) {
			//Cancel any physics events as we will be replacing this block
			event.setCancelled(true);
		} else {
			// Check the block below and cancel the event if that block is going to be replaced.			
			Block blockBelow = block.getRelative(BlockFace.DOWN);
			blockLocation = new BlockLocation(blockBelow.getLocation());

			if (TownyRegenAPI.hasProtectionRegenTask(blockLocation) && (NeedsPlaceholder.contains(block.getType()))) {
				//System.out.print("Cancelling for Below on - " + block.getType().toString());
				event.setCancelled(true);
			}
		}

		//plugin.sendDebugMsg("onBlockPhysics took " + (System.currentTimeMillis() - start) + "ms ("+event.isCancelled() +")");
	}

}
