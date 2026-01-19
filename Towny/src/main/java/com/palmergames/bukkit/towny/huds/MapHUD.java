package com.palmergames.bukkit.towny.huds;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyAsciiMap;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.asciimap.WildernessMapEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.map.TownyMapData;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;

import java.util.Map;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class MapHUD {

	/* Scoreboards use old-timey colours. */
	private static final ChatColor WHITE = ChatColor.WHITE;
	private static final ChatColor GOLD = ChatColor.GOLD;
	private static final ChatColor YELLOW = ChatColor.YELLOW;
	private static final ChatColor LIGHT_PURPLE = ChatColor.LIGHT_PURPLE;
	private static final ChatColor RED = ChatColor.RED;
	private static final ChatColor GREEN = ChatColor.GREEN;
	private static final ChatColor DARK_GREEN = ChatColor.DARK_GREEN;

	/* Scoreboards use Teams here is our team names.*/
	private static final String HUD_OBJECTIVE = "MAP_HUD_OBJ";
	private static final String TEAM_MAP_PREFIX = "mapTeam";
	private static final String TEAM_TOWN = "townTeam";
	private static final String TEAM_OWNER = "ownerTeam";
	private static final String TEAM_DISTRICT = "districtTeam";
	private static final String TEAM_PLOTNAME = "plotnameTeam";

	private static final int mapLineWidth = 19, mapLineHeight = 10;
	private static final int halfMapLineWidth = mapLineWidth/2;
	private static final int halfMapLineHeight = mapLineHeight/2;
	private static final int MAX_NON_MAP_LINES = HUDManager.MAX_SCOREBOARD_HEIGHT - mapLineHeight;
	
	public static String mapHudTestKey() {
		return "mapTeam1";
	}

	public static void toggleOn(Player player) {
		Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective objective = BukkitTools.objective(board, HUD_OBJECTIVE, "maphud");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		try {
			objective.numberFormat(NumberFormat.blank());
		} catch (NoSuchMethodError | NoClassDefFoundError ignored) {}
		
		// We have a number of lines below the map, this is how many.
		int nonMapLines = Math.max(4, MAX_NON_MAP_LINES);
		int score = mapLineHeight + nonMapLines;
		ChatColor[] colors = ChatColor.values();
		for (int i = 0; i < mapLineHeight; i++) {
			board.registerNewTeam(TEAM_MAP_PREFIX + i).addEntry(colors[i].toString());
			objective.getScore(colors[i].toString()).setScore(score);
			score--;
		}
		
		String townEntry = DARK_GREEN + Translatable.of("town_sing").forLocale(player) + ": ";
		// A blank entry has to have a unique, invisible character. The TEAM_MAP_PREFIX teams have taken colors 0-9.
		String ownerEntry = YELLOW.toString();
		String districtEntry = LIGHT_PURPLE.toString();
		String plotnameEntry = RED.toString();

		score = nonMapLines;
		board.registerNewTeam(TEAM_TOWN).addEntry(townEntry);
		objective.getScore(townEntry).setScore(score--);

		board.registerNewTeam(TEAM_OWNER).addEntry(ownerEntry);
		objective.getScore(ownerEntry).setScore(score--);

		board.registerNewTeam(TEAM_DISTRICT).addEntry(districtEntry);
		objective.getScore(districtEntry).setScore(score--);

		board.registerNewTeam(TEAM_PLOTNAME).addEntry(plotnameEntry);
		objective.getScore(plotnameEntry).setScore(score--);

		player.setScoreboard(board);
		updateMap(player);
	}
	
	public static void updateMap(Player player) {
		updateMap(player, WorldCoord.parseWorldCoord(player));
	}
	
	public static void updateMap(Player player, WorldCoord wc) {
		Scoreboard board = player.getScoreboard();
		if (board == null) {
			toggleOn(player);
			return;
		} else if (board.getObjective(HUD_OBJECTIVE) == null || wc.getTownyWorld() == null || !wc.getTownyWorld().isUsingTowny()) {
			HUDManager.toggleOff(player);
			return;
		}

		int wcX = wc.getX();
		int wcZ = wc.getZ();
		// Set the board title.
		String boardTitle = String.format("%sTowny Map %s(%s, %s)", GOLD, WHITE, wcX, wcZ);
		board.getObjective(HUD_OBJECTIVE).setDisplayName(boardTitle);

		// Populate our map into an array.
		String[][] map = new String[mapLineWidth][mapLineHeight];
		fillMapArray(wcX, wcZ, TownyAPI.getInstance().getResident(player.getName()), player.getWorld(), map);

		// Write out the map to the board.
		writeMapToBoard(board, map);

		TownBlock tb = wc.getTownBlockOrNull();
		board.getTeam(TEAM_TOWN).setSuffix(GREEN + (tb != null && tb.hasTown() ? tb.getTownOrNull().getName() : Translatable.of("status_no_town").forLocale(player)));
		board.getTeam(TEAM_OWNER).setSuffix(getOwnerName(tb, player));
		board.getTeam(TEAM_DISTRICT).setSuffix(getDistrictName(tb, player));
		board.getTeam(TEAM_PLOTNAME).setSuffix(getPlotName(tb, player));
	}

	private static String getOwnerName(TownBlock tb, Player player) {
		String name = "";
		if (tb == null)
			return name;
		name = !tb.hasResident() ? "" : tb.getResidentOrNull().getName();
		String prefix = Translatable.of("owner_status").forLocale(player);
		return HUDManager.check(DARK_GREEN + prefix + ": " + GREEN + name);
	}

	private static String getDistrictName(TownBlock tb, Player player) {
		String name = "";
		if (tb == null)
			return name;
		name = !tb.hasDistrict() ? "" : tb.getDistrict().getFormattedName();
		String prefix = Translatable.of("msg_map_hud_district").forLocale(player);
		return HUDManager.check(DARK_GREEN + prefix + GREEN + name);
	}

	private static String getPlotName(TownBlock tb, Player player) {
		String name = "";
		if (tb == null)
			return name;
		if (tb.hasPlotObjectGroup() && !tb.getPlotObjectGroup().getName().isEmpty())
			name = tb.getPlotObjectGroup().getFormattedName();
		if (!tb.hasPlotObjectGroup() && !tb.getName().isEmpty())
			name = tb.getFormattedName();

		String slug = tb.hasPlotObjectGroup() ? "msg_perm_hud_plotgroup_name" : "msg_perm_hud_plot_name";
		String prefix = Translatable.of(slug).forLocale(player);
		return HUDManager.check(DARK_GREEN + prefix + GREEN + name);
	}

	private static void fillMapArray(int wcX, int wcZ, Resident resident, World bukkitWorld, String[][] map) {
		int x, y = 0;
		for (int tby = wcX + (mapLineWidth - halfMapLineWidth - 1); tby >= wcX - halfMapLineWidth; tby--) {
			x = 0;
			for (int tbx = wcZ - halfMapLineHeight; tbx <= wcZ + (mapLineHeight - halfMapLineHeight - 1); tbx++) {
				final WorldCoord worldCoord = new WorldCoord(bukkitWorld, tby, tbx);
				if (worldCoord.hasTownBlock())
					mapTownBlock(resident, map, x, y, worldCoord.getTownBlockOrNull());
				else
					mapWilderness(map, x, y, worldCoord);
				x++;
			}
			y++;
		}
	}

	private static void mapTownBlock(Resident resident, String[][] map, int x, int y, final TownBlock townBlock) {
		// Set the townblock colour.
		map[y][x] = getTownBlockColour(resident, x, y, townBlock);

		// Set the townblock symbol.
		if (isForSale(townBlock))
			map[y][x] += TownyAsciiMap.forSaleSymbol;
		else if (townBlock.isHomeBlock())
			map[y][x] += TownyAsciiMap.homeSymbol;
		else if (townBlock.isOutpost())
			map[y][x] += TownyAsciiMap.outpostSymbol;
		else
			map[y][x] += townBlock.getType().getAsciiMapKey();
	}

	private static String getTownBlockColour(Resident resident, int x, int y, final TownBlock townBlock) {
		if (playerLocatedAtThisCoord(x, y))
			// This is the player's location, colour it special.
			return Colors.Gold;
		else if (townBlock.hasResident(resident))
			// Resident's own plot
			return Colors.Yellow;
		else if (townBlock.getData().hasColour())
			// Set the colour of the townblocktype if it has one.
			return Colors.getLegacyFromNamedTextColor(townBlock.getData().getColour());
		else if (resident.hasTown())
			// The townblock could have a variety of colours.
			return getTownBlockColour(resident, townBlock.getTownOrNull());
		else
			// Default fallback.
			return Colors.White;
	}

	private static String getTownBlockColour(Resident resident, Town townAtTownBlock) {
		// The player is a part of this town.
		if (townAtTownBlock.hasResident(resident))
			return Colors.LightGreen;

		if (!resident.hasNation())
			return Colors.White;

		Nation resNation = resident.getNationOrNull();
		// Another town in the player's nation.
		if (resNation.hasTown(townAtTownBlock))
			return Colors.Green;

		if (!townAtTownBlock.hasNation())
			return Colors.White;

		Nation townBlockNation = townAtTownBlock.getNationOrNull();
		if (resNation.hasAlly(townBlockNation))
			return Colors.Green;
		else if (resNation.hasEnemy(townBlockNation))
			return Colors.Red;
		else 
			return Colors.White;
	}

	private static boolean playerLocatedAtThisCoord(int x, int y) {
		return x == halfMapLineHeight && y == halfMapLineWidth;
	}

	private static boolean isForSale(final TownBlock townBlock) {
		return townBlock.getPlotPrice() != -1 || townBlock.hasPlotObjectGroup() && townBlock.getPlotObjectGroup().getPrice() != -1;
	}


	private static void mapWilderness(String[][] map, int x, int y, final WorldCoord worldCoord) {
		// Colour gold if this is the player loc, otherwise normal gray.
		map[y][x] = playerLocatedAtThisCoord(x, y) ?  Colors.Gold : Colors.Gray;

		String symbol;
		// Cached TownyMapData is present and not old.
		final TownyMapData data = getWildernessMapDataMap().get(worldCoord);
		
		if (data != null && !data.isOld()) {
			TownyMapData mapData = getWildernessMapDataMap().get(worldCoord);
			symbol = mapData.getSymbol();
		// Cached TownyMapData is either not present or was considered old.
		} else {
			WildernessMapEvent wildMapEvent = new WildernessMapEvent(worldCoord);
			BukkitTools.fireEvent(wildMapEvent);
			symbol = wildMapEvent.getMapSymbol();
			getWildernessMapDataMap().put(worldCoord, new TownyMapData(worldCoord, symbol, wildMapEvent.getHoverText(), wildMapEvent.getClickCommand()));
			
			Towny.getPlugin().getScheduler().runAsyncLater(() -> {
				getWildernessMapDataMap().computeIfPresent(worldCoord, (key, cachedData) -> cachedData.isOld() ? null : cachedData);
			}, 20 * 35);
		}

		/* 
		 * We are only using symbol here but we have generated hovertext and clickcommands because the same
		 * TownyMapData cache is used for the ascii map seen in the /towny map commands. We would not want
		 * to fill only a part of that cache.
		 */
		map[y][x] += symbol;
	}

	private static Map<WorldCoord, TownyMapData> getWildernessMapDataMap() {
		return TownyUniverse.getInstance().getWildernessMapDataMap();
	}

	private static void writeMapToBoard(Scoreboard board, String[][] map) {
		for (int my = 0; my < mapLineHeight; my++) {
			String line = "";
			for (int mx = mapLineWidth - 1; mx >= 0; mx--)
				line += map[mx][my];

			board.getTeam(TEAM_MAP_PREFIX + my).setSuffix(line);
		}
	}
	
}
