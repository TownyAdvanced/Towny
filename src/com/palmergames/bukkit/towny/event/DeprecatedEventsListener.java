package com.palmergames.bukkit.towny.event;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.util.BukkitTools;

@SuppressWarnings("deprecation")
public class DeprecatedEventsListener implements Listener {

	@EventHandler
	public void onNationSpawn(com.palmergames.bukkit.towny.event.teleport.NationSpawnEvent event) {
		BukkitTools.getPluginManager().callEvent(new com.palmergames.bukkit.towny.event.NationSpawnEvent(event.getPlayer(), event.getFrom(), event.getTo()));
	}
	
	@EventHandler
	public void onTownSpawn(com.palmergames.bukkit.towny.event.teleport.TownSpawnEvent event) {
		BukkitTools.getPluginManager().callEvent(new com.palmergames.bukkit.towny.event.TownSpawnEvent(event.getPlayer(), event.getFrom(), event.getTo()));
	}
	
	@EventHandler
	public void onBankTransaction(com.palmergames.bukkit.towny.event.economy.BankTransactionEvent event) {
		BukkitTools.getPluginManager().callEvent(new com.palmergames.bukkit.towny.event.BankTransactionEvent(event.getAccount(), event.getTransaction()));
	}
	
	@EventHandler
	public void onTownTransaction(com.palmergames.bukkit.towny.event.economy.TownTransactionEvent event) {
		BukkitTools.getPluginManager().callEvent(new com.palmergames.bukkit.towny.event.TownTransactionEvent(event.getTown(), event.getTransaction()));
	}

	@EventHandler
	public void onNationTransaction(com.palmergames.bukkit.towny.event.economy.NationTransactionEvent event) {
		BukkitTools.getPluginManager().callEvent(new com.palmergames.bukkit.towny.event.NationTransactionEvent(event.getNation(), event.getTransaction()));
	}

	@EventHandler
	public void onTownyTransaction(com.palmergames.bukkit.towny.event.economy.TownyTransactionEvent event) {
		BukkitTools.getPluginManager().callEvent(new com.palmergames.bukkit.towny.event.TownyTransactionEvent(event.getTransaction()));
	}
}
