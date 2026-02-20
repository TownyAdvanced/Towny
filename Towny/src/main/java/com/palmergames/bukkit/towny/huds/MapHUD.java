package com.palmergames.bukkit.towny.huds;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyAsciiMap;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.asciimap.WildernessMapEvent;
import com.palmergames.bukkit.towny.huds.providers.HUD;
import com.palmergames.bukkit.towny.huds.providers.ServerHUD;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.map.TownyMapData;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;

import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class MapHUD implements HUDImplementer {

	final HUD hud;
	private static final int mapLineWidth = 19, mapLineHeight = 10;
	private static final int halfMapLineWidth = mapLineWidth/2;
	private static final int halfMapLineHeight = mapLineHeight/2;

	public MapHUD(HUD hud) {
		this.hud = hud;
	}

	@Override
	public HUD getHUD() {
		return hud;
	}

	public static void updateMap(Player player) {
		updateMap(player, WorldCoord.parseWorldCoord(player));
	}
	
	public static void updateMap(Player player, WorldCoord wc) {
		Translator translator = Translator.locale(player);
		ServerHUD hud = HUDManager.getHUD("mapHUD");
		if (hud == null) {
			Towny.getPlugin().getLogger().warning("MapHUD could not find mapHUD from HUDManager for player: " + player.getName());
			return;
		}

		int wcX = wc.getX();
		int wcZ = wc.getZ();
		// Set the board title.
		UUID uuid = player.getUniqueId();
		hud.setTitle(uuid, TownyComponents.miniMessage(String.format("%sTowny Map %s(%s, %s)", Colors.GOLD, Colors.WHITE, wcX, wcZ)));

		// Populate our map into an array.
		String[][] map = new String[mapLineWidth][mapLineHeight];
		fillMapArray(wcX, wcZ, TownyAPI.getInstance().getResident(player.getName()), player.getWorld(), map);

		LinkedList<Component> sbComponents = new LinkedList<>();
		for (int my = 0; my < mapLineHeight; my++) {
			String line = "";
			for (int mx = mapLineWidth - 1; mx >= 0; mx--)
				line += map[mx][my];

			sbComponents.add(TownyComponents.miniMessage(line));
		}

		TownBlock tb = wc.getTownBlockOrNull();
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("town_sing") + ": "
					+ (tb != null && tb.hasTown() ? tb.getTownOrNull().getName() : translator.of("status_no_town"))));
		if (tb != null) {
			sbComponents.add(getOwnerName(tb, translator));
			sbComponents.add(getDistrictName(tb, translator));
			sbComponents.add(getPlotName(tb, translator));
		}
		hud.setLines(uuid, sbComponents);
	}

	private static Component getOwnerName(TownBlock tb, Translator translator) {
		String name = !tb.hasResident() ? "" : tb.getResidentOrNull().getName();
		String prefix = translator.of("owner_status");
		return TownyComponents.miniMessage(HUDManager.check(Colors.DARK_GREEN + prefix + ": " + Colors.GREEN + name));
	}

	private static Component getDistrictName(TownBlock tb, Translator translator) {
		String name = !tb.hasDistrict() ? "" : tb.getDistrict().getFormattedName();
		String prefix = translator.of("msg_map_hud_district");
		return TownyComponents.miniMessage(HUDManager.check(Colors.DARK_GREEN + prefix + Colors.GREEN + name));
	}

	private static Component getPlotName(TownBlock tb, Translator translator) {
		String name = "";
		boolean hasPlotGroup = tb.hasPlotObjectGroup();
		if (hasPlotGroup && !tb.getPlotObjectGroup().getName().isEmpty())
			name = tb.getPlotObjectGroup().getFormattedName();
		if (!hasPlotGroup && !tb.getName().isEmpty())
			name = tb.getFormattedName();

		String prefix = translator.of(hasPlotGroup ? "msg_perm_hud_plotgroup_name" : "msg_perm_hud_plot_name");
		return TownyComponents.miniMessage(HUDManager.check(Colors.DARK_GREEN + prefix + Colors.GREEN + name));
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
			return Colors.GOLD;
		else if (townBlock.hasResident(resident))
			// Resident's own plot
			return Colors.YELLOW;
		else if (townBlock.getData().hasColour())
			// Set the colour of the townblocktype if it has one.
			return "<" + townBlock.getData().getColour() + ">";
		else if (resident.hasTown())
			// The townblock could have a variety of colours.
			return getTownBlockColour(resident, townBlock.getTownOrNull());
		else
			// Default fallback.
			return Colors.WHITE;
	}

	private static String getTownBlockColour(Resident resident, Town townAtTownBlock) {
		// The player is a part of this town.
		if (townAtTownBlock.hasResident(resident))
			return Colors.GREEN;

		if (!resident.hasNation())
			return Colors.WHITE;

		Nation resNation = resident.getNationOrNull();
		// Another town in the player's nation.
		if (resNation.hasTown(townAtTownBlock))
			return Colors.DARK_GREEN;

		if (!townAtTownBlock.hasNation())
			return Colors.WHITE;

		Nation townBlockNation = townAtTownBlock.getNationOrNull();
		if (resNation.hasAlly(townBlockNation))
			return Colors.DARK_GREEN;
		else if (resNation.hasEnemy(townBlockNation))
			return Colors.DARK_RED;
		else 
			return Colors.WHITE;
	}

	private static boolean playerLocatedAtThisCoord(int x, int y) {
		return x == halfMapLineHeight && y == halfMapLineWidth;
	}

	private static boolean isForSale(final TownBlock townBlock) {
		return townBlock.getPlotPrice() != -1 || townBlock.hasPlotObjectGroup() && townBlock.getPlotObjectGroup().getPrice() != -1;
	}


	private static void mapWilderness(String[][] map, int x, int y, final WorldCoord worldCoord) {
		// Colour gold if this is the player loc, otherwise normal gray.
		map[y][x] = playerLocatedAtThisCoord(x, y) ?  Colors.GOLD : Colors.DARK_GRAY;

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
//
//	private static void writeMapToBoard(Scoreboard board, String[][] map) {
//		for (int my = 0; my < mapLineHeight; my++) {
//			String line = "";
//			for (int mx = mapLineWidth - 1; mx >= 0; mx--)
//				line += map[mx][my];
//
//			board.getTeam(TEAM_MAP_PREFIX + my).setSuffix(line);
//		}
//	}
	
}
