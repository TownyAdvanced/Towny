package com.palmergames.bukkit.towny.war.eventwar.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.event.TownyLoadedDatabaseEvent;
import com.palmergames.bukkit.towny.event.statusscreen.ResidentStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownBlockStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.event.teleport.OutlawTeleportEvent;
import com.palmergames.bukkit.towny.event.time.NewHourEvent;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.war.eventwar.WarBooks;
import com.palmergames.bukkit.towny.war.eventwar.WarDataBase;
import com.palmergames.bukkit.towny.war.eventwar.WarMetaDataController;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;
import com.palmergames.bukkit.towny.war.eventwar.settings.EventWarSettings;
import com.palmergames.bukkit.util.BookFactory;
import com.palmergames.bukkit.util.Colors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;

public class EventWarTownyListener implements Listener {

	
    @EventHandler
    public void onTownyDatabaseLoad(TownyLoadedDatabaseEvent event) {
    	WarDataBase.loadAll();
    }
    
    @EventHandler
    public void onTownStatus(TownStatusScreenEvent event) {
    	try {
			War war = TownyUniverse.getInstance().getWarEvent(event.getTown());
			if (war == null)
				return;
			event.getStatusScreen().addComponentOf("activeEventwar", Colors.Green + "War: " + Colors.LightGreen + war.getWarName(),
					HoverEvent.showText(Component.text(war.getWarType().name()).append(Component.newline())
							.append(Component.text("Spoils: " + TownyEconomyHandler.getFormattedBalance(war.getWarSpoils())))
							.append(Component.newline())
							.append(Component.text("Delinquents: " + war.getWarParticipants().getTowns()))
							.append(Component.newline())
							.append(Component.text("ID: " + war.getWarUUID()))
							));
		} catch (Exception e) {
			return;
		}
    }
    
    @EventHandler
    public void onTBStatus(TownBlockStatusScreenEvent event) {
    	try {
			War war = TownyUniverse.getInstance().getWarEvent(event.getTownBlock());
			if (war == null)
				return;
			event.getStatusScreen().addComponentOf("activeEventwar", Colors.Green + "War: " + Colors.LightGreen + war.getWarName(),
					HoverEvent.showText(Component.text(war.getWarType().name()).append(Component.newline())
							.append(Component.text("Spoils: " + TownyEconomyHandler.getFormattedBalance(war.getWarSpoils())))
							.append(Component.newline())
							.append(Component.text("Delinquents: " + war.getWarParticipants().getTowns()))
							.append(Component.newline())
							.append(Component.text("ID: " + war.getWarUUID()))
							));
		} catch (Exception e) {
			return;
		}
    }
    
    @EventHandler
    public void onResidentStatus(ResidentStatusScreenEvent event) {
    	try {
			War war = TownyUniverse.getInstance().getWarEvent(event.getResident());
			if (war == null)
				return;
			event.getStatusScreen().addComponentOf("activeEventwar", Colors.Green + "War: " + Colors.LightGreen + war.getWarName(),
					HoverEvent.showText(Component.text(war.getWarType().name()).append(Component.newline())
							.append(Component.text("Spoils: " + TownyEconomyHandler.getFormattedBalance(war.getWarSpoils())))
							.append(Component.newline())
							.append(Component.text("Delinquents: " + war.getWarParticipants().getTowns()))
							.append(Component.newline())
							.append(Component.text("ID: " + war.getWarUUID()))
							));
		} catch (Exception e) {
			return;
		}
    }

	@EventHandler
	public void onNewHourEvent(NewHourEvent event) {
		for (War war : TownyUniverse.getInstance().getWars()) {
			ItemStack book = BookFactory.makeBook(war.getWarName(), "War Continues", WarBooks.warUpdateBook(war));
			war.getWarParticipants().getOnlineWarriors().stream()
				.forEach(res -> res.getPlayer().getInventory().addItem(book));
		}
	}
	
	/**
	 * On each new day, if the nation or town is not peaceful, they receive a WarToken.
	 * @param event NewDayEvent.
	 */
	@EventHandler
	public void onNewDayEvent(NewDayEvent event) {
		if (!EventWarSettings.areWarTokensGivenOnNewDay())
			return;
		
		TownyUniverse.getInstance().getTowns().stream()
			.filter(town -> !town.isNeutral())
			.forEach(town -> {
				WarMetaDataController.incrementTokens(town);
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_war_token_received", town));
			});

		TownyUniverse.getInstance().getNations().stream()
			.filter(nation -> !nation.isNeutral())
			.forEach(nation -> {
				WarMetaDataController.incrementTokens(nation);
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_war_token_received", nation));
			});
	}
	
	
	/**
	 * Prevent outlaws from being teleported away when 
	 * they enter the town they are outlawed in.
	 * 
	 * @param event OutlawTeleportEvent thrown by Towny.
	 */
	@EventHandler
	public void onOutlawTeleport(OutlawTeleportEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;
		
		if (event.getTown().hasActiveWar()
			&& event.getOutlaw().hasTown()
			&& event.getOutlaw().getTownOrNull().hasActiveWar())
			event.setCancelled(true);
	}
	

}
