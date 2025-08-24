package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.object.TownyWorld;
import java.util.List;

import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.utils.BorderUtil;

import com.palmergames.bukkit.towny.utils.CombatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class TownyWorldListener implements Listener {
	
	private final Towny plugin;

	public TownyWorldListener(Towny instance) {

		plugin = instance;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onWorldLoad(WorldLoadEvent event) {

		newWorld(event.getWorld());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldUnload(WorldUnloadEvent event) {
		if (event.isCancelled())
			return;
		TownyWorld world = TownyUniverse.getInstance().getWorld(event.getWorld().getName());
		if (world == null)
			return;
		TownyUniverse.getInstance().unregisterTownyWorld(world);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onWorldInit(WorldInitEvent event) {

		newWorld(event.getWorld());

	}

	private void newWorld(World world) {
		checkWorlds();
		
		// Check if this world was already loaded by Towny and present in the DB.
		if (TownyUniverse.getInstance().getWorldIDMap().containsKey(world.getUID())) {
			if (TownyUniverse.getInstance().getWorld(world.getUID()).getName().equalsIgnoreCase(world.getName()))
				// This is a world we already know about, the world and UUID are already a match.
				return;

			if (!TownyUniverse.getInstance().getWorldIDMap().get(world.getUID()).getName().equalsIgnoreCase(world.getName())) {
				// The world's UUID is already a TownyWorld uuid map, but the name is outdated.
				TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(world.getUID());
				TownyUniverse.getInstance().getWorldMap().remove(townyWorld.getName());
				TownyUniverse.getInstance().getWorldMap().put(world.getName(), townyWorld);
				townyWorld.setName(world.getName());
				townyWorld.save();
				return;
			}
		}

		// This is a world we've never seen before, make a new TownyWorld.
		TownyUniverse.getInstance().newWorld(world);
		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(world.getUID());
		
		if (townyWorld == null) {
			TownyMessaging.sendErrorMsg("Could not create data for " + world.getName());
			return;
		}
		
		TownyUniverse.getInstance().getDataSource().loadWorld(townyWorld);
		
		if (world.getName().startsWith("DXL_") &&
				Bukkit.getServer().getPluginManager().getPlugin("DungeonsXL") != null) {
			townyWorld.setUsingTowny(false);
			townyWorld.save();
		}
	}

	/**
	 * Attaches UUIDs to worlds that might not have them
	 */
	private void checkWorlds() {
		for (World world : Bukkit.getServer().getWorlds()) {
			TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(world.getName());
			if (townyWorld != null && townyWorld.getUUID() == null) {
				townyWorld.setUUID(world.getUID());
				townyWorld.save();
				
				TownyUniverse.getInstance().getWorldIDMap().putIfAbsent(world.getUID(), townyWorld);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onLightningStrike(LightningStrikeEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		/*
		 * Paper provides a method to get the entity who caused a lightning strike, if the return is non-null then we don't need to add it to the list,
		 * since we can get the player directly in the CombatUtil.
		 */
		if (event.getCause().equals(LightningStrikeEvent.Cause.TRIDENT) && CombatUtil.getLightningCausingEntity(event.getLightning()) != null)
			return;

		final TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getWorld());
		if (townyWorld == null || !townyWorld.isUsingTowny())
			return;

		/*
		 * Add trident-caused lightning strikes to a map temporarily. 
		 */
		if (event.getCause().equals(LightningStrikeEvent.Cause.TRIDENT)) {
			townyWorld.addTridentStrike(event.getLightning().getUniqueId());
			
			plugin.getScheduler().runLater(event.getLightning().getLocation(), () -> townyWorld.removeTridentStrike(event.getLightning().getUniqueId()), 20L);
		}
	}
	
	/**
	 * Protect trees and mushroom growth transforming neighbouring plots which do not share the same owner. 
	 * @param event - StructureGrowEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onStructureGrow(StructureGrowEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getWorld()))
			return;

		// The event Location is always one spot, and although 2x2 trees technically should have 4 locations, 
		// we can trust that the saplings were all placed by one person, or group of people, who were allowed
		// to place them.
		List<BlockState> disallowed = BorderUtil.disallowedBlocks(event.getBlocks(), event.getLocation().getBlock(), event.getPlayer());
		
		if (!disallowed.isEmpty())
			event.getBlocks().removeAll(disallowed);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPortalCreate(PortalCreateEvent event) {
		if (!(event.getReason() == PortalCreateEvent.CreateReason.NETHER_PAIR) ||
			!TownyAPI.getInstance().isTownyWorld(event.getWorld()) ||
			!(event.getEntity() instanceof Player player))
			return;
		
		for (BlockState block : event.getBlocks()) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canBuild(player, block.getLocation(), Material.NETHER_PORTAL)) {
				TownyMessaging.sendErrorMsg(event.getEntity(), Translatable.of("msg_err_you_are_not_allowed_to_create_the_other_side_of_this_portal"));
				event.setCancelled(true);
				break;
			}
		}
	}

}
