package ca.xshade.bukkit.towny.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.TownyException;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.object.TownyWorld;
import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;
import ca.xshade.bukkit.util.MinecraftTools;
import ca.xshade.util.StringMgmt;

/**
 * Send a list of all general townyworld help commands to player
 * Command: /townyworld
 */

public class TownyWorldCommand implements CommandExecutor  {
	
	private static Towny plugin;
	private static final List<String> townyworld_help = new ArrayList<String>();
	private static final List<String> townyworld_set = new ArrayList<String>();
	
	public TownyWorldCommand(Towny instance) {
		plugin = instance;
	}		

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		townyworld_help.add(ChatTools.formatTitle("/townyworld"));
		townyworld_help.add(ChatTools.formatCommand("", "/townyworld", "", TownySettings.getLangString("world_help_1")));
		townyworld_help.add(ChatTools.formatCommand("", "/townyworld", TownySettings.getLangString("world_help_2"), TownySettings.getLangString("world_help_3")));
		townyworld_help.add(ChatTools.formatCommand("", "/townyworld", "list", TownySettings.getLangString("world_help_4")));
		townyworld_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyworld", "set [] .. []", ""));
		
		townyworld_set.add(ChatTools.formatTitle("/townyworld set"));
		townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "claimable [on/off]", ""));
		townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "pvp [on/off]", ""));
		townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "wildname [name]", ""));
		townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "usingtowny [on/off]", ""));
		
		// if using permissions and it's active disable this command
		if (!plugin.isPermissions()) {
			townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "usedefault", ""));
			townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "wildperm [perm] .. [perm]", "build,destroy,switch,useitem"));
			townyworld_set.add(ChatTools.formatCommand("", "/townyworld set", "wildignore [id] [id] [id]", ""));
		}
		
		if (sender instanceof Player) {
			Player player = (Player)sender;
			parseWorldCommand(player,args);
		} else {
			// Console
			for (String line : townyworld_help)
				sender.sendMessage(Colors.strip(line));
		}
				
		townyworld_help.clear();
		return true;
	}
	
	public void parseWorldCommand(Player player, String[] split) {
		if (split.length == 0)
			try {
				TownyWorld world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(world));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_area_not_recog"));
			}
		else if (split[0].equalsIgnoreCase("?"))
			for (String line : townyworld_help)
				player.sendMessage(line);
		else if (split[0].equalsIgnoreCase("list"))
			listWorlds(player);
		else if (split[0].equalsIgnoreCase("set"))
			worldSet(player, StringMgmt.remFirstArg(split));
		else
			try {
				TownyWorld world = plugin.getTownyUniverse().getWorld(split[0]);
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(world));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
			}
	}
	
	public void listWorlds(Player player) {
		player.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("world_plu")));
		ArrayList<String> formatedList = new ArrayList<String>();
		HashMap<String,Integer> playersPerWorld = MinecraftTools.getPlayersPerWorld(plugin.getServer());
		for (TownyWorld world : plugin.getTownyUniverse().getWorlds()) {
			int numPlayers = playersPerWorld.containsKey(world.getName()) ? playersPerWorld.get(world.getName()) : 0;
			formatedList.add(Colors.LightBlue + world.getName() + Colors.Blue + " [" + numPlayers + "]" + Colors.White);
		}
		for (String line : ChatTools.list(formatedList))
			player.sendMessage(line);
	}
	
	public void worldSet(Player player, String[] split) {
		
		if (split.length == 0) {
			for (String line : townyworld_set)
				player.sendMessage(line);
		} else {
			TownyWorld world;
			if (!plugin.isTownyAdmin(player)) {
			}
			try {
				if (!plugin.isTownyAdmin(player))
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only"));
				world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			if (split[0].equalsIgnoreCase("claimable")) {
				
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /townyworld set claimable on");
				else
					try {
						boolean choice = parseOnOff(split[1]);
						world.setClaimable(choice);
						plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_set_claim"), world.getName(), split[1]));
					} catch (Exception e) {
						plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
					}
			} else if (split[0].equalsIgnoreCase("pvp")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /townyworld set pvp off");
				else
					try {
						boolean choice = parseOnOff(split[1]);
						world.setPvP(choice);
						plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_set_pvp"), world.getName(), split[1]));
					} catch (Exception e) {
						plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
					}
			} else if (split[0].equalsIgnoreCase("usedefault")) {
				
				// if using permissions and it's active disable this command
				if (plugin.isPermissions()){
					plugin.sendErrorMsg(player, "Command disabled: Using permissions.");
					return;					
				}
				
				world.setUsingDefault(true);
				plugin.updateCache();
				plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_usedefault"), world.getName()));
			} else if (split[0].equalsIgnoreCase("wildperm")) {
				
				// if using permissions and it's active disable this command
				if (plugin.isPermissions()){
					plugin.sendErrorMsg(player, "Command disabled: Using permissions.");
					return;					
				}
				
				if (split.length < 2) {
					// set default wildperm settings (/tw set wildperm)
					world.setUsingDefault(true);
					plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_usedefault"), world.getName()));
				} else
					try {
						List<String> perms = Arrays.asList(StringMgmt.remFirstArg(split));
						world.setUnclaimedZoneBuild(perms.contains("build"));
						world.setUnclaimedZoneDestroy(perms.contains("destroy"));
						world.setUnclaimedZoneSwitch(perms.contains("switch"));
						world.setUnclaimedZoneItemUse(perms.contains("itemuse"));
						world.setUsingDefault(false);
						plugin.updateCache();
						plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_set_wild_perms"), world.getName(), perms.toString()));
					} catch (Exception e) {
						plugin.sendErrorMsg(player, "Eg: /townyworld set wildperm build destroy");
					}
			} else if (split[0].equalsIgnoreCase("wildignore")) {
				
				// if using permissions and it's active disable this command
				if (plugin.isPermissions()){
					plugin.sendErrorMsg(player, "Command disabled: Using permissions.");
					return;					
				}
				
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /townyworld set wildignore 11,25,45,67");
				else
					try {
						List<Integer> nums = new ArrayList<Integer>();
						for (String s: StringMgmt.remFirstArg(split))
							try {
								nums.add(Integer.parseInt(s));
							} catch (NumberFormatException e) {
							}
						world.setUnclaimedZoneIgnore(nums);
						world.setUsingDefault(false);
						plugin.updateCache();
						plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_set_wild_ignore"), world.getName(), Arrays.toString(nums.toArray(new Integer[0]))));
					} catch (Exception e) {
						plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
					}
			} else if (split[0].equalsIgnoreCase("wildname")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /townyworld set wildname Wildy");
				else
					try {
						world.setUnclaimedZoneName(split[1]);
						world.setUsingDefault(false);
						plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_set_wild_name"), world.getName(), split[1]));
					} catch (Exception e) {
						plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
					}
			} else if (split[0].equalsIgnoreCase("usingtowny")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /townyworld set usingtowny off");
				else
					try {
						boolean choice = parseOnOff(split[1]);
						world.setUsingTowny(choice);
						plugin.updateCache();
						if (world.isUsingTowny())
							plugin.sendMsg(player, TownySettings.getLangString("msg_set_use_towny_on"));
						else
							plugin.sendMsg(player, TownySettings.getLangString("msg_set_use_towny_off"));
					} catch (Exception e) {
						plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
					}
			} else {
				plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "world"));
				return;
			}

			plugin.getTownyUniverse().getDataSource().saveWorld(world);
		}
	}
	
	private boolean parseOnOff(String s) throws Exception {
		if (s.equalsIgnoreCase("on"))
			return true;
		else if (s.equalsIgnoreCase("off"))
			return false;
		else
			throw new Exception(String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
	}

}
