package com.palmergames.bukkit.towny.huds;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.asciimap.WildernessMapEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.map.TownyMapData;

import net.kyori.adventure.text.TextComponent;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class MapHUD {
	private static int lineWidth = 19, lineHeight = 10;
	private static final int townBlockSize = TownySettings.getTownBlockSize();
	
	public static void toggleOn(Player player) {
		Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective objective = board.registerNewObjective("MAP_HUD_OBJ", "dummy", "maphud");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		int score = lineHeight + 2;
		ChatColor[] colors = ChatColor.values();
		for (int i = 0; i < lineHeight; i++) {
			board.registerNewTeam("mapTeam" + i).addEntry(colors[i].toString());
			objective.getScore(colors[i].toString()).setScore(score);
			score--;
		}
		
		String townEntry = ChatColor.DARK_GREEN + Translatable.of("town_sing").forLocale(player) + ": ";
		String ownerEntry = ChatColor.DARK_GREEN + Translatable.of("owner_status").forLocale(player) + ": ";
		
		board.registerNewTeam("townTeam").addEntry(townEntry);
		objective.getScore(townEntry).setScore(2);
		
		board.registerNewTeam("ownerTeam").addEntry(ownerEntry);
		objective.getScore(ownerEntry).setScore(1);
		
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
		} else if (board.getObjective("MAP_HUD_OBJ") == null) {
			HUDManager.toggleOff(player);
			return;
		}

		Objective objective = board.getObjective("MAP_HUD_OBJ");
		objective.setDisplayName(ChatColor.GOLD + "Towny Map " + ChatColor.WHITE + "(" + wc.getX() + ", " + wc.getZ() + ")");

		TownyWorld world = wc.getTownyWorldOrNull();
		Resident resident = TownyAPI.getInstance().getResident(player.getName());
		if (world == null || !world.isUsingTowny() || resident == null) {
			HUDManager.toggleOff(player);
			return;
		}
		
		boolean hasTown = resident.hasTown();

		int halfLineWidth = lineWidth/2;
		int halfLineHeight = lineHeight/2;
		
		String[][] map = new String[lineWidth][lineHeight];
		int x, y = 0;
		for (int tby = wc.getX() + (lineWidth - halfLineWidth - 1); tby >= wc.getX() - halfLineWidth; tby--) {
			x = 0;
			for (int tbx = wc.getZ() - halfLineHeight; tbx <= wc.getZ() + (lineHeight - halfLineHeight - 1); tbx++) {
				map[y][x] = ChatColor.WHITE.toString();
				try {
					TownBlock townblock = world.getTownBlock(tby, tbx);
					if (!townblock.hasTown())
						throw new TownyException();
					if (x == halfLineHeight && y == halfLineWidth)
						// location
						map[y][x] = ChatColor.GOLD.toString();
					else if (hasTown) {
						if (resident.getTown() == townblock.getTown()) {
							// own town
							map[y][x] = ChatColor.GREEN.toString();
							try {
								if (resident == townblock.getResident())
									//own plot
									map[y][x] = ChatColor.YELLOW.toString();
							} catch (NotRegisteredException e) {
							}
						} else if (resident.hasNation()) {
							if (resident.getTown().getNation().hasTown(townblock.getTown()))
								// towns
								map[y][x] = ChatColor.DARK_GREEN.toString();
							else if (townblock.getTown().hasNation()) {
								Nation nation = resident.getTown().getNation();
								if (nation.hasAlly(townblock.getTown().getNation()))
									map[y][x] = ChatColor.DARK_GREEN.toString();
								else if (nation.hasEnemy(townblock.getTown().getNation()))
									// towns
									map[y][x] = ChatColor.DARK_RED.toString();
							}
						}
					}

					// Registered town block
					if (townblock.getPlotPrice() != -1 || townblock.hasPlotObjectGroup() && townblock.getPlotObjectGroup().getPrice() != -1) {
						// override the colour if it's a shop plot for sale
						if (townblock.getType().equals(TownBlockType.COMMERCIAL))
							map[y][x] = ChatColor.DARK_AQUA.toString();
						map[y][x] += "$";
					} else if (townblock.isHomeBlock())
						map[y][x] += "H";
					else
						map[y][x] += townblock.getType().getAsciiMapKey();
				} catch (TownyException e) {
					// Unregistered town block
					
					if (x == halfLineHeight && y == halfLineWidth)
						map[y][x] = ChatColor.GOLD.toString();
					else
						map[y][x] = ChatColor.DARK_GRAY.toString();

					WorldCoord worldcoord = WorldCoord.parseWorldCoord(world.getName(), tby * townBlockSize , tbx* townBlockSize);
					String symbol;
					TextComponent hoverText; 
					String clickCommand;
					// Cached TownyMapData is present and not old.
					if (getWildernessMapDataMap().containsKey(worldcoord) && !getWildernessMapDataMap().get(worldcoord).isOld()) {
						TownyMapData mapData = getWildernessMapDataMap().get(worldcoord);
						symbol = mapData.getSymbol();
						hoverText = mapData.getHoverText();
						clickCommand = mapData.getClickCommand();
					// Cached TownyMapData is either not present or was considered old.
					} else {
						if (getWildernessMapDataMap().containsKey(worldcoord))
							getWildernessMapDataMap().remove(worldcoord);
						WildernessMapEvent wildMapEvent = new WildernessMapEvent(worldcoord);
						Bukkit.getPluginManager().callEvent(wildMapEvent);
						symbol = wildMapEvent.getMapSymbol();
						hoverText = wildMapEvent.getHoverText();
						clickCommand = wildMapEvent.getClickCommand();
						getWildernessMapDataMap().put(worldcoord, new TownyMapData(worldcoord, symbol, hoverText, clickCommand));
						
						Bukkit.getScheduler().runTaskLater(Towny.getPlugin(), ()-> {
							if (getWildernessMapDataMap().containsKey(worldcoord) && getWildernessMapDataMap().get(worldcoord).isOld())
								getWildernessMapDataMap().remove(worldcoord);
						}, 20 * 35);
					}

					/* 
					 * We are only using symbol here but we have generated hovertext and clickcommands because the same
					 * TownyMapData cache is used for the ascii map seen in the /towny map commands. We would not want
					 * to fill only a part of that cache.
					 */
					map[y][x] += symbol;
				}
				x++;
			}
			y++;
		}
		
		for (int my = 0; my < lineHeight; my++) {
			String line = "";
			for (int mx = lineWidth - 1; mx >= 0; mx--)
				line += map[mx][my];

			board.getTeam("mapTeam" + my).setSuffix(line);
		}
		
		TownBlock tb = wc.getTownBlockOrNull();
		board.getTeam("townTeam").setSuffix(ChatColor.GREEN + (tb != null && tb.hasTown() ? tb.getTownOrNull().getName() : Translatable.of("status_no_town").forLocale(player)));
		board.getTeam("ownerTeam").setSuffix(ChatColor.GREEN + (tb != null && tb.hasResident() ? tb.getResidentOrNull().getName() : Translatable.of("status_no_town").forLocale(player)));
	}
	
	private static Map<WorldCoord, TownyMapData> getWildernessMapDataMap() {
		return TownyUniverse.getInstance().getWildernessMapDataMap();
	}
}
