package com.palmergames.bukkit.towny.huds;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class PermHUD {

	public static void updatePerms (Player p) {
		WorldCoord worldCoord = new WorldCoord(p.getWorld(), Coord.parseCoord(p));
		updatePerms(p, worldCoord);
	}

	public static void updatePerms(Player p, WorldCoord worldCoord) {
		Translator translator = Translator.locale(p);
		String plotName, build, destroy, switching, item, type, pvp, explosions, firespread, mobspawn, title;
		Scoreboard board = p.getScoreboard();
		// Due to tick delay (probably not confirmed), a HUD can actually be removed from the player.
		// Causing board to return null, and since we don't create a new board, a NullPointerException occurs.
		// So we can call the toggleOn Method and return, causing this to be rerun and also the creation
		// of the scoreboard, at least that's the plan.
		if (board == null) {
			toggleOn(p);
			return;
		}
		
		if (board.getObjective("PERM_HUD_OBJ") == null) { // Some other plugin's scoreboard has  
			HUDManager.toggleOff(p);                      // likely been activated, meaning we
			return;                                       // will throw NPEs if we continue.
		}

		if (worldCoord.isWilderness()) {
			clearPerms(p);
			return;
		}

		TownBlock townBlock = worldCoord.getTownBlockOrNull();
		TownBlockOwner owner = townBlock.getTownBlockOwner();
		TownyWorld world = townBlock.getWorld();
		TownyPermission tp = townBlock.getPermissions();
		String v = (owner instanceof Resident) ? "f" : "r";
		String u = (owner instanceof Resident) ? "t" : "n";
		build = (tp.getResidentPerm(ActionType.BUILD) ? v : "-") + (tp.getNationPerm(ActionType.BUILD) ? u : "-") + (tp.getAllyPerm(ActionType.BUILD) ? "a" : "-") + (tp.getOutsiderPerm(ActionType.BUILD) ? "o" : "-");
		destroy = (tp.getResidentPerm(ActionType.DESTROY) ? v : "-") + (tp.getNationPerm(ActionType.DESTROY) ? u : "-") + (tp.getAllyPerm(ActionType.DESTROY) ? "a" : "-") + (tp.getOutsiderPerm(ActionType.DESTROY) ? "o" : "-");
		switching = (tp.getResidentPerm(ActionType.SWITCH) ? v : "-") + (tp.getNationPerm(ActionType.SWITCH) ? u : "-") + (tp.getAllyPerm(ActionType.SWITCH) ? "a" : "-") + (tp.getOutsiderPerm(ActionType.SWITCH) ? "o" : "-");
		item = (tp.getResidentPerm(ActionType.ITEM_USE) ? v : "-") + (tp.getNationPerm(ActionType.ITEM_USE) ? u : "-") + (tp.getAllyPerm(ActionType.ITEM_USE) ? "a" : "-") + (tp.getOutsiderPerm(ActionType.ITEM_USE) ? "o" : "-");
		type = townBlock.getType().equals(TownBlockType.RESIDENTIAL) ? " " : townBlock.getType().getName();
		pvp = !CombatUtil.preventPvP(worldCoord.getTownyWorld(), townBlock) ? translator.of("status_on") : translator.of("status_off");
		explosions = (world.isForceExpl() || tp.explosion) ? translator.of("status_on") : translator.of("status_off");
		firespread = (world.isForceFire() || tp.fire) ? translator.of("status_on") : translator.of("status_off");
		mobspawn = (world.isForceTownMobs() || tp.mobs) ? translator.of("status_on") : translator.of("status_off");

		// Displays the name of the owner, and if the owner is a resident the town name as well.
		title = ChatColor.GOLD + owner.getName() + (townBlock.hasResident() ? " (" + townBlock.getTownOrNull().getName() + ")" : ""); 

		plotName = townBlock.getName().isEmpty() ? "" : translator.of("msg_perm_hud_plot_name") + townBlock.getName();
		if (!plotName.isEmpty())
			board.getTeam("plot").setSuffix(HUDManager.check(plotName));
		else
			board.getTeam("plot").setSuffix(" ");
		board.getTeam("build").setSuffix(build);
		board.getTeam("destroy").setSuffix(destroy);
		board.getTeam("switching").setSuffix(switching);
		board.getTeam("item").setSuffix(item);
		board.getTeam("plotType").setSuffix(type);
		board.getTeam("pvp").setSuffix(pvp);
		board.getTeam("explosions").setSuffix(explosions);
		board.getTeam("firespread").setSuffix(firespread);
		board.getTeam("mobspawn").setSuffix(mobspawn);
		board.getObjective("PERM_HUD_OBJ").setDisplayName(HUDManager.check(title));
	}

	private static void clearPerms (Player p) {
		Scoreboard board = p.getScoreboard();
		try {
			board.getTeam("plot").setSuffix(" ");
			board.getTeam("build").setSuffix(" ");
			board.getTeam("destroy").setSuffix(" ");
			board.getTeam("switching").setSuffix(" ");
			board.getTeam("item").setSuffix(" ");
			board.getTeam("plotType").setSuffix(" ");
			board.getTeam("pvp").setSuffix(" ");
			board.getTeam("explosions").setSuffix(" ");
			board.getTeam("firespread").setSuffix(" ");
			board.getTeam("mobspawn").setSuffix(" ");
			board.getObjective("PERM_HUD_OBJ").setDisplayName(HUDManager.check(getFormattedWildernessName(p.getWorld())));
		} catch (NullPointerException e) {
			toggleOn(p);
		}
	}
	
	private static String getFormattedWildernessName(World w) {
		StringBuilder wildernessName = new StringBuilder().append(ChatColor.DARK_RED).append(ChatColor.BOLD);
		if (TownyAPI.getInstance().isTownyWorld(w)) 
			wildernessName.append(TownyAPI.getInstance().getTownyWorld(w).getFormattedUnclaimedZoneName());
		else 
			wildernessName.append("Unknown");

		return wildernessName.toString();
	}
	
	public static void toggleOn (Player p) {
		Translator translator = Translator.locale(p);
		String PERM_HUD_TITLE = ChatColor.GOLD + "";
		String permsTitle_entry = ChatColor.YELLOW + "" + ChatColor.UNDERLINE + translator.of("msg_perm_hud_title");
		String plotName_entry = ChatColor.DARK_GREEN + "";
		String build_entry = ChatColor.DARK_GREEN + translator.of("msg_perm_hud_build") + ChatColor.GRAY;
		String destroy_entry = ChatColor.DARK_GREEN + translator.of("msg_perm_hud_destroy") + ChatColor.GRAY;
		String switching_entry = ChatColor.DARK_GREEN + translator.of("msg_perm_hud_switch") + ChatColor.GRAY;
		String item_entry = ChatColor.DARK_GREEN + translator.of("msg_perm_hud_item_use") + ChatColor.GRAY;
		String keyPlotType_entry = ChatColor.DARK_GREEN + translator.of("msg_perm_hud_plot_type");
		String pvp_entry = ChatColor.DARK_GREEN + translator.of("msg_perm_hud_pvp") + " ";
		String explosions_entry = ChatColor.DARK_GREEN + translator.of("msg_perm_hud_explosions") + " ";
		String firespread_entry = ChatColor.DARK_GREEN + translator.of("msg_perm_hud_firespread") + " ";
		String mobspawn_entry = ChatColor.DARK_GREEN + translator.of("msg_perm_hud_mobspawns") + " ";
		String keyTitle_entry = ChatColor.YELLOW + "" + ChatColor.UNDERLINE + translator.of("msg_perm_hud_key");
		String keyResident_entry = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "f" + ChatColor.WHITE + " - " + ChatColor.GRAY + translator.of("msg_perm_hud_friend") + ChatColor.DARK_GREEN + " " + ChatColor.BOLD + "r" + ChatColor.WHITE + " - " + ChatColor.GRAY + translator.of("msg_perm_hud_resident");
		String keyNation_entry = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "t" + ChatColor.WHITE + " - " + ChatColor.GRAY + translator.of("msg_perm_hud_town") + ChatColor.DARK_GREEN + " " + ChatColor.BOLD + "n" + ChatColor.WHITE + " - " + ChatColor.GRAY + translator.of("msg_perm_hud_nation");
		String keyAlly_entry = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "a" + ChatColor.WHITE + " - " + ChatColor.GRAY + translator.of("msg_perm_hud_ally") + ChatColor.DARK_GREEN + " " + ChatColor.BOLD + "o" + ChatColor.WHITE + " - " + ChatColor.GRAY + translator.of("msg_perm_hud_outsider");

		//init objective
		Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective obj = BukkitTools.objective(board, "PERM_HUD_OBJ", PERM_HUD_TITLE);
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);
		obj.setDisplayName(PERM_HUD_TITLE);
		//register teams
		Team permsTitle = board.registerNewTeam("permsTitle");
		Team plotName = board.registerNewTeam("plot");
		Team build = board.registerNewTeam("build");
		Team destroy = board.registerNewTeam("destroy");
		Team switching = board.registerNewTeam("switching");
		Team item = board.registerNewTeam("item");
		Team keyPlotType = board.registerNewTeam("plotType");
		Team pvp = board.registerNewTeam("pvp");
		Team explosions = board.registerNewTeam("explosions");
		Team firespread = board.registerNewTeam("firespread");
		Team mobspawn= board.registerNewTeam("mobspawn");
		Team keyTitle = board.registerNewTeam("keyTitle");
		Team keyResident = board.registerNewTeam("keyResident");
		Team keyFriend = board.registerNewTeam("keyFriend");
		Team keyAlly = board.registerNewTeam("keyAlly");

		//register players
		permsTitle.addEntry(permsTitle_entry);
		plotName.addEntry(plotName_entry);
		build.addEntry(build_entry);
		destroy.addEntry(destroy_entry);
		switching.addEntry(switching_entry);
		item.addEntry(item_entry);
		keyPlotType.addEntry(keyPlotType_entry);
		pvp.addEntry(pvp_entry);
		explosions.addEntry(explosions_entry);
		firespread.addEntry(firespread_entry);
		mobspawn.addEntry(mobspawn_entry);
		keyTitle.addEntry(keyTitle_entry);
		keyResident.addEntry(keyResident_entry);
		keyFriend.addEntry(keyNation_entry);
		keyAlly.addEntry(keyAlly_entry);

		//set scores for positioning
		obj.getScore(permsTitle_entry).setScore(15);
		obj.getScore(plotName_entry).setScore(14);
		obj.getScore(build_entry).setScore(13);
		obj.getScore(destroy_entry).setScore(12);
		obj.getScore(switching_entry).setScore(11);
		obj.getScore(item_entry).setScore(10);
		obj.getScore(pvp_entry).setScore(8);
		obj.getScore(keyPlotType_entry).setScore(9);
		obj.getScore(explosions_entry).setScore(7);
		obj.getScore(firespread_entry).setScore(6);
		obj.getScore(mobspawn_entry).setScore(5);
		obj.getScore(keyTitle_entry).setScore(4);
		obj.getScore(keyResident_entry).setScore(3);
		obj.getScore(keyNation_entry).setScore(2);
		obj.getScore(keyAlly_entry).setScore(1);
		
		//set the board
		p.setScoreboard(board);
		updatePerms(p);
	}
}
