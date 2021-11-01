package com.palmergames.bukkit.towny.war.eventwar.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.AddonCommand;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.towny.war.eventwar.WarUniverse;
import com.palmergames.bukkit.towny.war.eventwar.db.WarMetaDataLoader;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;
import com.palmergames.bukkit.towny.war.eventwar.settings.EventWarSettings;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.util.StringMgmt;

public class TownyAdminWarAddon extends BaseCommand implements TabExecutor {

	private Towny plugin;
	private CommandSender sender;

	private static final List<String> townyWarAdminTabCompletes = Arrays.asList(
			"riot",
			"townwar",
			"civilwar",
			"nationwar",
			"worldwar",
			"endwar",
			"list",
			"purge",
			"toggle"
		);
	
	private static final List<String> townyWarToggleAdminTabCompletes = Arrays.asList("allowpeaceful");
	
	public TownyAdminWarAddon(Towny towny) {
		this.plugin = towny;
		AddonCommand townyAdminWarCommand = new AddonCommand(CommandType.TOWNYADMIN, "war", this);
		TownyCommandAddonAPI.addSubCommand(townyAdminWarCommand);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		parseAdminWarCommand(args, sender);
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		switch (args[0].toLowerCase()) {
			case "civilwar":
				if (args.length == 2)
					return getTownyStartingWith(args[1], "n");
				else
					return Collections.emptyList();
			case "riot":
				if (args.length == 2)
					return getTownyStartingWith(args[1], "t");
				else 
					return Collections.emptyList();
			case "townwar":
				if (args.length == 2)
					return getTownyStartingWith(args[1], "t");
				else if (args.length == 3)
					return getTownyStartingWith(args[2], "t");
				else 
					return Collections.emptyList();
			case "nationwar":
				if (args.length == 2)
					return getTownyStartingWith(args[1], "n");
				else if (args.length == 3)
					return getTownyStartingWith(args[2], "n");
				else 
					return Collections.emptyList();
			case "endwar": 
				if (args.length == 2)
					return NameUtil.filterByStart(WarUniverse.getInstance().getWarNames(), args[1]);
				else
					return Collections.emptyList();
			case "toggle":
				if (args.length == 2)
					return NameUtil.filterByStart(townyWarToggleAdminTabCompletes, args[1]);
				else
					return Collections.emptyList();
			default:
				if (args.length == 1)
					return NameUtil.filterByStart(townyWarAdminTabCompletes, args[0]);
		}
		return Collections.emptyList();
	}
	
	private void parseAdminWarCommand(String[] split, CommandSender sender) {
		if (sender instanceof Player player) {
			// TODO: Make a proper permission node.
			if (!TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player)) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_command_disable"));
				return;
			}
		}
		
		this.sender = sender;
		
		try {
			if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
				showWarHelp();
				return;
			}

			switch (split[0].toLowerCase()) {
				case "worldwar":
					parseWorldWarCommand();
					break;
				case "riot":
					parseRiotCommand(StringMgmt.remFirstArg(split));
					break;
				case "townwar":
					parseTownWarCommand(StringMgmt.remFirstArg(split));
					break;
				case "civilwar":
					parseCivilWarCommand(StringMgmt.remFirstArg(split));
					break;
				case "nationwar":
					parseNationWarCommand(StringMgmt.remFirstArg(split));
					break;
				case "list":
					parseListCommand();
					break;
				case "purge":
					parsePurgeCommand();
					break;
				case "endwar":
					parseEndWarCommand(StringMgmt.remFirstArg(split));
					break;
				case "toggle":
					parseToggleCommand(StringMgmt.remFirstArg(split));
					break;
				default:
					showWarHelp();
			}

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}

	}
	
	private void parseToggleCommand(String[] split) {
		if (sender instanceof Player player) {
			// TODO: Make a proper permission node.
			if (!TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player)) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_command_disable"));
				return;
			}
		}
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			showWarToggleHelp();
			return;
		}	
		switch (split[0].toLowerCase()) {
			case "allowpeaceful":
				EventWarSettings.setDeclaringNeutral(!EventWarSettings.isDeclaringNeutral());
				TownyMessaging.sendMsg(sender, Translatable.of("msg_nation_allow_peaceful", EventWarSettings.isDeclaringNeutral() ? Translatable.of("enabled") : Translatable.of("disabled")));
				break;
			default:
				showWarToggleHelp();
		}
	}

	private void parseEndWarCommand(String[] split) {
		if (split.length == 0) {
			sender.sendMessage(ChatTools.formatTitle("/twa endwar"));
			sender.sendMessage(ChatTools.formatCommand("", "/twa endwar", "{warname}", "Used to end a war early."));
			return;
		}
		endWar(sender, split);
	}

	private void parsePurgeCommand() {
		Confirmation.runOnAccept(()-> {
			WarMetaDataLoader.removeAllWars(true);
			TownyMessaging.sendMsg(sender, Translatable.of("msg_wars_purged"));
		}).sendTo(sender);
	}

	private void parseListCommand() {
		sender.sendMessage(ChatTools.formatTitle("Ongoing Wars"));
		if (!TownyAPI.getInstance().isWarTime()) {
			sender.sendMessage(ChatTools.formatCommand("None", "", ""));
			return;
		}
		for (War war : WarUniverse.getInstance().getWars())
			sender.sendMessage(ChatTools.formatCommand("War Name: " + war.getWarName(), "Type: " + war.getWarType().getName(), ""));
	}

	private void parseNationWarCommand(String[] split) throws TownyException {
		List<Nation> nations = new ArrayList<>();
		List<Resident> residents = new ArrayList<>();
		Nation nation = TownyAPI.getInstance().getNation(split[0]);
		if (nation == null)
			throw new TownyException(Translatable.of("msg_invalid_name").forLocale(sender));
		nations.add(nation);
		residents.addAll(nation.getResidents());
		nation = TownyAPI.getInstance().getNation(split[1]);
		if (nation == null)
			throw new TownyException(Translatable.of("msg_invalid_name").forLocale(sender));
		nations.add(nation);
		residents.addAll(nation.getResidents());
		TownyMessaging.sendMsg(sender, "Beginning Nation War in " + EventWarSettings.nationWarDelay() + " seconds.");
		new War(plugin, nations, null, residents, WarType.NATIONWAR);
	}

	private void parseCivilWarCommand(String[] split) throws TownyException {
		List<Nation> nations = new ArrayList<>();
		List<Resident> residents = new ArrayList<>();
		Nation nation = TownyAPI.getInstance().getNation(split[0]);
		if (nation == null)
			throw new TownyException(Translatable.of("msg_invalid_name").forLocale(sender));
		nations.add(nation);
		residents.addAll(nation.getResidents());
		TownyMessaging.sendMsg(sender, "Beginning Civil War in " + EventWarSettings.civilWarDelay() + " seconds.");
		new War(plugin, nations, null, residents, WarType.CIVILWAR);
	}

	private void parseTownWarCommand(String[] split) throws TownyException {
		List<Town> towns = new ArrayList<>();
		List<Resident> residents = new ArrayList<>();
		Town town = TownyAPI.getInstance().getTown(split[0]);
		if (town == null)
			throw new TownyException(Translatable.of("msg_invalid_name").forLocale(sender));
		towns.add(town);
		residents.addAll(town.getResidents());
		town = TownyAPI.getInstance().getTown(split[1]);
		if (town == null)
			throw new TownyException(Translatable.of("msg_invalid_name").forLocale(sender));
		towns.add(town);
		residents.addAll(town.getResidents());
		TownyMessaging.sendMsg(sender, "Beginning Town War in " + EventWarSettings.townWarDelay() + " seconds.");
		new War(plugin, null, towns, residents, WarType.TOWNWAR);
	}

	private void parseRiotCommand(String[] split) throws TownyException {
		List<Town> towns = new ArrayList<>();
		List<Resident> residents = new ArrayList<>();
		Town town = TownyAPI.getInstance().getTown(split[0]);
		if (town == null)
			throw new TownyException(Translatable.of("msg_invalid_name").forLocale(sender));
		towns.add(town);
		residents.addAll(town.getResidents());
		TownyMessaging.sendMsg(sender, "Beginning Riot War in " + EventWarSettings.riotDelay() + " seconds.");
		new War(plugin, null, towns, residents, WarType.RIOT);
	}

	private void parseWorldWarCommand() {
		List<Nation> nations = new ArrayList<>();
		List<Resident> residents = new ArrayList<>();

		for (Nation nation : TownyUniverse.getInstance().getNations()) {
			nations.add(nation);
			residents.addAll(nation.getResidents());
		}
		TownyMessaging.sendMsg(sender, "Beginning World War in " + EventWarSettings.worldWarDelay() + " seconds.");
		new War(plugin, nations, null, residents, WarType.WORLDWAR);
	}

	private void endWar(CommandSender sender, String[] args) {
		String warName = StringMgmt.join(args);	
		War war = null;
		for (War wars: WarUniverse.getInstance().getWars())
			if (wars.getWarName().equalsIgnoreCase(warName)) {
				war = wars;
				break;
			}
		if (war != null) {
			war.end(true);
			TownyMessaging.sendMessage(sender, Translatable.of("msg_war_name_ended", warName));
		} else
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_unknown_war", warName));
	}
	
	private void showWarHelp() {
		sender.sendMessage(ChatTools.formatTitle("/townyadmin war"));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "RIOT [town]", ""));		
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "TOWNWAR [town] [town]", ""));		
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "CIVILWAR [nation]", ""));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "NATIONWAR [nation] [nation]", ""));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "WORLDWAR", ""));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "endwar [war name]", ""));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "list", ""));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin war", "purge", ""));
	}
	
	private void showWarToggleHelp() {
		sender.sendMessage(ChatTools.formatTitle("/ta war toggle"));
		sender.sendMessage(ChatTools.formatCommand("", "/ta war toggle", "allowpeaceful", ""));
	}
}
	