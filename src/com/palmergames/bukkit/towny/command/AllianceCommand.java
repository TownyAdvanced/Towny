package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.event.alliance.AllianceAddEnemyEvent;
import com.palmergames.bukkit.towny.event.alliance.AlliancePreAddEnemyEvent;
import com.palmergames.bukkit.towny.event.alliance.AlliancePreAddNationEvent;
import com.palmergames.bukkit.towny.event.alliance.AlliancePreRemoveEnemyEvent;
import com.palmergames.bukkit.towny.event.alliance.AlliancePreRenameEvent;
import com.palmergames.bukkit.towny.event.alliance.AllianceRemoveEnemyEvent;
import com.palmergames.bukkit.towny.event.alliance.AllianceRemoveNationEvent;
import com.palmergames.bukkit.towny.event.alliance.AllianceRequestNationJoinAllianceEvent;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Alliance;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.comparators.ComparatorCaches;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.towny.object.inviteobjects.AllianceNationInvite;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.StringMgmt;

public class AllianceCommand extends BaseCommand implements CommandExecutor {
	private static Towny plugin;
	private CommandSender sender;
	private Player player;
	private Translator translator;

	public AllianceCommand(Towny instance) {
		plugin = instance;
	}
	
	private static final List<String> allianceTabCompletes = Arrays.asList(
		"set",
		"add",
		"invite",
		"kick",
		"remove",
		"enemy",
		"enemylist",
		"memberlist",
		"list",
		"online",
		"delete"
	);
	
	private static final List<String> allianceEnemyTabCompletes = Arrays.asList(
		"add",
		"remove"
	);
	
	private static final List<String> allianceSetTabCompletes = Arrays.asList(
		"name"
	);
	
	private static final List<String> allianceListTabCompletes = Arrays.asList(
		"residents",
		"founded",
		"name",		
		"online",
		"townblocks",
		"towns",
		"nations"
	);
	
	private static final List<String> allianceConsoleTabCompletes = Arrays.asList(
		"?",
		"help",
		"list"
	);
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			switch (args[0].toLowerCase()) {
			case "list":
				switch (args.length) {
				case 2:
					return Collections.singletonList("by");
				case 3:
					return NameUtil.filterByStart(allianceListTabCompletes, args[2]);
				default:
					return Collections.emptyList();
			}
			case "delete":
				return Collections.emptyList();
			case "add":
			case "invite":
				return getTownyStartingWith(args[args.length - 1], "n");
			case "kick":
			case "remove":
				Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
				if (res != null && res.hasAlliance())
					return NameUtil.filterByStart(NameUtil.getNames(res.getAllianceOrNull().getMembers()), args[args.length - 1]);
				else 
					return Collections.emptyList();
			case "enemy":
				if (args.length == 2) {
					return NameUtil.filterByStart(allianceEnemyTabCompletes, args[1]);
				} else if (args.length >= 3){
					switch (args[1].toLowerCase()) {
						case "add":
							return getTownyStartingWith(args[2], "a");
						case "remove":
							// Return enemies of alliance
							try {
								return NameUtil.filterByStart(NameUtil.getNames(getAllianceFromPlayerOrThrow(player).getEnemies()), args[2]);
							} catch (TownyException ignored) {}
						default:
							return Collections.emptyList();
					}
				}
				break;
			case "enemylist":
			case "memberlist":
			case "online":
				return getTownyStartingWith(args[args.length - 1], "a");
			case "set":
				if (args.length == 2) {
					return NameUtil.filterByStart(allianceSetTabCompletes, args[1]);
				}
				return Collections.emptyList();
			default:
				if (args.length == 1)
					return filterByStartOrGetTownyStartingWith(TownyCommandAddonAPI.getTabCompletes(CommandType.ALLIANCE, allianceTabCompletes), args[0], "a");
				else if (args.length > 1 && TownyCommandAddonAPI.hasCommand(CommandType.ALLIANCE, args[0]))
					return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.ALLIANCE, args[0]).getTabCompletion(sender, args), args[args.length-1]);
			}
		} else if (args.length == 1) {
			return filterByStartOrGetTownyStartingWith(allianceConsoleTabCompletes, args[0], "a");
		}
		return Collections.emptyList();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		this.sender = sender;
		this.translator = Translator.locale(Translation.getLocale(sender));
		if (sender instanceof Player player) {
			if (plugin.isError()) {
				TownyMessaging.sendErrorMsg(player, "Locked in Safe mode!");
				return false;
			}
			this.player = player;
			try {
				parseAllianceCommand(player, args);
			} catch (TownyException te) {
				TownyMessaging.sendErrorMsg(player, te.getMessage(player));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}

		} else
			parseAllianceCommandForConsole(sender, args);

		return true;
	}

	private void parseAllianceCommandForConsole(CommandSender sender, String[] split) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

			HelpMenu.ALLIANCE_HELP_CONSOLE.send(sender);
		
		} else if (split[0].equalsIgnoreCase("list")) {

			try {
				allianceList(sender, split);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			}
			
		} else if (split[0].equalsIgnoreCase("memberlist")) {

			try {
				allianceMemberList(sender, split);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			}
		} else if (TownyCommandAddonAPI.hasCommand(CommandType.ALLIANCE, split[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.ALLIANCE, split[0]).execute(sender, "alliance", split);
		} else {
			Alliance alliance = TownyUniverse.getInstance().getAlliance(split[0]);
			
			if (alliance != null)
				allianceStatusScreen(sender, alliance);
			else
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", split[0]));
		}
		
	}

	private void parseAllianceCommand(Player player, String[] split) throws TownyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		
		if (split.length == 0) {

			allianceStatusScreen(player, getAllianceFromPlayerOrThrow(player));
		} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

			HelpMenu.ALLIANCE_HELP.send(player);
		} else if (split[0].equalsIgnoreCase("set")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_SET.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));

			allianceSet(player, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase("add") || split[0].equalsIgnoreCase("invite")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_ADD.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));

			allianceAdd(player, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase("remove") || split[0].equalsIgnoreCase("kick")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_REMOVE.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));

			allianceRemove(player, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase("delete")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_DELETE.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));

			allianceDelete(player, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase("enemy")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_ENEMY.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));

			allianceEnemy(player, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase("online")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_ONLINE.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
	
			allianceOnline(player, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase("list")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_LIST.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
	
			allianceList(player, split);
		} else if (split[0].equalsIgnoreCase("memberlist")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_MEMBERLIST.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
	
			allianceMemberList(player, split);
		} else if (split[0].equalsIgnoreCase("enemylist")) {
			
			allianceEnemyList(player, split);
		}
		
	}

	private void allianceList(CommandSender sender, String[] split) throws TownyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		boolean console = true;
		if (sender instanceof Player player)
			console = false;

		/*
		 * The default comparator on /alliance list is by residents, test it before we start anything else.
		 */
		if (split.length < 2 && !console && !permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_LIST_RESIDENTS.getNode()))
			throw new TownyException(Translatable.of("msg_err_command_disable"));
		
		List<Alliance> alliancesToSort = new ArrayList<>(TownyUniverse.getInstance().getAlliances());
		int page = 1;
		boolean pageSet = false;
		boolean comparatorSet = false;
		ComparatorType type = ComparatorType.RESIDENTS;
		int total = (int) Math.ceil(((double) alliancesToSort.size()) / ((double) 10));
		for (int i = 1; i < split.length; i++) {
			if (split[i].equalsIgnoreCase("by")) { // Is a case of someone using /alliance list by {comparator}
				if (comparatorSet) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_multiple_comparators"));
					return;
				}
				i++;
				if (i < split.length) {
					comparatorSet = true;
					if (split[i].equalsIgnoreCase("resident")) 
						split[i] = "residents";
					
					if (!console && !permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_LIST.getNode(split[i])))
						throw new TownyException(Translatable.of("msg_err_command_disable"));
					
					if (!allianceListTabCompletes.contains(split[i].toLowerCase()))
						throw new TownyException(Translatable.of("msg_error_invalid_comparator_alliance"));

					type = ComparatorType.valueOf(split[i].toUpperCase());
				} else {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_missing_comparator"));
					return;
				}
				comparatorSet = true;
			} else { // Is a case of someone using /alliance list, /alliance list # or /alliance list by {comparator} #
				if (pageSet) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_too_many_pages"));
					return;
				}
				try {
					page = Integer.parseInt(split[i]);
					if (page < 0) {
						TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_negative"));
						return;
					} else if (page == 0) {
						TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_must_be_int"));
						return;
					}
					pageSet = true;
				} catch (NumberFormatException e) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_must_be_int"));
					return;
				}
			}
		}
		
		if (page > total) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("LIST_ERR_NOT_ENOUGH_PAGES", total));
			return;
		}

		final ComparatorType finalType = type;
		final int pageNumber = page;
		try {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				TownyMessaging.sendAllianceList(sender, ComparatorCaches.getAllianceListCache(finalType), finalType, pageNumber, total);
			});
		} catch (RuntimeException e) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_comparator_failed"));
		}
	}

	private void allianceSet(Player player, String[] split) throws TownyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		
		if (split.length == 0) {
			HelpMenu.ALLIANCE_SET.send(player);
		} else if (split[0].equalsIgnoreCase("name")) {
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_SET_NAME.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));

			allianceSetName(player, StringMgmt.remFirstArg(split));
		}
	}

	private void allianceSetName(Player player, String[] names) throws TownyException {
		Nation nation = getNationFromPlayerOrThrow(player);
		Alliance alliance = nation.getAllianceOrNull();
		if (!alliance.getFounder().equals(nation))
			// Alliance isn't founded by the player's nation.
			throw new TownyException(Translatable.of("msg_own_alliance_disallow"));
		if (names.length == 0)
			// No names were supplied.
			throw new TownyException(Translatable.of("msg_specify_alliance_name"));

		// Combine any words given into one string.
		String name = StringMgmt.join(names, "_");

		// Auto-capitalize.
		if (TownySettings.getTownAutomaticCapitalisationEnabled())
			name = StringMgmt.capitalizeStrings(name);

		// Make sure this name passes the smell-test and isn't otherwise used.
		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			filteredName = null;
		}
		if (filteredName == null || TownyUniverse.getInstance().hasAlliance(filteredName))
			throw new TownyException(Translatable.of("msg_err_invalid_name", name));

		// Call cancellable event.
		AlliancePreRenameEvent apsne = new AlliancePreRenameEvent(alliance, filteredName);
		Bukkit.getPluginManager().callEvent(apsne);
		if (apsne.isCancelled())
			throw new TownyException(apsne.getCancelMessage());

		// Replace the previous name in the TU trie.
		TownyUniverse.getInstance().getAlliancesTrie().removeKey(alliance.getName());
		TownyUniverse.getInstance().getAlliancesTrie().addKey(filteredName);

		// Rename and Save the alliance.
		alliance.setName(filteredName);
		alliance.save();
		TownyMessaging.sendPrefixedAllianceMessage(alliance, Translatable.of("msg_alliance_set_name", player.getName(), alliance.getName()));
	}

	private void allianceAdd(Player player, String[] names) throws TownyException {
		if (names.length == 0)
			throw new TownyException(Translatable.of("msg_usage", "/alliance add [nation name]"));
		
		Alliance alliance = getAllianceFromPlayerOrThrow(player);
		ArrayList<Nation> inviteList = new ArrayList<>();
		for (String name : names) {
			Nation nation = TownyUniverse.getInstance().getNation(name);
			if (nation != null) {
				if (alliance.hasNation(nation)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nation_already_part_of_alliance", name));
				} else {
					inviteList.add(nation);
				}
			}
		}
		if (!inviteList.isEmpty())
			allianceAdd(player, alliance, inviteList);
		
	}

	private void allianceAdd(Player player, Alliance alliance, ArrayList<Nation> toInvite) throws TownyException {
		List<Nation> removed = new ArrayList<>(); 
		for (Nation nation : toInvite) {
			if (!allianceInviteAlly(player, alliance, nation))
				// Will return false if the AlliancePreAddNationEvent is cancelled or the player
				// isn't allowed to invite an NPC nation.
				removed.add(nation);
		}
		for (Nation removedNation : removed)
			toInvite.remove(removedNation);
		if (toInvite.isEmpty())
			throw new TownyException(Translatable.of("msg_invalid_name"));
	}

	private boolean allianceInviteAlly(Player player, Alliance alliance, Nation nation) throws TownyException {
		AlliancePreAddNationEvent apane = new AlliancePreAddNationEvent(alliance, nation);
		Bukkit.getPluginManager().callEvent(apane);
		if (apane.isCancelled()) {
			TownyMessaging.sendErrorMsg(player, apane.getCancelMessage());
			return false;
		}
		if (!nation.getCapital().getMayor().isNPC()) {
			// Send an invite to the nation.
			allianceCreateAllyRequest(player, alliance, nation);
			TownyMessaging.sendPrefixedAllianceMessage(alliance, Translatable.of("msg_ally_req_sent", nation));
			return true;
		} else {
			// Send the invite to the NPC, only successful if player is an admin.
			return allianceAddNPCNationAsAlly(player, alliance, nation);
		}
	}

	private void allianceCreateAllyRequest(CommandSender sender, Alliance alliance, Nation nation) throws TownyException {
		AllianceNationInvite invite = new AllianceNationInvite(sender, nation, alliance);
		try {
			if (!InviteHandler.inviteIsActive(invite)) {
				nation.newReceivedInvite(invite);
				alliance.newSentAllianceInvite(invite);
				InviteHandler.addInvite(invite);

				for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
					if (player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_ALLY_ACCEPT.getNode()))
						TownyMessaging.sendRequestMessage(player, invite);
				
				Bukkit.getPluginManager().callEvent(new AllianceRequestNationJoinAllianceEvent(invite));
			} else {
				throw new TownyException(Translatable.of("msg_err_ally_already_requested", nation));
			}
		} catch (TooManyInvitesException e) {
			nation.deleteReceivedInvite(invite);
			alliance.deleteSentAllianceInvite(invite);
			throw new TownyException(e.getMessage());
		}
	}

	private boolean allianceAddNPCNationAsAlly(Player player, Alliance alliance, Nation nation) {
		if (TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player)) {
			nation.setAlliance(alliance);
			nation.save();
			TownyMessaging.sendPrefixedAllianceMessage(alliance, Translatable.of("msg_nation_has_joined_alliance", nation));
			return true;
		} else {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_unable_ally_npc"));
			return false;
		}
	}

	private void allianceRemove(Player player, String[] names) throws TownyException {
		if (names.length == 0)
			throw new TownyException(Translatable.of("msg_usage", "/alliance remove [nation name]"));
		
		Alliance alliance = getAllianceFromPlayerOrThrow(player);
		ArrayList<Nation> removeList = new ArrayList<>();
		for (String name : names) {
			Nation nation = TownyUniverse.getInstance().getNation(name);
			if (nation != null) {
				if (!alliance.hasNation(nation)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nation_not_part_of_alliance", name));
				} else {
					removeList.add(nation);
				}
			}
		}
		if (!removeList.isEmpty())
			allianceRemove(player, alliance, removeList);
	}

	private void allianceRemove(Player player, Alliance alliance, ArrayList<Nation> removeList) throws TownyException {
		List<Nation> failed = new ArrayList<>(); 
		for (Nation nation : removeList) {
			if (!allianceRemoveAlly(player, alliance, nation))
				// Will return false if the AllianceRemoveNationEvent is cancelled.
				failed.add(nation);
		}
		for (Nation removedNation : failed)
			removeList.remove(removedNation);
		if (removeList.isEmpty())
			throw new TownyException(Translatable.of("msg_invalid_name"));
	}

	private boolean allianceRemoveAlly(Player player, Alliance alliance, Nation nation) {
		AllianceRemoveNationEvent arne = new AllianceRemoveNationEvent(alliance, nation);
		Bukkit.getPluginManager().callEvent(arne);
		if (arne.isCancelled()) {
			TownyMessaging.sendErrorMsg(player, arne.getCancelMessage());
			return false;
		}

		nation.removeAlliance();
		nation.save();
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_removed_ally", alliance));
		TownyMessaging.sendPrefixedAllianceMessage(alliance, Translatable.of("msg_alliance_removed_ally", nation));
		return true;
	}

	private void allianceEnemy(Player player, String[] split) throws TownyException {
		if (split.length < 2)
			throw new TownyException(Translatable.of("msg_usage", "/alliance enemy [add/remove] [name]"));

		Alliance alliance = getAllianceFromPlayerOrThrow(player);
		String test = split[0]; // Should be either add or remove
		String[] names = StringMgmt.remFirstArg(split); // Remainder should be a list of names.

		if ((test.equalsIgnoreCase("remove") || test.equalsIgnoreCase("add")) && names.length > 0) {
			ArrayList<Alliance> list = new ArrayList<>();
			Alliance enemy;
			boolean add = test.equalsIgnoreCase("add");

			for (String name : names) {
				enemy = getAllianceOrThrow(name);

				if (alliance.equals(enemy))
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_own_alliance_disallow"));
				else if (add && alliance.hasEnemy(enemy))
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_alliance_already_enemies_with", enemy.getName()));
				else if (!add && !alliance.hasEnemy(enemy))
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_alliance_not_enemies_with", enemy.getName()));
				else
					list.add(enemy);
			}
			if (!list.isEmpty())
				allianceEnemy(player, alliance, list, add);

		} else {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", "[add/remove]"));
		}
	}

	private void allianceEnemy(Player player, Alliance alliance, ArrayList<Alliance> enemies, boolean add) {

		ArrayList<Alliance> remove = new ArrayList<>();
		for (Alliance targetAlliance : enemies)
			if (add && !alliance.getEnemies().contains(targetAlliance)) {
				AlliancePreAddEnemyEvent apaee = new AlliancePreAddEnemyEvent(alliance, targetAlliance);
				Bukkit.getPluginManager().callEvent(apaee);
				
				if (!apaee.isCancelled()) {
					alliance.addEnemy(targetAlliance);
					
					AllianceAddEnemyEvent aaee = new AllianceAddEnemyEvent(alliance, targetAlliance);
					Bukkit.getPluginManager().callEvent(aaee);

					TownyMessaging.sendPrefixedAllianceMessage(targetAlliance, Translatable.of("msg_added_enemy_alliance", alliance));
				} else {
					TownyMessaging.sendErrorMsg(player, apaee.getCancelMessage());
					remove.add(targetAlliance);
				}

			} else if (alliance.getEnemies().contains(targetAlliance)) {
				AlliancePreRemoveEnemyEvent apree = new AlliancePreRemoveEnemyEvent(alliance, targetAlliance);
				Bukkit.getPluginManager().callEvent(apree);
				if (!apree.isCancelled()) {
					alliance.removeEnemy(targetAlliance);

					AllianceRemoveEnemyEvent aree = new AllianceRemoveEnemyEvent(alliance, targetAlliance);
					Bukkit.getPluginManager().callEvent(aree);
					
					TownyMessaging.sendPrefixedAllianceMessage(targetAlliance, Translatable.of("msg_removed_enemy_alliance", alliance));
				} else {
					TownyMessaging.sendErrorMsg(player, apree.getCancelMessage());
					remove.add(targetAlliance);
				}
			}
		
		for (Alliance newEnemy : remove)
			enemies.remove(newEnemy);

		if (enemies.size() > 0) {
			String msg = "";

			for (Alliance newEnemy : enemies)
				msg += newEnemy.getName() + ", ";

			msg = msg.substring(0, msg.length() - 2);
			if (add)
				TownyMessaging.sendPrefixedAllianceMessage(alliance, Translatable.of("msg_enemy_alliance", player.getName(), msg));
			else
				TownyMessaging.sendPrefixedAllianceMessage(alliance, Translatable.of("msg_enemy_alliance_to_neutral", player.getName(), msg));

			TownyUniverse.getInstance().getDataSource().saveAlliances();

			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
	}

	private void allianceDelete(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			Alliance alliance = getAllianceFromPlayerOrThrow(player);
			Confirmation.runOnAccept(() -> TownyUniverse.getInstance().getDataSource().removeAlliance(alliance)).sendTo(player);
		} else {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_ALLIANCE_DELETE.getNode()))
				throw new TownyException(Translatable.of("msg_err_admin_only_delete_alliance"));

			Alliance alliance = getAllianceOrThrow(split[0]);
			Confirmation.runOnAccept(() -> {
				TownyMessaging.sendMsg(player, translator.of("alliance_deleted_by_admin", alliance.getName()));
				TownyUniverse.getInstance().getDataSource().removeAlliance(alliance);
			}).sendTo(player);

		}
	}

	private void allianceOnline(Player player, String[] split) throws TownyException {
		if (split.length > 0) {
			Alliance alliance = getAllianceOrThrow(split[0]);
			
			List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, alliance);
			if (onlineResidents.size() > 0) {
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(translator.of("msg_alliance_online"), alliance, player));
			} else {
				TownyMessaging.sendMessage(player, Colors.White + "0 " + translator.of("res_list") + " " + (translator.of("msg_alliance_online") + ": " + alliance));
			}
		} else {
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(translator.of("msg_alliance_online"), getAllianceFromPlayerOrThrow(player), player));
		}
	}

	private void allianceEnemyList(Player player, String[] split) throws TownyException {
		Alliance alliance = null;
		try {
			if (split.length == 1) {
				alliance = getAllianceFromPlayerOrThrow(player);
			} else {
				alliance = getAllianceOrThrow(split[1]);
			}
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(player, translator.of("msg_specify_name"));
			return;
		}
		if (alliance.getEnemies().isEmpty())
			TownyMessaging.sendErrorMsg(player, translator.of("msg_error_alliance_has_no_enemies"));
		else {
			TownyMessaging.sendMessage(player, ChatTools.formatTitle(alliance.getName() + " " + translator.of("status_nation_enemies")));
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedAlliances(translator.of("status_nation_enemies"), new ArrayList<>(alliance.getEnemies())));
		}
	}

	private void allianceMemberList(CommandSender sender, String[] args) throws TownyException {

		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;

		Alliance alliance = null;
		if (args.length == 1 && player != null) {
			alliance = getAllianceFromPlayerOrThrow(player);
		} else if (args.length == 2){
			alliance = TownyUniverse.getInstance().getAlliance(args[1]);
		}
		
		if (alliance != null) {
			TownyMessaging.sendMessage(sender, ChatTools.formatTitle(alliance.getName() + " " + translator.of("nation_plu")));
			TownyMessaging.sendMessage(sender, TownyFormatter.getFormattedTownyObjects(translator.of("nation_plu"), new ArrayList<>(alliance.getMembers())));
		} else 
			TownyMessaging.sendErrorMsg(sender, translator.of("msg_specify_name"));
	}

	private void allianceStatusScreen(CommandSender sender, Alliance alliance) {
		/*
		 * This is run async because if banks are added to alliance, this will ping the economy plugin.
		 */
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(alliance, sender)));
	}

}
