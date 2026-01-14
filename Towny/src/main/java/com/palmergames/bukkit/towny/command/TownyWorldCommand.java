package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.NoPermissionException;
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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TownyWorldCommand extends BaseCommand implements CommandExecutor {

	private final Towny plugin;

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
		"unclaimentitydelete",
		"plotcleardelete",
		"wildernessuse",
		"jailing"
	);
	
	private static final List<String> townySetTabCompletes = Arrays.asList(
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
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String commandLabel, String[] args) {
		if (plugin.isError() && sender instanceof Player) {
			TownyMessaging.sendErrorMsg(sender, "Locked in Safe mode!");
			return true;
		}

		try {
			parseWorldCommand(sender, args);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return tabComplete(sender, args, true);
	}
	
	public List<String> tabComplete(CommandSender sender, String[] args, boolean showWorlds) {
		
		switch (args[0].toLowerCase(Locale.ROOT)) {
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
				else if (showWorlds && BukkitTools.getWorldNames(true).contains(args[0].toLowerCase(Locale.ROOT)))
					return tabComplete(sender, StringMgmt.remFirstArg(args), false);
		}
		
		return Collections.emptyList();
	}

	public void parseWorldCommand(CommandSender sender, String[] split) throws TownyException {

		TownyWorld world;
		if (!(sender instanceof Player player)) {
			if (split.length == 0) {
				HelpMenu.TOWNYWORLD_HELP_CONSOLE.send(sender);
				return;
			}

			world = TownyAPI.getInstance().getTownyWorld(split[0]);
			if (world == null) {
				Translatable error = Translatable.of("msg_err_invalid_townyworld", split[0]);

				if (townyWorldTabCompletes.contains(split[0].toLowerCase(Locale.ROOT)))
					error = Translatable.of("msg_err_enter_world_name_first");

				throw new TownyException(error);
			}
			
			split = StringMgmt.remFirstArg(split);
		} else { // We have a Player using the /tw command.
			if (split.length > 0 && !townyWorldTabCompletes.contains(split[0].toLowerCase(Locale.ROOT)) && TownyAPI.getInstance().getTownyWorld(split[0]) != null) {
				world = TownyAPI.getInstance().getTownyWorld(split[0]);
				split = StringMgmt.remFirstArg(split);
			} else
				world = TownyAPI.getInstance().getTownyWorld(player.getWorld());

			if (world == null)
				throw new TownyException(Translatable.of("msg_area_not_recog"));
		}

		if (split.length == 0) {
			TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(world, sender));
			return;
		}

		if (split[0].equalsIgnoreCase("?")) {
			if (sender instanceof Player)
				HelpMenu.TOWNYWORLD_HELP.send(sender);
			else
				HelpMenu.TOWNYWORLD_HELP_CONSOLE.send(sender);
			return;
		}
		
		String[] subArgs = StringMgmt.remFirstArg(split);
		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "list" -> listWorlds(sender);
		case "set" -> worldSet(sender, world, subArgs);
		case "toggle" -> worldToggle(sender, world, subArgs);
		default -> {
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD, split[0]).execute(sender, "townyworld", split);
				return;
			}
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", "townyworld"));
		}}

	}

	public void listWorlds(CommandSender sender) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_LIST.getNode());
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle(Translatable.of("world_plu").forLocale(sender)));

		ArrayList<String> formattedList = new ArrayList<>();
		HashMap<String, Integer> playersPerWorld = BukkitTools.getPlayersPerWorld();
		for (TownyWorld world : TownyUniverse.getInstance().getTownyWorlds()) {
			int numPlayers = playersPerWorld.getOrDefault(world.getName(), 0);
			formattedList.add(Colors.AQUA + world.getName() + Colors.DARK_AQUA + " [" + numPlayers + "]" + Colors.WHITE);
		}

		TownyMessaging.sendMessage(sender, ChatTools.list(formattedList));
	}

	public void worldToggle(CommandSender sender, TownyWorld world, String[] split) throws TownyException {
		if (split.length == 0 ) {
			if (sender instanceof Player)
				HelpMenu.TOWNYWORLD_TOGGLE.send(sender);
			else
				HelpMenu.TOWNYWORLD_TOGGLE_CONSOLE.send(sender);
			return;
		}

		if (!world.isUsingTowny() && !split[0].equalsIgnoreCase("usingtowny"))
			throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

		Optional<Boolean> choice = Optional.empty();
		if (split.length == 2) {
			choice = parseToggleChoice(split[1]);
		}

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "claimable" -> toggleClaimable(sender, world, choice);
		case "usingtowny" -> toggleUsingTowny(sender, world, choice);
		case "warallowed" -> toggleWarAllowed(sender, world, choice);
		case "pvp" -> togglePVP(sender, world, choice);
		case "forcepvp" -> toggleForcePVP(sender, world, choice);
		case "friendlyfire" -> toggleFriendlyFire(sender, world, choice);
		case "explosion" -> toggleExplosion(sender, world, choice);
		case "forceexplosion" -> toggleForceExplosion(sender, world, choice);
		case "fire" -> toggleFire(sender, world, choice);
		case "forcefire" -> toggleForceFire(sender, world, choice);
		case "townmobs" -> toggleTownMobs(sender, world, choice);
		case "worldmobs" -> toggleWorldMobs(sender, world, choice);
		case "wildernessmobs" -> toggleWildernessMobs(sender, world, choice);
		case "revertunclaim" -> toggleRevertUnclaim(sender, world, choice);
		case "revertentityexpl" -> toggleRevertEntityExpl(sender, world, choice);
		case "revertblockexpl" -> toggleRevertBlockExpl(sender, world, choice);
		case "plotcleardelete" -> togglePlotClearDelete(sender, world, choice);
		case "unclaimblockdelete" -> toggleUnclaimBlockDelete(sender, world, choice);
		case "unclaimentitydelete" -> toggleUnclaimEntityDelete(sender, world, choice);
		case "wildernessuse" -> toggleWildernessUse(sender, world, split);
		case "jailing" -> toggleJailing(sender, world, choice);
		default -> {
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD_TOGGLE, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD_TOGGLE, split[0]).execute(sender, "townyworld", split);
			} else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", "'" + split[0] + "'"));
				return;
			}
		}}

		world.save();

		//Change settings event
		BukkitTools.fireEvent(new TownBlockSettingsChangedEvent(world));
	}

	private void toggleClaimable(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_CLAIMABLE.getNode());
		world.setClaimable(choice.orElse(!world.isClaimable()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_set_claim", world.getName(), formatBool(world.isClaimable())));
	}

	private void toggleUsingTowny(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_USINGTOWNY.getNode());
		world.setUsingTowny(choice.orElse(!world.isUsingTowny()));
		plugin.resetCache();
		TownyMessaging.sendMsg(sender, world.isUsingTowny() ? Translatable.of("msg_set_use_towny_on") : Translatable.of("msg_set_use_towny_off"));
		
		// Towny might be getting shut off in a world in order to stop the revert-on-unclaim feature, here we stop any active reverts.
		if (!world.isUsingTowny() && world.isUsingPlotManagementRevert())
			TownyRegenAPI.turnOffRevertOnUnclaimForWorld(world);
	}

	private void toggleWarAllowed(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_WARALLOWED.getNode());
		world.setWarAllowed(choice.orElse(!world.isWarAllowed()));
		plugin.resetCache();
		TownyMessaging.sendMsg(sender, world.isWarAllowed() ? Translatable.of("msg_set_war_allowed_on") : Translatable.of("msg_set_war_allowed_off"));
	}

	private void togglePVP(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_PVP.getNode());
		world.setPVP(choice.orElse(!world.isPVP()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Global PVP", world.getName(), formatBool(world.isPVP())));
	}

	private void toggleForcePVP(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEPVP.getNode());
		world.setForcePVP(choice.orElse(!world.isForcePVP()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Force town PVP", world.getName(), formatBool(world.isForcePVP(), "forced", "adjustable")));
	}

	private void toggleFriendlyFire(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FRIENDLYFIRE.getNode());
		world.setFriendlyFire(choice.orElse(!world.isFriendlyFireEnabled()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Friendly Fire", world.getName(), formatBool(world.isFriendlyFireEnabled())));
	}

	private void toggleExplosion(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_EXPLOSION.getNode());
		world.setExpl(choice.orElse(!world.isExpl()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Explosions", world.getName(), formatBool(world.isExpl())));
	}

	private void toggleForceExplosion(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEEXPLOSION.getNode());
		world.setForceExpl(choice.orElse(!world.isForceExpl()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Force town Explosions", world.getName(), formatBool(world.isForceExpl(), "forced", "adjustable")));
	}

	private void toggleFire(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FIRE.getNode());
		world.setFire(choice.orElse(!world.isFire()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Fire Spread", world.getName(), formatBool(world.isFire())));
	}

	private void toggleForceFire(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_FORCEFIRE.getNode());
		world.setForceFire(choice.orElse(!world.isForceFire()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Force town Fire Spread", world.getName(), formatBool(world.isForceFire(), "forced", "adjustable")));
	}

	private void toggleTownMobs(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_TOWNMOBS.getNode());
		world.setForceTownMobs(choice.orElse(!world.isForceTownMobs()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Town Mob spawns", world.getName(), formatBool(world.isForceTownMobs(), "forced", "adjustable")));
	}

	private void toggleWorldMobs(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_WORLDMOBS.getNode());
		world.setWorldMobs(choice.orElse(!world.hasWorldMobs()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "World Mob spawns", world.getName(), formatBool(world.hasWorldMobs())));
	}

	private void toggleWildernessMobs(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_WILDERNESSMOBS.getNode());
		world.setWildernessMobs(choice.orElse(!world.hasWildernessMobs()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Wilderness Mob spawns", world.getName(), formatBool(world.hasWildernessMobs())));
	}

	private void toggleRevertUnclaim(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_REVERTUNCLAIM.getNode());
		world.setUsingPlotManagementRevert(choice.orElse(!world.isUsingPlotManagementRevert()));
		if (!world.isUsingPlotManagementRevert()) 
			TownyRegenAPI.turnOffRevertOnUnclaimForWorld(world);
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Unclaim Revert", world.getName(), formatBool(world.isUsingPlotManagementRevert())));
	}

	private void toggleRevertEntityExpl(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_REVERTENTITYEXPL.getNode());
		world.setUsingPlotManagementWildEntityRevert(choice.orElse(!world.isUsingPlotManagementWildEntityRevert()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Wilderness Entity Explosion Revert", world.getName(), formatBool(world.isUsingPlotManagementWildEntityRevert())));
	}

	private void toggleRevertBlockExpl(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_REVERTBLOCKEXPL.getNode());
		world.setUsingPlotManagementWildBlockRevert(choice.orElse(!world.isUsingPlotManagementWildBlockRevert()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Wilderness Block Explosion Revert", world.getName(), formatBool(world.isUsingPlotManagementWildBlockRevert())));
	}

	private void togglePlotClearDelete(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_PLOTCLEARDELETE.getNode());
		world.setUsingPlotManagementMayorDelete(choice.orElse(!world.isUsingPlotManagementMayorDelete()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Plot Clear Delete", world.getName(), formatBool(world.isUsingPlotManagementMayorDelete())));
	}

	private void toggleUnclaimBlockDelete(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_UNCLAIMBLOCKDELETE.getNode());
		world.setUsingPlotManagementDelete(choice.orElse(!world.isUsingPlotManagementDelete()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Unclaim Block Delete", world.getName(), formatBool(world.isUsingPlotManagementDelete())));
	}

	private void toggleUnclaimEntityDelete(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_UNCLAIMENTITYDELETE.getNode());
		world.setDeletingEntitiesOnUnclaim(choice.orElse(!world.isDeletingEntitiesOnUnclaim()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Unclaim Entity Delete", world.getName(), formatBool(world.isDeletingEntitiesOnUnclaim())));
	}

	private void toggleWildernessUse(CommandSender sender, TownyWorld world, String[] split) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_WILDERNESSUSE.getNode());
		String value = split.length > 1 ? split[1] : "";
		boolean toggle = parseToggleChoice(value).orElse(!world.getUnclaimedZoneBuild());
		
		world.setUnclaimedZoneBuild(toggle);
		world.setUnclaimedZoneDestroy(toggle);
		world.setUnclaimedZoneItemUse(toggle);
		world.setUnclaimedZoneSwitch(toggle);
		
		TownyMessaging.sendMsg(sender, Translatable.of("msg_wilderness_use_set_to", toggle, world.getName()));
	}

	private void toggleJailing(CommandSender sender, TownyWorld world, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE_JAILING.getNode());
		world.setJailingEnabled(choice.orElse(!world.isJailingEnabled()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_world_setting", "Jailing", world.getName(), formatBool(world.isJailingEnabled())));
	}

	public void worldSet(CommandSender sender, TownyWorld world, String[] split) throws NoPermissionException {

		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_SET.getNode());

		if (split.length == 0) {
			HelpMenu.TOWNYWORLD_SET.send(sender);
			return;
		}

		switch(split[0].toLowerCase(Locale.ROOT)) {
		case "usedefault" -> setUseDefaults(sender, world);
		case "wildperm" -> setWildPerm(sender, world, split);
		case "wildignore" -> setWildIgnore(sender, world, split);
		case "wildregen" -> setWildRegen(sender, world, split);
		case "wildname" -> setWildName(sender, world, split);
		default -> {
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYWORLD_SET, split[0])) {
				try {
					TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYWORLD_SET, split[0]).execute(sender, "townyworld", split);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(sender, e.getMessage());
				}
			} else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", "world"));
				return;
			}
		}
		}

		world.save();
	}

	private void setUseDefaults(CommandSender sender, TownyWorld world) {
		Confirmation.runOnAccept(() -> {
			world.setUsingDefault();
			world.save();
			plugin.resetCache();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_usedefault", world.getName()));
		})
		.setTitle(Translatable.of("confirmation_are_you_sure_you_want_to_reset_this_worlds_settings"))
		.sendTo(sender);
	}

	private void setWildPerm(CommandSender sender, TownyWorld world, String[] split) {
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /townyworld set wildperm build destroy <world>");
			return;
		}
		try {
			List<String> perms = Arrays.asList(String.join(",", StringMgmt.remFirstArg(split)).toLowerCase(Locale.ROOT).split(","));
			world.setUnclaimedZoneBuild(perms.contains("build"));
			world.setUnclaimedZoneDestroy(perms.contains("destroy"));
			world.setUnclaimedZoneSwitch(perms.contains("switch"));
			world.setUnclaimedZoneItemUse(perms.contains("itemuse") || perms.contains("item_use"));

			plugin.resetCache();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_set_wild_perms", world.getName(), perms.toString()));
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /townyworld set wildperm build destroy <world>");
		}
	}

	private void setWildIgnore(CommandSender sender, TownyWorld world, String[] split) {
		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_input", "/townyworld set wildignore OAK_SAPLING GOLD_ORE IRON_ORE"));
		else
			try {
				List<String> mats = Arrays.asList(StringMgmt.remFirstArg(split));
				world.setUnclaimedZoneIgnore(mats);

				plugin.resetCache();
				TownyMessaging.sendMsg(sender, Translatable.of("msg_set_wild_ignore", world.getName(), world.getUnclaimedZoneIgnoreMaterials()));

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_input", "/townyworld set wildignore OAK_SAPLING GOLD_ORE IRON_ORE"));
			}
	}

	private void setWildRegen(CommandSender sender, TownyWorld world, String[] split) {
		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /townyworld set wildregen Creeper,EnderCrystal,EnderDragon,Fireball,SmallFireball,LargeFireball,TNTPrimed,ExplosiveMinecart");
		else {
			
			String[] entities = String.join(",", StringMgmt.remFirstArg(split)).split(",");
			List<String> entityList = new ArrayList<>(Arrays.asList(entities));

			world.setPlotManagementWildRevertEntities(entityList);

			TownyMessaging.sendMsg(sender, Translatable.of("msg_set_wild_regen", world.getName(), world.getPlotManagementWildRevertEntities()));
		}
	}

	private void setWildName(CommandSender sender, TownyWorld world, String[] split) {
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /townyworld set wildname Wildy");
		} else {
			world.setUnclaimedZoneName(split[1]);
			TownyMessaging.sendMsg(sender, Translatable.of("msg_set_wild_name", world.getName(), split[1]));
		}
	}
	
	private Translatable formatBool(boolean bool) {
		return bool ? Translatable.of("enabled") : Translatable.of("disabled");
	}
	
	private Translatable formatBool(boolean bool, String ifTrue, String ifFalse) {
		return bool ? Translatable.of(ifTrue) : Translatable.of(ifFalse);
	}
}
