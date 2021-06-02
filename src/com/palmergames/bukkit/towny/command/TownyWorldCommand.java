package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Send a list of all general townyworld help commands to player Command:
 * /townyworld
 */

public class TownyWorldCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;

	private static TownyWorld Globalworld;
	
	private static final List<String> townyWorldTabCompletes = Arrays.asList(
		"list",
		"toggle",
		"set",
		"regen",
		"undo"
	);

	private static final List<String> townyWorldToggleTabCompletes = Arrays.asList(
		"claimable",
		"usingtowny",
		"pvp",
		"forcepvp",
		"explosion",
		"forceexplosion",
		"friendlyfire",
		"fire",
		"forcefire",
		"townmobs",
		"worldmobs",
		"wildernessmobs",
		"revertunclaim",
		"revertentityexpl",
		"revertblockexpl",
		"warallowed",
		"plotcleardelete"
	);
	
	private static List<String> townySetTabCompletes = Arrays.asList(
		"usedefault",
		"wildperm",
		"wildignore",
		"wildregen",
		"wildname"
	);
	
	private boolean isConsole = false;

	public TownyWorldCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (sender instanceof Player) {
			if (plugin.isError()) {
				TownyMessaging.sendMessage(sender, Colors.Rose + "[Towny Error] Locked in Safe mode!");
				return false;
			}
			parseWorldCommand(sender, args);
		} else {
			isConsole = true;			
			parseWorldFromConsole(sender, args);
		}

		Globalworld = null;
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		switch (args[0].toLowerCase()) {
			case "toggle":
				if (args.length == 2)
					return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYWORLD_TOGGLE, townyWorldToggleTabCompletes), args[1]);
				else if (args.length == 3)
					return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
				break;
			case "set":
				if (args.length == 2)
					return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYWORLD_SET, townySetTabCompletes), args[1]);
				else if (args.length > 2 && TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD_SET, args[1]))
					return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD_SET, args[1]).getTabCompletion(args.length-1), args[args.length-1]);
				break;
			default:
				if (args.length == 1)
					return filterByStartOrGetTownyStartingWith(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYWORLD, townyWorldTabCompletes), args[0], "+w");
				else if (args.length > 1 && TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD, args[0]))
					return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD, args[0]).getTabCompletion(args.length), args[args.length-1]);
		}
		
		return Collections.emptyList();
	}

	private void parseWorldFromConsole(CommandSender sender, String[] split) {

		Player player = null;

		if ((split.length == 0) || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.TOWNYWORLD_HELP_CONSOLE.send(sender);
			return;
		}
		
		if (split[0].equalsIgnoreCase("list")){
			listWorlds(player, sender);
			return;
		}		

		if (split[0].equalsIgnoreCase("set")) {
			HelpMenu.TOWNYWORLD_SET_CONSOLE.send(sender);
		}
		else if (split[0].equalsIgnoreCase("regen") || split[0].equalsIgnoreCase("undo") || split[0].equalsIgnoreCase("toggle")) {
			HelpMenu.TOWNYWORLD_HELP_CONSOLE.send(sender);
		} else {
			try {
				Globalworld = TownyUniverse.getInstance().getDataSource().getWorld(split[0].toLowerCase());
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_area_not_recog"));
				return;
			}
			split = StringMgmt.remFirstArg(split);
			parseWorldCommand(sender, split);
		}
	}

	public void parseWorldCommand(CommandSender sender, String[] split) {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		Player player = null;

		if (sender instanceof Player) {
			player = (Player) sender;
			try {
				if (Globalworld == null)
					Globalworld = TownyUniverse.getInstance().getDataSource().getWorld(player.getWorld().getName());
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_area_not_recog"));
				return;
			}
			
		}

		if (split.length == 0) {
			if (player == null) {
				for (String line : TownyFormatter.getStatus(Globalworld)) {
					TownyMessaging.sendMessage(sender, Colors.strip(line));
				}
			} else {
				TownyMessaging.sendMessage(player, TownyFormatter.getStatus(Globalworld));
			}

			return;
		}

		try {

			if (split[0].equalsIgnoreCase("?")) {
				HelpMenu.TOWNYWORLD_HELP.send(sender);
			} else if (split[0].equalsIgnoreCase("list")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_LIST.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				listWorlds(player, sender);

			} else if (split[0].equalsIgnoreCase("set")) {

				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_SET.getNode()))
					throw new TownyException(Translation.of("msg_err_command_disable"));

				worldSet(player, sender, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("toggle")) {

				worldToggle(player, sender, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("regen")) {
				
				TownyMessaging.sendErrorMsg(player, "This command has been removed for 1.13 compatibility, look for its return in the future.");
//				if (isConsole)
//					throw new TownyException("Command cannot be run from console.");
//
//				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_REGEN.getNode()))
//					throw new TownyException(Translation.of("msg_err_command_disable"));
//
//				if (TownyUniverse.isWarTime()) {
//					TownyMessaging.sendErrorMsg(player, Translation.of("msg_war_cannot_do"));
//					return;
//				}
//
//				if (!permSource.isTownyAdmin(player)) {
//					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_admin_only"));
//					return;
//				}
//
//				if (TownySettings.getTownBlockSize() != 16) {
//					TownyMessaging.sendErrorMsg(player, Translation.of("msg_plot_regen_wrong_size"));
//					return;
//				}
//
//				// Regen this chunk
//				if (player != null) {
//					TownyRegenAPI.regenChunk(player);
//				}
//
//			} else if (split[0].equalsIgnoreCase("undo")) {
//				if (isConsole)
//					throw new TownyException("Command cannot be run from console.");
//
//				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_UNDO.getNode()))
//					throw new TownyException(Translation.of("msg_err_command_disable"));
//
//				if (player != null)
//					try {
//						TownyUniverse.getDataSource().getResident(player.getName()).regenUndo();
//					} catch (NotRegisteredException e) {
//						// Failed to get resident
//					}

			} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD, split[0]).execute(sender, "townyworld", split);
			} else {
				TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_invalid_property", "townyworld"));
			}

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	public void listWorlds(Player player, CommandSender sender) {

		if (player == null) {
			TownyMessaging.sendMessage(sender, ChatTools.formatTitle(Translation.of("world_plu")));
		} else
			TownyMessaging.sendMessage(player, ChatTools.formatTitle(Translation.of("world_plu")));

		ArrayList<String> formatedList = new ArrayList<>();
		HashMap<String, Integer> playersPerWorld = BukkitTools.getPlayersPerWorld();
		for (TownyWorld world : TownyUniverse.getInstance().getDataSource().getWorlds()) {
			int numPlayers = playersPerWorld.getOrDefault(world.getName(), 0);
			formatedList.add(Colors.LightBlue + world.getName() + Colors.Blue + " [" + numPlayers + "]" + Colors.White);
		}

		if (player == null) {
			for (String line : ChatTools.list(formatedList)) {
				TownyMessaging.sendMessage(sender, line);
			}
		} else {
			for (String line : ChatTools.list(formatedList)) {
				TownyMessaging.sendMessage(player, line);
			}
		}
	}

	public void worldToggle(Player player, CommandSender sender, String[] split) throws TownyException {
		if (split.length == 0 ) {
			if (!isConsole) {		
				TownyMessaging.sendMessage(player, ChatTools.formatTitle("/TownyWorld toggle"));
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/TownyWorld toggle", "claimable", ""));
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/TownyWorld toggle", "usingtowny", ""));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld toggle", "warallowed", ""));
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/TownyWorld toggle", "pvp/forcepvp", ""));
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/TownyWorld toggle", "friendlyfire", ""));
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/TownyWorld toggle", "explosion/forceexplosion", ""));
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/TownyWorld toggle", "fire/forcefire", ""));
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/TownyWorld toggle", "townmobs/wildernessmobs/worldmobs", ""));
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/TownyWorld toggle", "revertunclaim", ""));
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/TownyWorld toggle", "revertentityexpl/revertblockexpl", ""));
				TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/TownyWorld toggle", "plotcleardelete", ""));
			} else {
				TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/TownyWorld toggle"));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld {world} toggle", "claimable", ""));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld {world} toggle", "usingtowny", ""));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld {world} toggle", "warallowed", ""));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld {world} toggle", "pvp/forcepvp", ""));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld {world} toggle", "friendlyfire", ""));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld {world} toggle", "explosion/forceexplosion", ""));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld {world} toggle", "fire/forcefire", ""));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld {world} toggle", "townmobs/worldmobs", ""));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld {world} toggle", "revertunclaim", ""));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld {world} toggle", "revertentityexpl/revertblockexpl", ""));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/TownyWorld {world} toggle", "plotcleardelete", ""));
			}
		} else {

			if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE.getNode(split[0].toLowerCase())))
				throw new TownyException(Translation.of("msg_err_command_disable"));

			String msg;
			Optional<Boolean> choice = Optional.empty();
			if (split.length == 2) {
				choice = BaseCommand.parseToggleChoice(split[1]);
			}

			if (split[0].equalsIgnoreCase("claimable")) {

				Globalworld.setClaimable(choice.orElse(!Globalworld.isClaimable()));
				msg = Translation.of("msg_set_claim", Globalworld.getName(), Globalworld.isClaimable() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("usingtowny")) {

				Globalworld.setUsingTowny(choice.orElse(!Globalworld.isUsingTowny()));
				plugin.resetCache();
				msg = String.format(Globalworld.isUsingTowny() ? Translation.of("msg_set_use_towny_on") : Translation.of("msg_set_use_towny_off"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);
			
			} else if (split[0].equalsIgnoreCase("warallowed")) {

				Globalworld.setWarAllowed(choice.orElse(!Globalworld.isWarAllowed()));
				plugin.resetCache();
				msg = String.format(Globalworld.isWarAllowed() ? Translation.of("msg_set_war_allowed_on") : Translation.of("msg_set_war_allowed_off"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);
				
			} else if (split[0].equalsIgnoreCase("pvp")) {

				Globalworld.setPVP(choice.orElse(!Globalworld.isPVP()));
				msg = Translation.of("msg_changed_world_setting", "Global PVP", Globalworld.getName(), Globalworld.isPVP() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("forcepvp")) {

				Globalworld.setForcePVP(choice.orElse(!Globalworld.isForcePVP()));
				msg = Translation.of("msg_changed_world_setting", "Force town PVP", Globalworld.getName(), Globalworld.isForcePVP() ? Translation.of("forced") : Translation.of("adjustable"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("friendlyfire")) {

				Globalworld.setFriendlyFire(choice.orElse(!Globalworld.isFriendlyFireEnabled()));
				msg = Translation.of("msg_changed_world_setting", "Friendly Fire", Globalworld.getName(), Globalworld.isFriendlyFireEnabled() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("explosion")) {

				Globalworld.setExpl(choice.orElse(!Globalworld.isExpl()));
				msg = Translation.of("msg_changed_world_setting", "Explosions", Globalworld.getName(), Globalworld.isExpl() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("forceexplosion")) {

				Globalworld.setForceExpl(choice.orElse(!Globalworld.isForceExpl()));
				msg = Translation.of("msg_changed_world_setting", "Force town Explosions", Globalworld.getName(), Globalworld.isForceExpl() ? Translation.of("forced") : Translation.of("adjustable"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("fire")) {

				Globalworld.setFire(choice.orElse(!Globalworld.isFire()));
				msg = Translation.of("msg_changed_world_setting", "Fire Spread", Globalworld.getName(), Globalworld.isFire() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("forcefire")) {

				Globalworld.setForceFire(choice.orElse(!Globalworld.isForceFire()));
				msg = Translation.of("msg_changed_world_setting", "Force town Fire Spread", Globalworld.getName(), Globalworld.isForceFire() ? Translation.of("forced") : Translation.of("adjustable"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("townmobs")) {

				Globalworld.setForceTownMobs(choice.orElse(!Globalworld.isForceTownMobs()));
				msg = Translation.of("msg_changed_world_setting", "Town Mob spawns", Globalworld.getName(), Globalworld.isForceTownMobs() ? Translation.of("forced") : Translation.of("adjustable"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("worldmobs")) {

				Globalworld.setWorldMobs(choice.orElse(!Globalworld.hasWorldMobs()));
				msg = Translation.of("msg_changed_world_setting", "World Mob spawns", Globalworld.getName(), Globalworld.hasWorldMobs() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("wildernessmobs")) {
				
				Globalworld.setWildernessMobs(choice.orElse(!Globalworld.hasWildernessMobs()));
				msg = Translation.of("msg_changed_world_setting", "Wilderness Mob spawns", Globalworld.getName(), Globalworld.hasWildernessMobs() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("revertunclaim")) {

				Globalworld.setUsingPlotManagementRevert(choice.orElse(!Globalworld.isUsingPlotManagementRevert()));

				if (!Globalworld.isUsingPlotManagementRevert()) {
					TownyRegenAPI.removeWorldCoords(Globalworld); // Stop any active snapshots being made.
					TownyRegenAPI.removePlotChunksForWorld(Globalworld, true); // Stop any active reverts being done.
				}
				
				msg = Translation.of("msg_changed_world_setting", "Unclaim Revert", Globalworld.getName(), Globalworld.isUsingPlotManagementRevert() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("revertentityexpl")) {

				Globalworld.setUsingPlotManagementWildEntityRevert(choice.orElse(!Globalworld.isUsingPlotManagementWildEntityRevert()));
				msg = Translation.of("msg_changed_world_setting", "Wilderness Entity Explosion Revert", Globalworld.getName(), Globalworld.isUsingPlotManagementWildEntityRevert() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("revertblockexpl")) {

				Globalworld.setUsingPlotManagementWildBlockRevert(choice.orElse(!Globalworld.isUsingPlotManagementWildBlockRevert()));
				msg = Translation.of("msg_changed_world_setting", "Wilderness Block Explosion Revert", Globalworld.getName(), Globalworld.isUsingPlotManagementWildBlockRevert() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("plotcleardelete")) {

				Globalworld.setUsingPlotManagementMayorDelete(choice.orElse(!Globalworld.isUsingPlotManagementWildBlockRevert()));
				msg = Translation.of("msg_changed_world_setting", "Plot Clear Delete", Globalworld.getName(), Globalworld.isUsingPlotManagementMayorDelete() ? Translation.of("enabled") : Translation.of("disabled"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);
			} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD_TOGGLE, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD_TOGGLE, split[0]).execute(sender, "townyworld", split);
			} else {
				msg = Translation.of("msg_err_invalid_property", "'" + split[0] + "'");
				if (player != null)
					TownyMessaging.sendErrorMsg(player, msg);
				else
					TownyMessaging.sendErrorMsg(msg);
				return;
			}
			
			Globalworld.save();
			
			//Change settings event
			TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(Globalworld);
			Bukkit.getServer().getPluginManager().callEvent(event);
		}

	}

	public void worldSet(Player player, CommandSender sender, String[] split) {

		if (split.length == 0) {
			HelpMenu.TOWNYWORLD_SET.send(sender);
		} else {

			if (split[0].equalsIgnoreCase("usedefault")) {

				Globalworld.setUsingDefault();
				plugin.resetCache();
				if (player != null)
					TownyMessaging.sendMsg(player, Translation.of("msg_usedefault", Globalworld.getName()));
				else
					TownyMessaging.sendMessage(sender, Translation.of("msg_usedefault", Globalworld.getName()));

			} else if (split[0].equalsIgnoreCase("wildperm")) {

				if (split.length < 2) {
					// set default wildperm settings (/tw set wildperm)
					Globalworld.setUsingDefault();
					if (player != null)
						TownyMessaging.sendMsg(player, Translation.of("msg_usedefault", Globalworld.getName()));
					else
						TownyMessaging.sendMessage(sender, Translation.of("msg_usedefault", Globalworld.getName()));
				} else
					try {
						List<String> perms = Arrays.asList(StringMgmt.remFirstArg(split));
						Globalworld.setUnclaimedZoneBuild(perms.contains("build"));
						Globalworld.setUnclaimedZoneDestroy(perms.contains("destroy"));
						Globalworld.setUnclaimedZoneSwitch(perms.contains("switch"));
						Globalworld.setUnclaimedZoneItemUse(perms.contains("itemuse"));

						plugin.resetCache();
						if (player != null)
							TownyMessaging.sendMsg(player, Translation.of("msg_set_wild_perms", Globalworld.getName(), perms.toString()));
						else
							TownyMessaging.sendMessage(sender, Translation.of("msg_set_wild_perms", Globalworld.getName(), perms.toString()));
					} catch (Exception e) {
						if (player != null)
							TownyMessaging.sendErrorMsg(player, "Eg: /townyworld set wildperm build destroy");
						else
							TownyMessaging.sendMessage(sender, "Eg: /townyworld set wildperm build destroy <world>");
					}

			} else if (split[0].equalsIgnoreCase("wildignore")) {

				if (split.length < 2)
					if (player != null)
						TownyMessaging.sendErrorMsg(player, "Eg: /townyworld set wildignore SAPLING,GOLD_ORE,IRON_ORE");
					else
						TownyMessaging.sendMessage(sender, "Eg: /townyworld set wildignore SAPLING,GOLD_ORE,IRON_ORE <world>");
				else
					try {
						List<String> mats = new ArrayList<>();
						for (String s : StringMgmt.remFirstArg(split))
							mats.add(Material.matchMaterial(s.trim().toUpperCase()).name());

						Globalworld.setUnclaimedZoneIgnore(mats);

						plugin.resetCache();
						if (player != null)
							TownyMessaging.sendMsg(player, Translation.of("msg_set_wild_ignore", Globalworld.getName(), Globalworld.getUnclaimedZoneIgnoreMaterials()));
						else
							TownyMessaging.sendMessage(sender, Translation.of("msg_set_wild_ignore", Globalworld.getName(), Globalworld.getUnclaimedZoneIgnoreMaterials()));

					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_input", " on/off."));
					}

			} else if (split[0].equalsIgnoreCase("wildregen")) {

				if (split.length < 2)
					if (player != null)
						TownyMessaging.sendErrorMsg(player, "Eg: /townyworld set wildregen Creeper,EnderCrystal,EnderDragon,Fireball,SmallFireball,LargeFireball,TNTPrimed,ExplosiveMinecart");
					else
						TownyMessaging.sendMessage(sender, "Eg: /townyworld set wildregen Creeper,EnderCrystal,EnderDragon,Fireball,SmallFireball,LargeFireball,TNTPrimed,ExplosiveMinecart <world>");
				else {

					List<String> entities = new ArrayList<>(Arrays.asList(StringMgmt.remFirstArg(split)));

					Globalworld.setPlotManagementWildRevertEntities(entities);

					if (player != null)
						TownyMessaging.sendMsg(player, Translation.of("msg_set_wild_regen", Globalworld.getName(), Globalworld.getPlotManagementWildRevertEntities()));
					else
						TownyMessaging.sendMessage(sender, Translation.of("msg_set_wild_regen", Globalworld.getName(), Globalworld.getPlotManagementWildRevertEntities()));

				}

			} else if (split[0].equalsIgnoreCase("wildname")) {

				if (split.length < 2) {
					if (player != null)
						TownyMessaging.sendErrorMsg(player, "Eg: /townyworld set wildname Wildy");
				} else
					try {
						Globalworld.setUnclaimedZoneName(split[1]);

						if (player != null)
							TownyMessaging.sendMsg(player, Translation.of("msg_set_wild_name", Globalworld.getName(), split[1]));
						else
							TownyMessaging.sendMessage(sender, Translation.of("msg_set_wild_name", Globalworld.getName(), split[1]));
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_input", " on/off."));
					}
			} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD_SET, split[0])) {
				try {
					TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD_SET, split[0]).execute(sender, "townyworld", split);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
			} else {
				if (player != null)
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_invalid_property", "world"));
				return;
			}

			Globalworld.save();
		}
	}

}
