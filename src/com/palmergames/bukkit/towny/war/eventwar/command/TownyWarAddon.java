package com.palmergames.bukkit.towny.war.eventwar.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.object.AddonCommand;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.towny.war.eventwar.WarUniverse;
import com.palmergames.bukkit.towny.war.eventwar.hud.HUDManager;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;
import com.palmergames.bukkit.util.ChatTools;

public class TownyWarAddon implements TabExecutor {

	public TownyWarAddon() {
		AddonCommand townyWarCommand = new AddonCommand(CommandType.TOWNY, "war", this);
		TownyCommandAddonAPI.addSubCommand(townyWarCommand);
	}
	
	private static final List<String> townyWarTabCompletes = Arrays.asList(
			"stats","scores","hud","participants","types"
	);
	

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return NameUtil.filterByStart(townyWarTabCompletes, args[0]);
	}

	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		parseWarCommand(args, sender);
		return true;
	}
	
	private void parseWarCommand(String[] args, CommandSender sender) {
		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;
		
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("stats")) {
				parseWarStats(sender);
			} else if (args[0].equalsIgnoreCase("scores")) {
				parseWarScores(sender);
			} else if (args[0].equalsIgnoreCase("participants")) {
				parseWarParticipants(sender);
			} else if (args[0].equalsIgnoreCase("hud") && player == null) {
				TownyMessaging.sendMsg("No hud for console!");
			} else if (args[0].equalsIgnoreCase("hud") && player != null) {
				if (TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_WAR_HUD.getNode()) 
				&& WarUniverse.getInstance().getWarEvent(player) != null) {
					HUDManager.toggleWarHUD(player);
				} else {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_command_disable"));
				}
			} else if (args[0].equalsIgnoreCase("types")) {
				TownyMessaging.sendMessage(sender, getWarTypes());
			}
		}
	}

	private List<String> getWarTypes() {
		List<String> lines = new ArrayList<>();
		lines.add(ChatTools.formatTitle("War Types"));
		WarType type = WarType.RIOT;
		lines.add(type.getName());
		lines.add("   hasMayorDeath: " + type.hasMayorDeath);
		lines.add("   residentLives: " + type.residentLives);
		lines.add("      mayorLives: " + type.mayorLives);
		lines.add("   pointsPerKill: " + type.pointsPerKill);
		lines.add("      baseSpoils: " + type.baseSpoils);
		lines.add("  hasTownBlockHP: " + type.hasTownBlockHP);
		lines.add("   takesoverTown: " + type.hasTownConquering);
		type = WarType.TOWNWAR;
		lines.add(type.getName());
		lines.add("   hasMayorDeath: " + type.hasMayorDeath);
		lines.add("   residentLives: " + type.residentLives);
		lines.add("      mayorLives: " + type.mayorLives);
		lines.add("   pointsPerKill: " + type.pointsPerKill);
		lines.add("      baseSpoils: " + type.baseSpoils);
		lines.add("  hasTownBlockHP: " + type.hasTownBlockHP);
		lines.add("   takesoverTown: " + type.hasTownConquering);
		type = WarType.CIVILWAR;
		lines.add(type.getName());
		lines.add("   hasMayorDeath: " + type.hasMayorDeath);
		lines.add("   residentLives: " + type.residentLives);
		lines.add("      mayorLives: " + type.mayorLives);
		lines.add("   pointsPerKill: " + type.pointsPerKill);
		lines.add("      baseSpoils: " + type.baseSpoils);
		lines.add("  hasTownBlockHP: " + type.hasTownBlockHP);
		lines.add("   takesoverTown: " + type.hasTownConquering);
		type = WarType.NATIONWAR;
		lines.add(type.getName());
		lines.add("   hasMayorDeath: " + type.hasMayorDeath);
		lines.add("   residentLives: " + type.residentLives);
		lines.add("      mayorLives: " + type.mayorLives);
		lines.add("   pointsPerKill: " + type.pointsPerKill);
		lines.add("      baseSpoils: " + type.baseSpoils);
		lines.add("  hasTownBlockHP: " + type.hasTownBlockHP);
		lines.add("   takesoverTown: " + type.hasTownConquering);
		type = WarType.WORLDWAR;
		lines.add(type.getName());
		lines.add("   hasMayorDeath: " + type.hasMayorDeath);
		lines.add("   residentLives: " + type.residentLives);
		lines.add("      mayorLives: " + type.mayorLives);
		lines.add("   pointsPerKill: " + type.pointsPerKill);
		lines.add("      baseSpoils: " + type.baseSpoils);
		lines.add("  hasTownBlockHP: " + type.hasTownBlockHP);
		lines.add("   takesoverTown: " + type.hasTownConquering);
		
		return lines;
	}

	private void parseWarParticipants(CommandSender sender) {
		if (!WarUniverse.getInstance().isWarTime()) {
			TownyMessaging.sendErrorMsg(sender, "There are no wars!");
			return;
		}
		for (War war : WarUniverse.getInstance().getWars()) {
			war.getWarParticipants().outputParticipants(sender, war.getWarType());
		}
	}	
	
	private void parseWarStats(CommandSender sender) {
		if (sender instanceof Player player) {
			War war = WarUniverse.getInstance().getWarEvent(player); 
			if (war != null)
				war.getScoreManager().sendStats(player);
			else 
				TownyMessaging.sendErrorMsg(player, "You do not have a war!");
			return;
		}
		TownyMessaging.sendErrorMsg(sender, "Not meant for console.");
	}
	
	private void parseWarScores(CommandSender sender) {
		if (sender instanceof Player player) {
			War war = WarUniverse.getInstance().getWarEvent(player); 
			if (war != null)
				war.getScoreManager().sendScores(player, -1);
			else 
				TownyMessaging.sendErrorMsg(player, "You do not have a war!");
			return;
		}
		TownyMessaging.sendErrorMsg(sender, "Not meant for console.");

	}
}
