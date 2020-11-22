package com.palmergames.bukkit.towny.war.siegewar.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
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
		if (TownySettings.getWarSiegeEnabled()) {
			if (SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(Bukkit.getWorld(event.getLocation().getWorld().getName()).getBlockAt(event.getLocation()))) {
				event.setMessage(Translation.of("msg_err_siege_war_cannot_destroy_siege_banner"));
				event.setCancelled(true);
			}
		}
	}
	
	
}
