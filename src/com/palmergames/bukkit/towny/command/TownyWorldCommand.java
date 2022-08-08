package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
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
import java.util.Locale;
import java.util.Optional;

/**
 * Send a list of all general townyworld help commands to player Command:
 * /townyworld
 */

public class TownyWorldCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;

	private static TownyWorld globalWorld;
	
	private static final List<String> townyWorldTabCompletes = Arrays.asList(
		"list",
		"toggle",
		"set"
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
		"unclaimblockdelete",
		"plotcleardelete",
		"wildernessuse"
	);
	
	private static List<String> townySetTabCompletes = Arrays.asList(
		"usedefault",
		"wildperm",
		"wildignore",
		"wildregen",
		"wildname"
	);
	
	public TownyWorldCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (plugin.isError() && sender instanceof Player) {
			TownyMessaging.sendErrorMsg(sender, "Locked in Safe mode!");
			return true;
		}
		
		parseWorldCommand(sender, args);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return tabComplete(sender, args, true);
	}
	
	public List<String> tabComplete(CommandSender sender, String[] args, boolean showWorlds) {
		
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
					return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD_SET, args[1]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);
				break;
			default:
				if (args.length == 1)
					return filterByStartOrGetTownyStartingWith(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYWORLD, townyWorldTabCompletes), args[0], showWorlds ? "+w" : "");
				else if (args.length > 1 && TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD, args[0]))
					return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD, args[0]).getTabCompletion(sender, args), args[args.length-1]);
				else if (showWorlds && BukkitTools.getWorldNames(true).contains(args[0].toLowerCase()))
					return tabComplete(sender, StringMgmt.remFirstArg(args), false);
		}
		
		return Collections.emptyList();
	}

	public void parseWorldCommand(CommandSender sender, String[] split) {

		if (sender instanceof Player player) {
			if (split.length > 0 && !townyWorldTabCompletes.contains(split[0].toLowerCase()) && TownyAPI.getInstance().getTownyWorld(split[0]) != null) {
				globalWorld = TownyAPI.getInstance().getTownyWorld(split[0]);
				split = StringMgmt.remFirstArg(split);
			} else
				globalWorld = TownyAPI.getInstance().getTownyWorld(player.getWorld());
			
			if (globalWorld == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_area_not_recog"));
				return;
			}
		} else {
			if (split.length == 0) {
				HelpMenu.TOWNYWORLD_HELP_CONSOLE.send(sender);
				return;
			}
			
			globalWorld = TownyAPI.getInstance().getTownyWorld(split[0]);
			if (globalWorld == null) {
				Translatable error = Translatable.of("msg_err_invalid_townyworld", split[0]);
				
				if (townyWorldTabCompletes.contains(split[0].toLowerCase()))
					error = Translatable.of("msg_err_enter_world_name_first");
						
				TownyMessaging.sendErrorMsg(sender, error);
				return;
			}
			
			split = StringMgmt.remFirstArg(split);
		}

		if (split.length == 0) {
			TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(globalWorld, sender));
			return;
		}

		try {

			if (split[0].equalsIgnoreCase("?")) {
				if (sender instanceof Player)
					HelpMenu.TOWNYWORLD_HELP.send(sender);
				else
					HelpMenu.TOWNYWORLD_HELP_CONSOLE.send(sender);
			} else if (split[0].equalsIgnoreCase("list")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_LIST.getNode());

				listWorlds(sender);

			} else if (split[0].equalsIgnoreCase("set")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_SET.getNode());

				worldSet(sender, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("toggle")) {

				worldToggle(sender, StringMgmt.remFirstArg(split));
			} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD, split[0]).execute(sender, "townyworld", split);
			} else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", "townyworld"));
			}

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}
	}

	public void listWorlds(CommandSender sender) {
		
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle(Translatable.of("world_plu").forLocale(sender)));

		ArrayList<String> formattedList = new ArrayList<>();
		HashMap<String, Integer> playersPerWorld = BukkitTools.getPlayersPerWorld();
		for (TownyWorld world : TownyUniverse.getInstance().getTownyWorlds()) {
			int numPlayers = playersPerWorld.getOrDefault(world.getName(), 0);
			formattedList.add(Colors.LightBlue + world.getName() + Colors.Blue + " [" + numPlayers + "]" + Colors.White);
		}

		TownyMessaging.sendMessage(sender, ChatTools.list(formattedList));
	}

	public void worldToggle(CommandSender sender, String[] split) throws TownyException {
		if (split.length == 0 ) {
			if (sender instanceof Player)		
				HelpMenu.TOWNYWORLD_TOGGLE.send(sender);
			else
				HelpMenu.TOWNYWORLD_TOGGLE_CONSOLE.send(sender);

		} else {

			if (!globalWorld.isUsingTowny() && !split[0].equalsIgnoreCase("usingtowny"))
				throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

			Optional<Boolean> choice = Optional.empty();
			if (split.length == 2) {
				choice = parseToggleChoice(split[1]);
			}

			if (split[0].equalsIgnoreCase("claimable")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_CLAIMABLE.getNode());
				globalWorld.setClaimable(choice.orElse(!globalWorld.isClaimable()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_set_claim", globalWorld.getName(), formatBool(globalWorld.isClaimable())));

			} else if (split[0].equalsIgnoreCase("usingtowny")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_USINGTOWNY.getNode());
				globalWorld.setUsingTowny(choice.orElse(!globalWorld.isUsingTowny()));
				plugin.resetCache();
				TownyMessaging.sendMsg(sender, globalWorld.isUsingTowny() ? Translatable.of("msg_set_use_towny_on") : Translatable.of("msg_set_use_towny_off"));
				
				// Towny might be getting shut off in a world in order to stop the revert-on-unclaim feature, here we stop any active reverts.
				if (!globalWorld.isUsingTowny() && globalWorld.isUsingPlotManagementRevert())
					TownyRegenAPI.turnOffRevertOnUnclaimForWorld(globalWorld);
			
			} else if (split[0].equalsIgnoreCase("warallowed")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_WARALLOWED.getNode());
				globalWorld.setWarAllowed(choice.orElse(!globalWorld.isWarAllowed()));
				plugin.resetCache();
				TownyMessaging.sendMsg(sender, globalWorld.isWarAllowed() ? Translatable.of("msg_set_war_allowed_on") : Translatable.of("msg_set_war_allowed_off"));
				
			} else if (split[0].equalsIgnoreCase("pvp")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_PVP.getNode());
				globalWorld.setPVP(choice.orElse(!globalWorld.isPVP()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Global PVP", globalWorld.getName(), formatBool(globalWorld.isPVP())));

			} else if (split[0].equalsIgnoreCase("forcepvp")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEPVP.getNode());
				globalWorld.setForcePVP(choice.orElse(!globalWorld.isForcePVP()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Force town PVP", globalWorld.getName(), formatBool(globalWorld.isForcePVP(), "forced", "adjustable")));

			} else if (split[0].equalsIgnoreCase("friendlyfire")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FRIENDLYFIRE.getNode());
				globalWorld.setFriendlyFire(choice.orElse(!globalWorld.isFriendlyFireEnabled()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Friendly Fire", globalWorld.getName(), formatBool(globalWorld.isFriendlyFireEnabled())));

			} else if (split[0].equalsIgnoreCase("explosion")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_EXPLOSION.getNode());
				globalWorld.setExpl(choice.orElse(!globalWorld.isExpl()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Explosions", globalWorld.getName(), formatBool(globalWorld.isExpl())));

			} else if (split[0].equalsIgnoreCase("forceexplosion")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEEXPLOSION.getNode());
				globalWorld.setForceExpl(choice.orElse(!globalWorld.isForceExpl()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Force town Explosions", globalWorld.getName(), formatBool(globalWorld.isForceExpl(), "forced", "adjustable")));

			} else if (split[0].equalsIgnoreCase("fire")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FIRE.getNode());
				globalWorld.setFire(choice.orElse(!globalWorld.isFire()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Fire Spread", globalWorld.getName(), formatBool(globalWorld.isFire())));

			} else if (split[0].equalsIgnoreCase("forcefire")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEFIRE.getNode());
				globalWorld.setForceFire(choice.orElse(!globalWorld.isForceFire()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Force town Fire Spread", globalWorld.getName(), formatBool(globalWorld.isForceFire(), "forced", "adjustable")));

			} else if (split[0].equalsIgnoreCase("townmobs")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_TOWNMOBS.getNode());
				globalWorld.setForceTownMobs(choice.orElse(!globalWorld.isForceTownMobs()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Town Mob spawns", globalWorld.getName(), formatBool(globalWorld.isForceTownMobs(), "forced", "adjustable")));

			} else if (split[0].equalsIgnoreCase("worldmobs")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_WORLDMOBS.getNode());
				globalWorld.setWorldMobs(choice.orElse(!globalWorld.hasWorldMobs()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "World Mob spawns", globalWorld.getName(), formatBool(globalWorld.hasWorldMobs())));

			} else if (split[0].equalsIgnoreCase("wildernessmobs")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_WILDERNESSMOBS.getNode());
				globalWorld.setWildernessMobs(choice.orElse(!globalWorld.hasWildernessMobs()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Wilderness Mob spawns", globalWorld.getName(), formatBool(globalWorld.hasWildernessMobs())));

			} else if (split[0].equalsIgnoreCase("revertunclaim")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_REVERTUNCLAIM.getNode());
				globalWorld.setUsingPlotManagementRevert(choice.orElse(!globalWorld.isUsingPlotManagementRevert()));

				if (!globalWorld.isUsingPlotManagementRevert()) 
					TownyRegenAPI.turnOffRevertOnUnclaimForWorld(globalWorld);
				
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Unclaim Revert", globalWorld.getName(), formatBool(globalWorld.isUsingPlotManagementRevert())));

			} else if (split[0].equalsIgnoreCase("revertentityexpl")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_REVERTENTITYEXPL.getNode());
				globalWorld.setUsingPlotManagementWildEntityRevert(choice.orElse(!globalWorld.isUsingPlotManagementWildEntityRevert()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Wilderness Entity Explosion Revert", globalWorld.getName(), formatBool(globalWorld.isUsingPlotManagementWildEntityRevert())));

			} else if (split[0].equalsIgnoreCase("revertblockexpl")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_REVERTBLOCKEXPL.getNode());
				globalWorld.setUsingPlotManagementWildBlockRevert(choice.orElse(!globalWorld.isUsingPlotManagementWildBlockRevert()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Wilderness Block Explosion Revert", globalWorld.getName(), formatBool(globalWorld.isUsingPlotManagementWildBlockRevert())));

			} else if (split[0].equalsIgnoreCase("plotcleardelete")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_PLOTCLEARDELETE.getNode());
				globalWorld.setUsingPlotManagementMayorDelete(choice.orElse(!globalWorld.isUsingPlotManagementMayorDelete()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Plot Clear Delete", globalWorld.getName(), formatBool(globalWorld.isUsingPlotManagementMayorDelete())));

			} else if (split[0].equalsIgnoreCase("unclaimblockdelete")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_UNCLAIMBLOCKDELETE.getNode());
				globalWorld.setUsingPlotManagementDelete(choice.orElse(!globalWorld.isUsingPlotManagementDelete()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Unclaim Block Delete", globalWorld.getName(), formatBool(globalWorld.isUsingPlotManagementDelete())));

			} else if (split[0].equalsIgnoreCase("wildernessuse")) {

				checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_WILDERNESSUSE.getNode());
				String value = split.length > 1 ? split[1] : "";
				boolean toggle = parseToggleChoice(value).orElse(!globalWorld.getUnclaimedZoneBuild());
				
				globalWorld.setUnclaimedZoneBuild(toggle);
				globalWorld.setUnclaimedZoneDestroy(toggle);
				globalWorld.setUnclaimedZoneItemUse(toggle);
				globalWorld.setUnclaimedZoneSwitch(toggle);
				
				TownyMessaging.sendMsg(sender, Translatable.of("msg_wilderness_use_set_to", toggle, globalWorld.getName()));
			
			} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD_TOGGLE, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD_TOGGLE, split[0]).execute(sender, "townyworld", split);
			} else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", "'" + split[0] + "'"));
				return;
			}
			
			globalWorld.save();
			
			//Change settings event
			TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(globalWorld);
			Bukkit.getServer().getPluginManager().callEvent(event);
		}

	}

	public void worldSet(CommandSender sender, String[] split) {

		if (split.length == 0) {
			HelpMenu.TOWNYWORLD_SET.send(sender);
		} else {

			if (split[0].equalsIgnoreCase("usedefault")) {

				globalWorld.setUsingDefault();
				plugin.resetCache();
				TownyMessaging.sendMsg(sender, Translatable.of("msg_usedefault", globalWorld.getName()));

			} else if (split[0].equalsIgnoreCase("wildperm")) {

				if (split.length < 2) {
					// set default wildperm settings (/tw set wildperm)
					globalWorld.setUsingDefault();
					TownyMessaging.sendMsg(sender, Translatable.of("msg_usedefault", globalWorld.getName()));
				} else
					try {
						List<String> perms = Arrays.asList(String.join(",", StringMgmt.remFirstArg(split)).toLowerCase(Locale.ROOT).split(","));
						globalWorld.setUnclaimedZoneBuild(perms.contains("build"));
						globalWorld.setUnclaimedZoneDestroy(perms.contains("destroy"));
						globalWorld.setUnclaimedZoneSwitch(perms.contains("switch"));
						globalWorld.setUnclaimedZoneItemUse(perms.contains("itemuse") || perms.contains("item_use"));

						plugin.resetCache();
						TownyMessaging.sendMsg(sender, Translatable.of("msg_set_wild_perms", globalWorld.getName(), perms.toString()));
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(sender, "Eg: /townyworld set wildperm build destroy <world>");
					}

			} else if (split[0].equalsIgnoreCase("wildignore")) {

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(sender, "Eg: /townyworld set wildignore SAPLING,GOLD_ORE,IRON_ORE <world>");
				else
					try {
						List<String> mats = new ArrayList<>();
						for (String s : StringMgmt.remFirstArg(split))
							mats.add(Material.matchMaterial(s.trim().toUpperCase(Locale.ROOT)).name());

						globalWorld.setUnclaimedZoneIgnore(mats);

						plugin.resetCache();
						TownyMessaging.sendMsg(sender, Translatable.of("msg_set_wild_ignore", globalWorld.getName(), globalWorld.getUnclaimedZoneIgnoreMaterials()));

					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_input", " on/off."));
					}

			} else if (split[0].equalsIgnoreCase("wildregen")) {

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(sender, "Eg: /townyworld set wildregen Creeper,EnderCrystal,EnderDragon,Fireball,SmallFireball,LargeFireball,TNTPrimed,ExplosiveMinecart");
				else {
					
					String[] entities = String.join(",", StringMgmt.remFirstArg(split)).split(",");
					List<String> entityList = new ArrayList<>(Arrays.asList(entities));

					globalWorld.setPlotManagementWildRevertEntities(entityList);

					TownyMessaging.sendMsg(sender, Translatable.of("msg_set_wild_regen", globalWorld.getName(), globalWorld.getPlotManagementWildRevertEntities()));
				}

			} else if (split[0].equalsIgnoreCase("wildname")) {

				if (split.length < 2) {
					TownyMessaging.sendErrorMsg(sender, "Eg: /townyworld set wildname Wildy");
				} else
					globalWorld.setUnclaimedZoneName(split[1]);
					TownyMessaging.sendMsg(sender, Translatable.of("msg_set_wild_name", globalWorld.getName(), split[1]));
			} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD_SET, split[0])) {
				try {
					TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD_SET, split[0]).execute(sender, "townyworld", split);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(sender, e.getMessage());
				}
			} else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", "world"));
				return;
			}

			globalWorld.save();
		}
	}
	
	private Translatable formatBool(boolean bool) {
		return bool ? Translatable.of("enabled") : Translatable.of("disabled");
	}
	
	private Translatable formatBool(boolean bool, String ifTrue, String ifFalse) {
		return bool ? Translatable.of(ifTrue) : Translatable.of(ifFalse);
	}
}
