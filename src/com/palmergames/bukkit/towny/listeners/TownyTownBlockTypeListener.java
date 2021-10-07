package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.actions.*;
import com.palmergames.bukkit.towny.event.damage.TownyDamageEvent;
import com.palmergames.bukkit.towny.object.TownBlock;
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
	public void onTownyBuild(TownyBuildEvent event) {
		if (!event.isInWilderness()) {
			TownBlock tb = event.getTownBlock();
			if (tb != null && !tb.hasCustomTownBlockType())
				event.setCancelled(!tb.getCustomTypeHandler().onTownyBuild(tb));
		}
	}

	@EventHandler
	public void onTownyDestroy(TownyDestroyEvent event) {
		if (!event.isInWilderness()) {
			TownBlock tb = event.getTownBlock();
			if (tb != null && !tb.hasCustomTownBlockType())
				event.setCancelled(!tb.getCustomTypeHandler().onTownyDestroy(tb));
		}
	}

	@EventHandler
	public void onTownySwitch(TownySwitchEvent event) {
		if (!event.isInWilderness()) {
			TownBlock tb = event.getTownBlock();
			if (tb != null && !tb.hasCustomTownBlockType())
				event.setCancelled(!tb.getCustomTypeHandler().onTownySwitch(tb));
		}
	}

	@EventHandler
	public void onTownyItemUse(TownyItemuseEvent event) {
		if (!event.isInWilderness()) {
			TownBlock tb = event.getTownBlock();
			if (tb != null && !tb.hasCustomTownBlockType())
				event.setCancelled(!tb.getCustomTypeHandler().onTownyItemUse(tb));
		}
	}

	@EventHandler
	public void onTownyBurn(TownyBurnEvent event) {
		if (!event.isInWilderness()) {
			TownBlock tb = event.getTownBlock();
			if (tb != null && !tb.hasCustomTownBlockType())
				event.setCancelled(!tb.getCustomTypeHandler().onTownyBurn(tb));
		}
	}

	@EventHandler
	public void onTownyExplosion(TownyExplodingBlocksEvent event) {
		Block testBlock = event.getBlockList().get(0);
		if (!TownyAPI.getInstance().isWilderness(testBlock)) {
			TownBlock tb = TownyAPI.getInstance().getTownBlock(testBlock.getLocation());
			if (tb != null && !tb.hasCustomTownBlockType())
				if (!tb.getCustomTypeHandler().onTownyExplosion(tb))
					event.getTownyFilteredBlockList().clear();
		}
	}

	@EventHandler
	public void onTownyDamage(TownyDamageEvent event) {
		if (!event.isInWilderness()) {
			TownBlock tb = event.getTownBlock();
			if (tb != null && !tb.hasCustomTownBlockType())
				event.setCancelled(!tb.getCustomTypeHandler().onTownyDamage(tb));
		}
	}
}
