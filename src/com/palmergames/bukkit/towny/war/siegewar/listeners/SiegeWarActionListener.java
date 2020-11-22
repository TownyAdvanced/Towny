package com.palmergames.bukkit.towny.war.siegewar.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.actions.TownyBurnEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;

public class SiegeWarActionListener implements Listener {

	@SuppressWarnings("unused")
	private final Towny plugin;
	
	public SiegeWarActionListener(Towny instance) {

		plugin = instance;
	}
	
	@EventHandler
	public void onBlockBreak(TownyDestroyEvent event) {
		if (TownySettings.getWarSiegeEnabled() && SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(event.getBlock())) {
			event.setMessage(Translation.of("msg_err_siege_war_cannot_destroy_siege_banner"));
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onBurn(TownyBurnEvent event) {
		if (TownySettings.getWarSiegeEnabled() && SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(event.getBlock())) {	
			event.setCancelled(true);	
		}
	}
	
	@EventHandler
	public void onBlockExplode(TownyExplodingBlocksEvent event) {
		if (TownySettings.getWarSiegeEnabled()) {
			List<Block> blockList = event.getBlockList();
			List<Block> filteredList = new ArrayList<>();
			for (Block block : blockList) {
				if (!SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(block)) {
					filteredList.add(block);
				}
			}
			event.setBlockList(filteredList);
		}
	}
}
