package com.palmergames.bukkit.towny.event;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.PlayerCache;
import com.palmergames.bukkit.towny.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.command.TownyCommand;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.util.Colors;


/**
 * Handle events for all Player related events
 * 
 * @author Shade
 * 
 */
public class TownyPlayerListener extends PlayerListener {
	private final Towny plugin;


	public TownyPlayerListener(Towny instance) {
		plugin = instance;
	}
	
	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		try {
			plugin.getTownyUniverse().onLogin(player);
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
	
	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		plugin.getTownyUniverse().onLogout(event.getPlayer());
		
		// Remove from teleport queue (if exists)
		try {
			if (plugin.getTownyUniverse().isTeleportWarmupRunning())
				plugin.getTownyUniverse().abortTeleportRequest(plugin.getTownyUniverse().getResident(event.getPlayer().getName().toLowerCase()));
		} catch (NotRegisteredException e) {
		}
		
		plugin.deleteCache(event.getPlayer());
	}
	
	@Override
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		plugin.sendDebugMsg("onPlayerDeath: " + player.getName());
		if (TownySettings.isTownRespawning())
			try {
				Location respawn = plugin.getTownyUniverse().getTownSpawnLocation(player);
				event.setRespawnLocation(respawn);
			} catch (TownyException e) {
				// Not set will make it default.
			}
	}
	
	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		
		//System.out.println("onPlayerInteract2");
		long start = System.currentTimeMillis();
				
		if (event.isCancelled()) {
			// Fix for bucket bug.
			if (event.getAction() == Action.RIGHT_CLICK_AIR){
				Integer item = event.getPlayer().getItemInHand().getTypeId();
				// block cheats for placing water/lava/fire/lighter use.
				if (item == 326 || item == 327 || item == 259 || (item >= 8 && item <= 11) || item == 51 )
					event.setCancelled(true);
			}		
			return;
		}
		
		Block block = event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
		TownyWorld townyWorld = null;
		
		try {
			townyWorld = plugin.getTownyUniverse().getWorld(block.getLocation().getWorld().getName());
		} catch (NotRegisteredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// prevent players trampling crops
		
		if ((event.getAction() == Action.PHYSICAL) && (townyWorld.isUsingTowny()))
		{
			if ((block.getType() == Material.SOIL) || (block.getType() == Material.CROPS))
				if (townyWorld.isDisablePlayerTrample()) {
					event.setCancelled(true);
					return;
		    }
		}
			
		if(event.hasItem())
		{
			
			if (TownySettings.isItemUseId(event.getItem().getTypeId()))
			{
				//System.out.println("onPlayerInteractEvent: IsItemUseId");
				onPlayerInteractEvent(event, true);
				return;
			}
		}
		// fix for minequest causing null block interactions.
		if (event.getClickedBlock() != null)
			if (TownySettings.isSwitchId(event.getClickedBlock().getTypeId()) || event.getAction() == Action.PHYSICAL)
			{
				//System.out.println("onPlayerInteractEvent: isSwitchId");
				onPlayerSwitchEvent(event, true, null);
				return;
			}
			plugin.sendDebugMsg("onPlayerItemEvent took " + (System.currentTimeMillis() - start) + "ms");
		//}
	}
	
	public void onPlayerInteractEvent(PlayerInteractEvent event, boolean firstCall) {	
		Player player = event.getPlayer();

		//Block block = event.getClickedBlock();
		WorldCoord worldCoord;
		//System.out.println("onPlayerInteractEvent");
		
		try {
			worldCoord = new WorldCoord(plugin.getTownyUniverse().getWorld(player.getWorld().getName()), Coord.parseCoord(player));
		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_configured"));
			event.setCancelled(true);
			return;
		}

		
		// Check cached permissions first
		try {
			PlayerCache cache = plugin.getCache(player);
			cache.updateCoord(worldCoord);
			TownBlockStatus status = cache.getStatus();
			if (status == TownBlockStatus.UNCLAIMED_ZONE && plugin.hasWildOverride(worldCoord.getWorld(), player, event.getItem().getTypeId(), TownyPermission.ActionType.ITEM_USE))
				return;
			if (!cache.getItemUsePermission())
				event.setCancelled(true);
			if (cache.hasBlockErrMsg())
				plugin.sendErrorMsg(player, cache.getBlockErrMsg());
			return;
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old destroy permission was null, update it
				TownBlockStatus status = plugin.cacheStatus(player, worldCoord, plugin.getStatusCache(player, worldCoord));
				plugin.cacheItemUse(player, worldCoord, getItemUsePermission(player, status, worldCoord));
				onPlayerInteractEvent(event, false);
			} else
				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_updating_item_perms"));
		}
	}
	
	public void onPlayerSwitchEvent(PlayerInteractEvent event, boolean firstCall, String errMsg) {
		
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		
		if (!TownySettings.isSwitchId(block.getTypeId()))
			return;

		WorldCoord worldCoord;
		try {
			worldCoord = new WorldCoord(plugin.getTownyUniverse().getWorld(block.getWorld().getName()), Coord.parseCoord(block));
		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_configured"));
			event.setCancelled(true);
			return;
		}

		// Check cached permissions first
		try {
			
			PlayerCache cache = plugin.getCache(player);
			cache.updateCoord(worldCoord);
			TownBlockStatus status = cache.getStatus();
			if (status == TownBlockStatus.UNCLAIMED_ZONE && plugin.hasWildOverride(worldCoord.getWorld(), player, event.getClickedBlock().getTypeId(), TownyPermission.ActionType.SWITCH))
				return;
			if (!cache.getSwitchPermission())
			{
				event.setCancelled(true);
			}
			if (cache.hasBlockErrMsg())
				plugin.sendErrorMsg(player, cache.getBlockErrMsg());
			return;
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old build permission was null, update it
				TownBlockStatus status = plugin.cacheStatus(player, worldCoord, plugin.getStatusCache(player, worldCoord));
				plugin.cacheSwitch(player, worldCoord, getSwitchPermission(player, status, worldCoord));
				onPlayerSwitchEvent(event, false, errMsg);
			} else
				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_updating_switch_perms"));
		}
	}
	
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location from;
		try {
			from = plugin.getCache(player).getLastLocation();
		} catch (NullPointerException e) {
			from = event.getFrom();
		}
		Location to = event.getTo();
		
		if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ())
			return;
		
	   // Prevent fly/double jump cheats
		if (TownySettings.isUsingCheatProtection() && !plugin.hasPermission(player, "towny.cheat.bypass"))
		   if (event.getEventName() != "PLAYER_TELEPORT" && from.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR
                    && !player.isSneaking() && player.getFallDistance() == 0 && player.getVelocity().getY() <= -0.6) {
			   //plugin.sendErrorMsg(player, "Cheat Detected!");
			   
			   Location blockLocation = from;
	
			   //find the first non air block below us
			   while (blockLocation.getBlock().getType() == Material.AIR)
				   blockLocation.setY(blockLocation.getY() - 1);
			   
			   // set to 1 block up so we are not sunk in the ground
			   blockLocation.setY(blockLocation.getY() + 1);
			   
			   plugin.getCache(player).setLastLocation(blockLocation);
			   player.teleport(blockLocation);
			   return;
		   }
		   
	  
		
		try {
			TownyWorld fromWorld = plugin.getTownyUniverse().getWorld(from.getWorld().getName());
			WorldCoord fromCoord = new WorldCoord(fromWorld, Coord.parseCoord(from));
			TownyWorld toWorld = plugin.getTownyUniverse().getWorld(to.getWorld().getName());
			WorldCoord toCoord = new WorldCoord(toWorld, Coord.parseCoord(to));
			if (!fromCoord.equals(toCoord))
				onPlayerMoveChunk(player, fromCoord, toCoord, from, to);
			else {
				//plugin.sendDebugMsg("    From: " + fromCoord);
				//plugin.sendDebugMsg("    To:   " + toCoord);
				//plugin.sendDebugMsg("        " + from.toString());
				//plugin.sendDebugMsg("        " + to.toString());
			}
		} catch (NotRegisteredException e) {
			plugin.sendErrorMsg(player, e.getError());
		}
		
		plugin.getCache(player).setLastLocation(to);
		//plugin.sendDebugMsg("onBlockMove: " + player.getName() + ": ");
		//plugin.sendDebugMsg("        " + from.toString());
		//plugin.sendDebugMsg("        " + to.toString());
	}

	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		onPlayerMove(event);
	}

	public void onPlayerMoveChunk(Player player, WorldCoord from, WorldCoord to, Location fromLoc, Location toLoc) {
		plugin.sendDebugMsg("onPlayerMoveChunk: " + player.getName());
		TownyUniverse universe = plugin.getTownyUniverse();
		
		plugin.getCache(player).setLastLocation(toLoc);
		plugin.getCache(player).updateCoord(to);		
		
		// TODO: Player mode
		if (plugin.hasPlayerMode(player, "townclaim"))
			TownCommand.parseTownClaimCommand(player, new String[]{});
		if (plugin.hasPlayerMode(player, "townunclaim"))
			TownCommand.parseTownUnclaimCommand(player, new String[]{});
		if (plugin.hasPlayerMode(player, "map"))
			TownyCommand.showMap(player);

		
		// claim: attempt to claim area
		// claim remove: remove area from town

		// Check if player has entered a new town/wilderness
		if (to.getWorld().isUsingTowny() && TownySettings.getShowTownNotifications()) {
			boolean fromWild = false, toWild = false, toForSale = false, toHomeBlock = false;
			TownBlock fromTownBlock, toTownBlock = null;
			Town fromTown = null, toTown = null;
			Resident fromResident = null, toResident = null;
			try {
				fromTownBlock = from.getTownBlock();
				try {
					fromTown = fromTownBlock.getTown();
				} catch (NotRegisteredException e) {
				}
				try {
					fromResident = fromTownBlock.getResident();
				} catch (NotRegisteredException e) {
				}
			} catch (NotRegisteredException e) {
				fromWild = true;
			}

			try {
				toTownBlock = to.getTownBlock();
				try {
					toTown = toTownBlock.getTown();
				} catch (NotRegisteredException e) {
				}
				try {
					toResident = toTownBlock.getResident();
				} catch (NotRegisteredException e) {
				}
				
				toForSale = toTownBlock.getPlotPrice() != -1;
				toHomeBlock = toTownBlock.isHomeBlock();
			} catch (NotRegisteredException e) {
				toWild = true;
			}
			
			boolean sendToMsg = false;
			String toMsg = Colors.Gold + " ~ ";

			if (fromWild ^ toWild || !fromWild && !toWild && fromTown != null && toTown != null && fromTown != toTown) {
				sendToMsg = true;
				if (toWild)
					toMsg += Colors.Green + to.getWorld().getUnclaimedZoneName();
				else
					toMsg += universe.getFormatter().getFormattedName(toTown);
			}
			
			if (fromResident != toResident && !toWild) {
				if (!sendToMsg) {
					sendToMsg = true;
                    if(toTownBlock.getType().getId() != 0) toMsg += "[" + toTownBlock.getType().toString() + "] ";
                }
				else {
					toMsg += Colors.LightGray + "  -  ";
                }
                if (toResident != null)
					toMsg += Colors.LightGreen + universe.getFormatter().getFormattedName(toResident);
				else
					toMsg += Colors.LightGreen + TownySettings.getUnclaimedPlotName();
			}
			
			if (toTown != null && (toForSale || toHomeBlock)) {
				if (!sendToMsg) {
					sendToMsg = true;
                    if(toTownBlock.getType().getId() != 0) toMsg += "[" + toTownBlock.getType().toString() + "] ";
                }
				else {
					toMsg += Colors.LightGray + "  -  ";
                }
                if (toHomeBlock)
                toMsg += Colors.LightBlue + "[Home]";
				if (toForSale)
					toMsg += Colors.Yellow + String.format(TownySettings.getLangString("For_Sale"), toTownBlock.getPlotPrice());
			}
			
			if (sendToMsg)
				player.sendMessage(toMsg);
			
			plugin.sendDebugMsg("onPlayerMoveChunk: " + fromWild + " ^ " + toWild + " " + fromTown + " = " + toTown);
		}
	}
	
	public boolean getBuildPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return plugin.getPermission(player, status, pos, TownyPermission.ActionType.BUILD);
	}

	public boolean getDestroyPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return plugin.getPermission(player, status, pos, TownyPermission.ActionType.DESTROY);
	}

	public boolean getSwitchPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return plugin.getPermission(player, status, pos, TownyPermission.ActionType.SWITCH);
	}
	
	public boolean getItemUsePermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return plugin.getPermission(player, status, pos, TownyPermission.ActionType.ITEM_USE);
	}
	

}