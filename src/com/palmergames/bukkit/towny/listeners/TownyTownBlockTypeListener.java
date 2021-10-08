package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.PlotChangeTypeEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBurnEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.event.damage.TownBlockPVPTestEvent;
import com.palmergames.bukkit.towny.object.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listener for the internal Towny Custom Plot Type API
 *
 * @author gnosii
 */
public class TownyTownBlockTypeListener implements Listener {

	@EventHandler
	public void onTownyCustomTownBlockType(PlotChangeTypeEvent event) {
		if (event.getNewType() == TownBlockType.CUSTOM)
			event.getTownBlock().setPlotPrice(event.getTownBlock().getCustomTownBlockType().getPrice());
	}
	
	@EventHandler
	public void onTownyBuild(TownyBuildEvent event) {
		if (!event.isInWilderness()) {
			TownBlock tb = event.getTownBlock();
			if (tb != null && tb.hasCustomTownBlockType())
				event.setCancelled(!tb.getCustomTypeHandler().onTownyBuild(tb));
		}
	}

	@EventHandler
	public void onTownyDestroy(TownyDestroyEvent event) {
		if (!event.isInWilderness()) {
			TownBlock tb = event.getTownBlock();
			if (tb != null && tb.hasCustomTownBlockType())
				event.setCancelled(!tb.getCustomTypeHandler().onTownyDestroy(tb));
		}
	}

	@EventHandler
	public void onTownySwitch(TownySwitchEvent event) {
		if (!event.isInWilderness()) {
			TownBlock tb = event.getTownBlock();
			if (tb != null && tb.hasCustomTownBlockType())
				event.setCancelled(!tb.getCustomTypeHandler().onTownySwitch(tb));
		}
	}

	@EventHandler
	public void onTownyItemUse(TownyItemuseEvent event) {
		if (!event.isInWilderness()) {
			TownBlock tb = event.getTownBlock();
			if (tb != null && tb.hasCustomTownBlockType())
				event.setCancelled(!tb.getCustomTypeHandler().onTownyItemUse(tb));
		}
	}

	@EventHandler
	public void onTownyBurn(TownyBurnEvent event) {
		if (!event.isInWilderness()) {
			TownBlock tb = event.getTownBlock();
			if (tb != null && tb.hasCustomTownBlockType())
				event.setCancelled(!tb.getCustomTypeHandler().onTownyBurn(tb));
		}
	}

	@EventHandler
	public void onTownyExplosion(TownyExplodingBlocksEvent event) {
		Block testBlock = event.getBlockList().get(0);
		if (!TownyAPI.getInstance().isWilderness(testBlock)) {
			TownBlock tb = TownyAPI.getInstance().getTownBlock(testBlock.getLocation());
			if (tb != null && tb.hasCustomTownBlockType())
				if (!tb.getCustomTypeHandler().onTownyExplosion(tb))
					event.getTownyFilteredBlockList().clear();
		}
	}

	@EventHandler
	public void onTownyDamage(TownBlockPVPTestEvent event) {
		TownBlock tb = event.getTownBlock();
		if (tb != null && !tb.hasCustomTownBlockType())
				event.setPvp(tb.getCustomTypeHandler().onTownyDamage(tb));
	}
}
