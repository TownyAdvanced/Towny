package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.actions.*;
import com.palmergames.bukkit.towny.event.damage.TownyDamageEvent;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listener for the internal Towny Custom Plot Type API
 * @author gnosii
 */
public class TownyTypeListener implements Listener {
	
	@EventHandler
	public void onTownyBuild(TownyBuildEvent event) {
		TownBlock tb = event.getTownBlock();
		if (tb.hasCustomTownBlockType())
			event.setCancelled(!tb.getCustomTypeHandler().onTownyBuild(tb));
	}

	@EventHandler
	public void onTownyDestroy(TownyDestroyEvent event) {
		TownBlock tb = event.getTownBlock();
		if (tb.hasCustomTownBlockType())
			event.setCancelled(!tb.getCustomTypeHandler().onTownyDestroy(tb));
	}

	@EventHandler
	public void onTownySwitch(TownySwitchEvent event) {
		TownBlock tb = event.getTownBlock();
		if (tb.hasCustomTownBlockType())
			event.setCancelled(!tb.getCustomTypeHandler().onTownySwitch(tb));
	}

	@EventHandler
	public void onTownyItemUse(TownyItemuseEvent event) {
		TownBlock tb = event.getTownBlock();
		if (tb.hasCustomTownBlockType())
			event.setCancelled(!tb.getCustomTypeHandler().onTownyItemUse(tb));
	}

	@EventHandler
	public void onTownyBurn(TownyBurnEvent event) {
		TownBlock tb = event.getTownBlock();
		if (tb.hasCustomTownBlockType())
			event.setCancelled(!tb.getCustomTypeHandler().onTownyBurn(tb));
	}

	@EventHandler
	public void onTownyExplosion(TownyExplodingBlocksEvent event) {
		TownBlock tb = TownyAPI.getInstance().getTownBlock(event.getBlockList().get(0).getLocation());
		if (tb != null && tb.hasCustomTownBlockType())
			if (!tb.getCustomTypeHandler().onTownyExplosion(tb))
				event.getTownyFilteredBlockList().clear();
			else
				return;
	}
	
	@EventHandler
	public void onTownyDamage(TownyDamageEvent event) {
		TownBlock tb = event.getTownBlock();
		if (tb.hasCustomTownBlockType())
			event.setCancelled(!tb.getCustomTypeHandler().onTownyDamage(tb));
	}
}
