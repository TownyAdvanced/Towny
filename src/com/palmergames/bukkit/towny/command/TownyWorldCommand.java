package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.MinecraftTools;
import com.palmergames.util.StringMgmt;

/**
 * Send a list of all general townyworld help commands to player
 * Command: /townyworld
 */

public class TownyWorldCommand implements CommandExecutor  {
	
	private static Towny plugin;
	private static final List<String> townyworld_help = new ArrayList<String>();
	private static final List<String> townyworld_set = new ArrayList<String>();
	private static TownyWorld Globalworld;
	
	public TownyWorldCommand(Towny instance) {
		plugin = instance;
	}		

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		townyworld_help.add(ChatTools.formatTitle("/townyworld"));
		townyworld_help.add(ChatTools.formatCommand("", "/townyworld", "", TownySettings.getLangString("world_help_1")));
		townyworld_help.add(ChatTools.formatCommand("", "/townyworld", TownySettings.getLangString("world_help_2"), TownySettings.getLangString("world_help_3")));
		townyworld_help.add(ChatTools.formatCommand("", "/townyworld", "list", TownySettings.getLangString("world_help_4")));
		townyworld_help.add(ChatTools.formatCommand("", "/townyworld", "toggle",""));
		townyworld_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyworld", "set [] .. []", ""));
		
		townyworld_set.add(ChatTools.formatTitle("/townyworld set"));
		townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "wildname [name]", ""));
		//townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "usingtowny [on/off]", ""));
		
		// if using permissions and it's active disable this command
		if (!plugin.isPermissions()) {
			townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "usedefault", ""));
			townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "wildperm [perm] .. [perm]", "build,destroy,switch,useitem"));
			townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "wildignore [id] [id] [id]", ""));
		}
		
		if (sender instanceof Player) {
			Player player = (Player)sender;
			System.out.println("[PLAYER_COMMAND] " + player.getName() + ": /" + commandLabel + " " + StringMgmt.join(args));
		}
		parseWorldCommand(sender,args);
		/*
		} else {
			// Console
			for (String line : townyworld_help)
				sender.sendMessage(Colors.strip(line));
		}
		*/
		
		townyworld_set.clear();
		townyworld_help.clear();
		Globalworld = null;
		return true;
	}
	
	public void parseWorldCommand(CommandSender sender, String[] split) {
		
		Player player = null;
		
		if (sender instanceof Player) {
			player = (Player)sender;
			try {
				Globalworld = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
			} catch (NotRegisteredException e) {
				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_area_not_recog"));
				return;
			}
		} else {
			if (split.length == 0) {
				sender.sendMessage(String.format(TownySettings.getLangString("msg_err_invalid_property"), "world"));
				return;
			}
			if ((!split[0].equalsIgnoreCase("?")) && (!split[0].equalsIgnoreCase("list")))
			try {
				if ((split.length >= 1)) {
					Globalworld = plugin.getTownyUniverse().getWorld(split[split.length-1].toLowerCase());
					split = StringMgmt.remLastArg(split);
				} else {
					sender.sendMessage(TownySettings.getLangString("msg_area_not_recog"));
					return;
				}

			} catch (NotRegisteredException e) {
				sender.sendMessage(String.format(TownySettings.getLangString("msg_err_invalid_property"), "world"));
				return;
			}
			
		}
		
		if (split.length == 0)
			if (player == null) {
				for (String line : plugin.getTownyUniverse().getStatus(Globalworld))
					sender.sendMessage(Colors.strip(line));
			} else
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(Globalworld));				

		else if (split[0].equalsIgnoreCase("?")) {
			if (player == null) {
				for (String line : townyworld_help)
					sender.sendMessage(line);
			} else
				for (String line : townyworld_help)
					player.sendMessage(line);
		} else if (split[0].equalsIgnoreCase("list")) {
			listWorlds(player, sender);
		} else if (split[0].equalsIgnoreCase("set")) {
			worldSet(player, sender, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase("toggle")) {
			worldToggle(player, sender, StringMgmt.remFirstArg(split));
		} else {
			/*
			try {
				TownyWorld world = plugin.getTownyUniverse().getWorld(split[0]);
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(world));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
			}
			*/
		}
	}
	
	public void listWorlds(Player player, CommandSender sender) {
		if (player == null) {
			sender.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("world_plu")));
		} else
			player.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("world_plu")));
		
		ArrayList<String> formatedList = new ArrayList<String>();
		HashMap<String,Integer> playersPerWorld = MinecraftTools.getPlayersPerWorld(plugin.getServer());
		for (TownyWorld world : plugin.getTownyUniverse().getWorlds()) {
			int numPlayers = playersPerWorld.containsKey(world.getName()) ? playersPerWorld.get(world.getName()) : 0;
			formatedList.add(Colors.LightBlue + world.getName() + Colors.Blue + " [" + numPlayers + "]" + Colors.White);
		}
		
		if (player == null) {
			for (String line : ChatTools.list(formatedList))
				sender.sendMessage(line);
		} else
			for (String line : ChatTools.list(formatedList))
				player.sendMessage(line);
	}
	
	
	public void worldToggle(Player player, CommandSender sender, String[] split) {
		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/TownyWorld toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "claimable", ""));
			player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "usingtowny", ""));
			player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "forcepvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "explosion", ""));
			player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "fire", ""));
			player.sendMessage(ChatTools.formatCommand("", "/TownyWorld toggle", "townmobs/worldmobs", ""));
		} else {

			if ((sender == null) && !plugin.isTownyAdmin(player))
				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_admin_only"));

			String msg;
			
			if (split[0].equalsIgnoreCase("claimable")) {

						Globalworld.setClaimable(!Globalworld.isClaimable());
						msg = String.format(TownySettings.getLangString("msg_set_claim"), Globalworld.getName(), Globalworld.isClaimable() ? "Enabled" : "Disabled");
						if (player != null)
							plugin.sendMsg(player, msg);
						else
							plugin.sendMsg(msg);
								
						
			} else if (split[0].equalsIgnoreCase("usingtowny")) {

					Globalworld.setUsingTowny(!Globalworld.isUsingTowny());
					plugin.updateCache();
					msg = String.format(Globalworld.isUsingTowny() ? TownySettings.getLangString("msg_set_use_towny_on") : TownySettings.getLangString("msg_set_use_towny_off"));
					if (player != null)
						plugin.sendMsg(player, msg);
					else
						plugin.sendMsg(msg);
					
			} else if (split[0].equalsIgnoreCase("pvp")) {

					Globalworld.setPVP(!Globalworld.isPVP());
					msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), "Global PVP" ,Globalworld.getName(), Globalworld.isPVP() ? "Enabled" : "Disabled");
					if (player != null)
						plugin.sendMsg(player, msg);
					else
						plugin.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("forcepvp")) {

					Globalworld.setForcePVP(!Globalworld.isForcePVP());
					msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), "PVP" ,Globalworld.getName(), Globalworld.isForcePVP() ? "Forced" : "Adjustable");
					if (player != null)
						plugin.sendMsg(player, msg);
					else
						plugin.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("explosion")) {

					Globalworld.setForceExpl(!Globalworld.isForceExpl());
					msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), "Explosions", Globalworld.getName(), Globalworld.isForceExpl() ? "Enabled" : "Disabled");
					if (player != null)
						plugin.sendMsg(player, msg);
					else
						plugin.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("fire")) {

					Globalworld.setForceFire(!Globalworld.isForceFire());
					msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), "Fire Spread", Globalworld.getName(), Globalworld.isForceFire() ? "Forced" : "Adjustable");
					if (player != null)
						plugin.sendMsg(player, msg);
					else
						plugin.sendMsg(msg);
				
			} else if (split[0].equalsIgnoreCase("townmobs")) {

					Globalworld.setForceTownMobs(!Globalworld.isForceTownMobs());
					msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), "Town Mob spawns", Globalworld.getName(), Globalworld.isForceTownMobs() ? "Forced" : "Adjustable");
					if (player != null)
						plugin.sendMsg(player, msg);
					else
						plugin.sendMsg(msg);
					
			} else if (split[0].equalsIgnoreCase("worldmobs")) {

					Globalworld.setWorldMobs(!Globalworld.hasWorldMobs());
					msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), "World Mob spawns", Globalworld.getName(), Globalworld.hasWorldMobs() ? "Enabled" : "Disabled");
					if (player != null)
						plugin.sendMsg(player, msg);
					else
						plugin.sendMsg(msg);
					
			} else {
				msg = String.format(TownySettings.getLangString("msg_err_invalid_property"), "'" + split[0] + "'");
				if (player != null)
					plugin.sendErrorMsg(player, msg);
				else
					plugin.sendErrorMsg(msg);
				return;
			}
			
			plugin.getTownyUniverse().getDataSource().saveWorld(Globalworld);
			
		}
		
	}
	
	public void worldSet(Player player, CommandSender sender, String[] split) {
		
		if (split.length == 0) {
			if (player == null) {
				for (String line : townyworld_set)
					sender.sendMessage(line);
			} else {
				for (String line : townyworld_set)
					player.sendMessage(line);
			}
		} else {

			try {
				if (!plugin.isTownyAdmin(player))
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only"));
				//Globalworld = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			if (split[0].equalsIgnoreCase("usedefault")) {
				
				// if using permissions and it's active disable this command
				if (plugin.isPermissions()){
					if (player != null) plugin.sendErrorMsg(player, "Command disabled: Using permissions.");
					return;					
				}
				
				Globalworld.setUsingDefault(true);
				plugin.updateCache();
				if (player != null) 
					plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_usedefault"), Globalworld.getName()));
				else
					sender.sendMessage(String.format(TownySettings.getLangString("msg_usedefault"), Globalworld.getName()));
				
			} else if (split[0].equalsIgnoreCase("wildperm")) {
				
				// if using permissions and it's active disable this command
				if (plugin.isPermissions()){
					plugin.sendErrorMsg(player, "Command disabled: Using permissions.");
					return;					
				}
				
				if (split.length < 2) {
					// set default wildperm settings (/tw set wildperm)
					Globalworld.setUsingDefault(true);
					if (player != null)
						plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_usedefault"), Globalworld.getName()));
					else
						sender.sendMessage(String.format(TownySettings.getLangString("msg_usedefault"), Globalworld.getName()));
				} else
					try {
						List<String> perms = Arrays.asList(StringMgmt.remFirstArg(split));
						Globalworld.setUnclaimedZoneBuild(perms.contains("build"));
						Globalworld.setUnclaimedZoneDestroy(perms.contains("destroy"));
						Globalworld.setUnclaimedZoneSwitch(perms.contains("switch"));
						Globalworld.setUnclaimedZoneItemUse(perms.contains("itemuse"));
						Globalworld.setUsingDefault(false);
						plugin.updateCache();
						if (player != null)
							plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_set_wild_perms"), Globalworld.getName(), perms.toString()));
						else
							sender.sendMessage(String.format(TownySettings.getLangString("msg_set_wild_perms"), Globalworld.getName(), perms.toString()));
					} catch (Exception e) {
						if (player != null)
							plugin.sendErrorMsg(player, "Eg: /townyworld set wildperm build destroy");
						else
							sender.sendMessage("Eg: /townyworld set wildperm build destroy <world>");
					}
			} else if (split[0].equalsIgnoreCase("wildignore")) {
				
				// if using permissions and it's active disable this command
				if (plugin.isPermissions()){
					if (player != null) plugin.sendErrorMsg(player, "Command disabled: Using permissions.");
					return;					
				}
				
				if (split.length < 2)
					if (player != null)
						plugin.sendErrorMsg(player, "Eg: /townyworld set wildignore 11,25,45,67");
					else
						sender.sendMessage("Eg: /townyworld set wildignore 11,25,45,67 <world>");
				else
					try {
						List<Integer> nums = new ArrayList<Integer>();
						for (String s: StringMgmt.remFirstArg(split))
							try {
								nums.add(Integer.parseInt(s));
							} catch (NumberFormatException e) {
							}
						Globalworld.setUnclaimedZoneIgnore(nums);
						Globalworld.setUsingDefault(false);
						plugin.updateCache();
						if (player != null)
							plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_set_wild_ignore"), Globalworld.getName(), Arrays.toString(nums.toArray(new Integer[0]))));
						else
							sender.sendMessage(String.format(TownySettings.getLangString("msg_set_wild_ignore"), Globalworld.getName(), Arrays.toString(nums.toArray(new Integer[0]))));
					} catch (Exception e) {
						plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
					}
			} else if (split[0].equalsIgnoreCase("wildname")) {
				if (split.length < 2) {
					if (player != null) plugin.sendErrorMsg(player, "Eg: /townyworld set wildname Wildy");
				} else
					try {
						Globalworld.setUnclaimedZoneName(split[1]);
						Globalworld.setUsingDefault(false);
						if (player != null)
							plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_set_wild_name"), Globalworld.getName(), split[1]));
						else
							sender.sendMessage(String.format(TownySettings.getLangString("msg_set_wild_name"), Globalworld.getName(), split[1]));
					} catch (Exception e) {
						plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
					}
			} else {
				if (player != null) plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "world"));
				return;
			}

			plugin.getTownyUniverse().getDataSource().saveWorld(Globalworld);
		}
	}
	

}
