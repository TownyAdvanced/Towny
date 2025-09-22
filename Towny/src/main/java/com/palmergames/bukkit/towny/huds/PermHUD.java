package com.palmergames.bukkit.towny.huds;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
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

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class PermHUD {

	/* Scoreboards use old-timey colours. */
	private static final ChatColor DARK_RED = ChatColor.DARK_RED;
	private static final ChatColor WHITE = ChatColor.WHITE;
	private static final ChatColor GRAY = ChatColor.GRAY;
	private static final ChatColor YELLOW = ChatColor.YELLOW;
	private static final ChatColor DARK_GREEN = ChatColor.DARK_GREEN;
	private static final ChatColor GOLD = ChatColor.GOLD;
	private static final ChatColor BOLD = ChatColor.BOLD;
	private static final ChatColor UNDERLINE = ChatColor.UNDERLINE;

	/* Scoreboards use Teams here is our team names.*/
	private static final String HUD_OBJECTIVE = "PERM_HUD_OBJ";
	private static final String TEAM_PERMS_TITLE = "permsTitle";
	private static final String TEAM_PLOT_COST = "plot_cost";
	private static final String TEAM_BUILD = "build";
	private static final String TEAM_DESTROY = "destroy";
	private static final String TEAM_SWITCH = "switching";
	private static final String TEAM_ITEMUSE = "item";
	private static final String TEAM_PLOT_TYPE = "plotType";
	private static final String TEAM_PVP = "pvp";
	private static final String TEAM_EXPLOSIONS = "explosions";
	private static final String TEAM_FIRESPREAD = "firespread";
	private static final String TEAM_MOBSPAWNING = "mobspawn";
	private static final String TEAM_TITLE = "keyTitle";
	private static final String TEAM_RESIDENT = "keyResident";
	private static final String TEAM_FRIEND = "keyFriend";
	private static final String TEAM_ALLY = "keyAlly";

	public static void updatePerms (Player p) {
		updatePerms(p, WorldCoord.parseWorldCoord(p));
	}

	public static String permHudTestKey() {
		return TEAM_BUILD;
	}

	public static void updatePerms(Player p, WorldCoord worldCoord) {
		Translator translator = Translator.locale(p);
		String build, destroy, switching, item, type, pvp, explosions, firespread, mobspawn;
		Scoreboard board = p.getScoreboard();
		// Due to tick delay (probably not confirmed), a HUD can actually be removed from the player.
		// Causing board to return null, and since we don't create a new board, a NullPointerException occurs.
		// So we can call the toggleOn Method and return, causing this to be rerun and also the creation
		// of the scoreboard, at least that's the plan.
		if (board == null) {
			toggleOn(p);
			return;
		}

		if (board.getObjective(HUD_OBJECTIVE) == null) { // Some other plugin's scoreboard has  
			HUDManager.toggleOff(p);                     // likely been activated, meaning we
			return;                                      // will throw NPEs if we continue.
		}

		if (worldCoord.isWilderness()) {
			clearPerms(p);
			return;
		}

		TownBlock townBlock = worldCoord.getTownBlockOrNull();
		TownBlockOwner owner = townBlock.getTownBlockOwner();
		boolean plotGroup = townBlock.hasPlotObjectGroup();

		// Displays the name of the owner, and if the owner is a resident the town name as well.
		Component title = Component.text(owner.getName() + (townBlock.hasResident() ? " (" + townBlock.getTownOrNull().getName() + ")" : ""), NamedTextColor.GOLD); 

		// Plot Type
		type = townBlock.getType().equals(TownBlockType.RESIDENTIAL) ? " " : townBlock.getType().getName();

		// Plot Price or "No"
		String forSale = getPlotPrice(translator, townBlock, plotGroup);

		TownyPermission tp = townBlock.getPermissions();
		boolean residentOwned = owner instanceof Resident;
		build = getPermLine(tp, ActionType.BUILD, residentOwned);
		destroy = getPermLine(tp, ActionType.DESTROY, residentOwned);
		switching = getPermLine(tp, ActionType.SWITCH, residentOwned);
		item = getPermLine(tp, ActionType.ITEM_USE, residentOwned);

		TownyWorld world = townBlock.getWorld();
		pvp = getTranslatedOnOrOff(!CombatUtil.preventPvP(world, townBlock), translator);
		explosions = getTranslatedOnOrOff(world.isForceExpl() || tp.explosion, translator);
		firespread = getTranslatedOnOrOff(world.isForceFire() || tp.fire, translator);
		mobspawn = getTranslatedOnOrOff(world.isForceTownMobs() || tp.mobs || townBlock.getTownOrNull().isAdminEnabledMobs(), translator);


		// Set the values to our Scoreboard's teams.
		board.getObjective(HUD_OBJECTIVE).displayName(title);
		board.getTeam(TEAM_PLOT_TYPE).setSuffix(type);
		board.getTeam(TEAM_PLOT_COST).setSuffix(forSale);

		board.getTeam(TEAM_BUILD).setSuffix(build);
		board.getTeam(TEAM_DESTROY).setSuffix(destroy);
		board.getTeam(TEAM_SWITCH).setSuffix(switching);
		board.getTeam(TEAM_ITEMUSE).setSuffix(item);

		board.getTeam(TEAM_PVP).setSuffix(pvp);
		board.getTeam(TEAM_EXPLOSIONS).setSuffix(explosions);
		board.getTeam(TEAM_FIRESPREAD).setSuffix(firespread);
		board.getTeam(TEAM_MOBSPAWNING).setSuffix(mobspawn);
	}

	private static String getPlotPrice(Translator translator, TownBlock townBlock, boolean plotGroup) {
		String forSale = translator.of("msg_perm_hud_no");
		if (TownyEconomyHandler.isActive()) {
			forSale = plotGroup && townBlock.getPlotObjectGroup().getPrice() > -1
				? prettyMoney(townBlock.getPlotObjectGroup().getPrice())
				: townBlock.isForSale() ? prettyMoney(townBlock.getPlotPrice()) : forSale;
		} else {
			// No economy is active but plots can be put up for sale (they're just free.)
			forSale = (plotGroup && townBlock.getPlotObjectGroup().getPrice() > -1) || (!plotGroup && townBlock.isForSale())
					? translator.of("msg_perm_hud_yes") : forSale;
		}
		return forSale;
	}

	private static String getPermLine(TownyPermission tp, ActionType actionType, boolean residentOwned) {
		String v = residentOwned ? "f" : "r";
		String u = residentOwned ? "t" : "n";
		return (tp.getResidentPerm(actionType) ? v : "-") + (tp.getNationPerm(actionType) ? u : "-") +
				(tp.getAllyPerm(actionType) ? "a" : "-") + (tp.getOutsiderPerm(actionType) ? "o" : "-");
	}

	private static String getTranslatedOnOrOff(boolean test, Translator translator) {
		return test ? translator.of("status_on") : translator.of("status_off");
	}

	private static void clearPerms (Player p) {
		Scoreboard board = p.getScoreboard();
		try {
			board.getObjective(HUD_OBJECTIVE).setDisplayName(HUDManager.check(getFormattedWildernessName(p.getWorld())));
			board.getTeam(TEAM_PLOT_TYPE).setSuffix(" ");
			board.getTeam(TEAM_PLOT_COST).setSuffix(" ");

			board.getTeam(TEAM_BUILD).setSuffix(" ");
			board.getTeam(TEAM_DESTROY).setSuffix(" ");
			board.getTeam(TEAM_SWITCH).setSuffix(" ");
			board.getTeam(TEAM_ITEMUSE).setSuffix(" ");

			board.getTeam(TEAM_PVP).setSuffix(" ");
			board.getTeam(TEAM_EXPLOSIONS).setSuffix(" ");
			board.getTeam(TEAM_FIRESPREAD).setSuffix(" ");
			board.getTeam(TEAM_MOBSPAWNING).setSuffix(" ");
		} catch (NullPointerException e) {
			toggleOn(p);
		}
	}
	
	private static String getFormattedWildernessName(World w) {
		StringBuilder wildernessName = new StringBuilder().append(DARK_RED).append(BOLD);
		if (TownyAPI.getInstance().isTownyWorld(w)) 
			wildernessName.append(TownyAPI.getInstance().getTownyWorld(w).getFormattedUnclaimedZoneName());
		else 
			wildernessName.append("Unknown");

		return wildernessName.toString();
	}
	
	public static void toggleOn (Player p) {
		//init scoreboard
		Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		initializeScoreboard(Translator.locale(p), board);

		//set the board onto the player
		p.setScoreboard(board);

		//call for the board's values to be populated
		updatePerms(p);
	}

	private static void initializeScoreboard(Translator translator, Scoreboard board) {
		String keyPlotType_entry = DARK_GREEN + translator.of("msg_perm_hud_plot_type");
		String forSale_entry = DARK_GREEN + translator.of("msg_perm_hud_plot_for_sale") + GRAY;

		String permsTitle_entry = YELLOW + "" + UNDERLINE + translator.of("msg_perm_hud_title");
		String build_entry =   DARK_GREEN + translator.of("msg_perm_hud_build") + GRAY;
		String destroy_entry = DARK_GREEN + translator.of("msg_perm_hud_destroy") + GRAY;
		String switching_entry = DARK_GREEN + translator.of("msg_perm_hud_switch") + GRAY;
		String item_entry = DARK_GREEN + translator.of("msg_perm_hud_item_use") + GRAY;

		String pvp_entry = DARK_GREEN + translator.of("msg_perm_hud_pvp") + " ";
		String explosions_entry = DARK_GREEN + translator.of("msg_perm_hud_explosions") + " ";
		String firespread_entry = DARK_GREEN + translator.of("msg_perm_hud_firespread") + " ";
		String mobspawn_entry = DARK_GREEN + translator.of("msg_perm_hud_mobspawns") + " ";

		String keyTitle_entry = YELLOW + "" + UNDERLINE + translator.of("msg_perm_hud_key");
		String keyResident_entry = DARK_GREEN + "" + BOLD + "f" + WHITE + " - " + GRAY + translator.of("msg_perm_hud_friend") +
				DARK_GREEN + " " + BOLD + "r" + WHITE + " - " + GRAY + translator.of("msg_perm_hud_resident");
		String keyNation_entry = DARK_GREEN + "" + BOLD + "t" + WHITE + " - " + GRAY + translator.of("msg_perm_hud_town") +
				DARK_GREEN + " " + BOLD + "n" + WHITE + " - " + GRAY + translator.of("msg_perm_hud_nation");
		String keyAlly_entry = DARK_GREEN + "" + BOLD + "a" + WHITE + " - " + GRAY + translator.of("msg_perm_hud_ally") +
				DARK_GREEN + " " + BOLD + "o" + WHITE + " - " + GRAY + translator.of("msg_perm_hud_outsider");

		Objective obj = BukkitTools.objective(board, HUD_OBJECTIVE, "");
		obj.setDisplaySlot(DisplaySlot.SIDEBAR);

		try {
			obj.numberFormat(NumberFormat.blank());
		} catch (NoSuchMethodError | NoClassDefFoundError ignored) {}

		//register teams
		Team keyPlotType = board.registerNewTeam(TEAM_PLOT_TYPE);
		Team forSaleTitle = board.registerNewTeam(TEAM_PLOT_COST);

		Team permsTitle = board.registerNewTeam(TEAM_PERMS_TITLE);
		Team build = board.registerNewTeam(TEAM_BUILD);
		Team destroy = board.registerNewTeam(TEAM_DESTROY);
		Team switching = board.registerNewTeam(TEAM_SWITCH);
		Team item = board.registerNewTeam(TEAM_ITEMUSE);

		Team pvp = board.registerNewTeam(TEAM_PVP);
		Team explosions = board.registerNewTeam(TEAM_EXPLOSIONS);
		Team firespread = board.registerNewTeam(TEAM_FIRESPREAD);
		Team mobspawn= board.registerNewTeam(TEAM_MOBSPAWNING);

		Team keyTitle = board.registerNewTeam(TEAM_TITLE);
		Team keyResident = board.registerNewTeam(TEAM_RESIDENT);
		Team keyFriend = board.registerNewTeam(TEAM_FRIEND);
		Team keyAlly = board.registerNewTeam(TEAM_ALLY);

		//add each team as an entry (this sets the prefix to each line of the HUD.)
		keyPlotType.addEntry(keyPlotType_entry);
		forSaleTitle.addEntry(forSale_entry);

		permsTitle.addEntry(permsTitle_entry);
		build.addEntry(build_entry);
		destroy.addEntry(destroy_entry);
		switching.addEntry(switching_entry);
		item.addEntry(item_entry);

		pvp.addEntry(pvp_entry);
		explosions.addEntry(explosions_entry);
		firespread.addEntry(firespread_entry);
		mobspawn.addEntry(mobspawn_entry);

		keyTitle.addEntry(keyTitle_entry);
		keyResident.addEntry(keyResident_entry);
		keyFriend.addEntry(keyNation_entry);
		keyAlly.addEntry(keyAlly_entry);

		int score = HUDManager.MAX_SCOREBOARD_HEIGHT;
		//set scores for positioning
		obj.getScore(keyPlotType_entry).setScore(score--);
		obj.getScore(forSale_entry).setScore(score--);
		obj.getScore(permsTitle_entry).setScore(score--);
		obj.getScore(build_entry).setScore(score--);
		obj.getScore(destroy_entry).setScore(score--);
		obj.getScore(switching_entry).setScore(score--);
		obj.getScore(item_entry).setScore(score--);
		obj.getScore(pvp_entry).setScore(score--);
		obj.getScore(explosions_entry).setScore(score--);
		obj.getScore(firespread_entry).setScore(score--);
		obj.getScore(mobspawn_entry).setScore(score--);
		obj.getScore(keyTitle_entry).setScore(score--);
		obj.getScore(keyResident_entry).setScore(score--);
		obj.getScore(keyNation_entry).setScore(score--);
		obj.getScore(keyAlly_entry).setScore(score--);
	}

	private static String prettyMoney(double price) {
		return TownyEconomyHandler.getFormattedBalance(price);
	}
}
