package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.map.TownyMapData;

import java.util.Map;

import com.palmergames.bukkit.towny.utils.TownyComponents;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import com.palmergames.bukkit.towny.event.asciimap.WildernessMapEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.Compass;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.Component.*;

public class TownyAsciiMap {

	public static final int lineWidth = 27;
	public static final int halfLineWidth = lineWidth / 2;
	private static final int townBlockSize = TownySettings.getTownBlockSize();
	public static String forSaleSymbol = ConfigNodes.ASCII_MAP_SYMBOLS_FORSALE.getDefault();
	public static String homeSymbol = ConfigNodes.ASCII_MAP_SYMBOLS_HOME.getDefault();
	public static String wildernessSymbol = ConfigNodes.ASCII_MAP_SYMBOLS_WILDERNESS.getDefault();
	
	static {
		TownySettings.addReloadListener(NamespacedKey.fromString("towny:ascii-map-symbols"), config -> {
			forSaleSymbol = parseSymbol(TownySettings.forSaleMapSymbol());
			homeSymbol = parseSymbol(TownySettings.homeBlockMapSymbol());
			wildernessSymbol = parseSymbol(TownySettings.wildernessMapSymbol());
		});
	}
	
	public static Component[] generateHelp(Player player) {
		final Translator translator = Translator.locale(player);
		
		return new Component[] {
			text("  ").append(text("-", DARK_GRAY)).append(text(" = ", GRAY)).append(translator.component("towny_map_unclaimed").color(GRAY)),
			text("  ").append(text("+", WHITE)).append(text(" = ", GRAY)).append(translator.component("towny_map_claimed").color(GRAY)),
			text("  ").append(text("$", WHITE)).append(text(" = ", GRAY)).append(translator.component("towny_map_forsale").color(GRAY)),
			text("  ").append(text("+", GREEN)).append(text(" = ", GRAY)).append(translator.component("towny_map_yourtown").color(GRAY)),
			text("  ").append(text("+", YELLOW)).append(text(" = ", GRAY)).append(translator.component("towny_map_yourplot").color(GRAY)),
			text("  ").append(text("+", DARK_GREEN)).append(text(" = ", GRAY)).append(translator.component("towny_map_ally").color(GRAY)),
			text("  ").append(text("+", DARK_RED)).append(text(" = ", GRAY)).append(translator.component("towny_map_enemy").color(GRAY)),
		};
	}

	public static Component[] generateCompass(Player player) {

		Compass.Point dir = Compass.getCompassPointForDirection(player.getLocation().getYaw());

		return new Component[] {
			text("  -----  ", BLACK),
			text("  -", BLACK).append(text(dir == Compass.Point.NW ? "\\" : "-", dir == Compass.Point.NW ? GOLD : BLACK)).append(text("N", dir == Compass.Point.N ? GOLD : WHITE)).append(text(dir == Compass.Point.NE ? "/" : "-", dir == Compass.Point.NE ? GOLD : BLACK)).append(text("-  ", BLACK)),
			text("  -", BLACK).append(text( "W", dir == Compass.Point.W ? GOLD : WHITE)).append(text("+", GRAY)).append(text("E", dir == Compass.Point.E ? GOLD : WHITE)).append(text("-  ", BLACK)),
			text("  -", BLACK).append(text(dir == Compass.Point.SW ? "/" : "-", dir == Compass.Point.SW ? GOLD : BLACK)).append(text("S", dir == Compass.Point.S ? GOLD : WHITE)).append(text(dir == Compass.Point.SE ? "\\" : "-", dir == Compass.Point.SE ? GOLD : BLACK)).append(text("-  ", BLACK))
		};
	}

	public static void generateAndSend(Towny plugin, Player player, int lineHeight) {

		// Collect Sample Data
		Resident resident = TownyAPI.getInstance().getResident(player);
		
		if (resident == null) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered"));
			return;
		}

		TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld());
		if (world == null) { 
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_configured"));
			return;
		}
		
		if (!world.isUsingTowny()) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_set_use_towny_off"));
			return;
		}
		
		Coord pos = Coord.parseCoord(plugin.getCache(player).getLastLocation());
		final Translator translator = Translator.locale(player);

		// Generate Map 
		int halfLineHeight = lineHeight / 2;
		TextComponent[][] townyMap = new TextComponent[lineWidth][lineHeight];
		int x, y = 0;
		for (int tby = pos.getX() + (lineWidth - halfLineWidth - 1); tby >= pos.getX() - halfLineWidth; tby--) {
			x = 0;
			for (int tbx = pos.getZ() - halfLineHeight; tbx <= pos.getZ() + (lineHeight - halfLineHeight - 1); tbx++) {
				try {
					townyMap[y][x] = Component.empty().color(NamedTextColor.WHITE);
					WorldCoord wc = new WorldCoord(world.getBukkitWorld(), new Coord(tby, tbx));
					if (wc.isWilderness())
						throw new TownyException();
					TownBlock townblock = wc.getTownBlockOrNull();
					Town town = townblock.getTownOrNull();
					if (x == halfLineHeight && y == halfLineWidth)
						// This is the player's location, colour it special.
						townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GOLD);
					else if (townblock.hasResident(resident))
						//own plot
						townyMap[y][x] = townyMap[y][x].color(NamedTextColor.YELLOW);
					else if (resident.hasTown())
						if (town.hasResident(resident)) {
								// own town
								townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GREEN);
						} else if (resident.hasNation()) {
							Nation resNation = resident.getNationOrNull();
							if (resNation.hasTown(town))
								// own nation
								townyMap[y][x] = townyMap[y][x].color(NamedTextColor.DARK_GREEN);
							else if (town.hasNation()) {
								Nation townBlockNation = town.getNationOrNull();
								if (resNation.hasAlly(townBlockNation))
									townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GREEN);
								else if (resNation.hasEnemy(townBlockNation))
									townyMap[y][x] = townyMap[y][x].color(NamedTextColor.DARK_RED);
							}
						}

					// Registered town block
					if (townblock.getPlotPrice() != -1 || townblock.hasPlotObjectGroup() && townblock.getPlotObjectGroup().getPrice() != -1) {
						// override the colour if it's a shop plot for sale
						if (townblock.getType().equals(TownBlockType.COMMERCIAL))
							townyMap[y][x] = townyMap[y][x].color(NamedTextColor.BLUE);
						townyMap[y][x] = townyMap[y][x].content(forSaleSymbol);
					} else if (townblock.isHomeBlock())
						townyMap[y][x] = townyMap[y][x].content(homeSymbol);
					else
						townyMap[y][x] = townyMap[y][x].content(townblock.getType().getAsciiMapKey());
					
					Component residentComponent = Component.empty();
					Component forSaleComponent = Component.empty();
					Component claimedAtComponent = Component.empty();
					Component groupComponent = Component.empty();
					
					if (TownyEconomyHandler.isActive()) {
						double cost = townblock.hasPlotObjectGroup() 
							? townblock.getPlotObjectGroup().getPrice()
							: townblock.getPlotPrice();
						if (cost > -1)
							forSaleComponent = TownyComponents.miniMessage(String.format(ChunkNotification.forSaleNotificationFormat, TownyEconomyHandler.getFormattedBalance(cost)).replaceAll("[\\[\\]]", "") + " " + translator.of("msg_click_purchase")).color(NamedTextColor.YELLOW).append(Component.newline());
					}
					
					if (townblock.getClaimedAt() > 0)
						claimedAtComponent = translator.component("msg_plot_perm_claimed_at").color(NamedTextColor.DARK_GREEN)
							.append(Component.space())
							.append(Component.text(TownyFormatter.registeredFormat.format(townblock.getClaimedAt()), NamedTextColor.GREEN))
							.append(Component.newline());
					
					if (townblock.hasPlotObjectGroup())
						groupComponent = translator.component("map_hover_plot_group").color(NamedTextColor.DARK_GREEN)
							.append(Component.text(townblock.getPlotObjectGroup().getFormattedName(), NamedTextColor.GREEN)
							.append(translator.component("map_hover_plot_group_size").color(NamedTextColor.DARK_GREEN)
							.append(translator.component("map_hover_plots", townblock.getPlotObjectGroup().getTownBlocks().size()).color(NamedTextColor.GREEN)
							.append(Component.newline()))));
					
					if (townblock.hasResident())
						residentComponent = Component.text(" (" + townblock.getResidentOrNull().getName() + ")", NamedTextColor.GREEN);
					
					Component townComponent = translator.component("status_town").color(NamedTextColor.DARK_GREEN)
						.append(Component.space())
						.append(Component.text(town.getName(), NamedTextColor.GREEN))
						.append(residentComponent)
						.append(Component.text(" (" + tby + ", " + tbx + ")", NamedTextColor.WHITE))
						.append(Component.newline()); 
					
					Component plotTypeComponent = translator.component("status_plot_type").color(NamedTextColor.DARK_GREEN)
						.append(Component.space())
						.append(Component.text(townblock.getType().getName(), NamedTextColor.GREEN))
						.append(Component.newline());
					
					Component hoverComponent = townComponent
						.append(plotTypeComponent)
						.append(groupComponent)
						.append(forSaleComponent)
						.append(claimedAtComponent)
						.append(translator.component("towny_map_detailed_information").color(NamedTextColor.DARK_GREEN));
					
					ClickEvent clickEvent = forSaleComponent.equals(Component.empty()) 
						? ClickEvent.runCommand("/towny:plot info " + tby + " " + tbx)
						: ClickEvent.runCommand("/towny:plot claim " + world.getName() + " x" + tby + " z" + tbx);
					
					townyMap[y][x] = townyMap[y][x].hoverEvent(HoverEvent.showText(hoverComponent)).clickEvent(clickEvent);
				} catch (TownyException e) {
					// Unregistered town block (Wilderness)

					if (x == halfLineHeight && y == halfLineWidth)
						townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GOLD);
					else
						townyMap[y][x] = townyMap[y][x].color(NamedTextColor.DARK_GRAY);

					WorldCoord wc = WorldCoord.parseWorldCoord(world.getName(), tby * townBlockSize , tbx* townBlockSize);
					String symbol;
					TextComponent hoverText;
					String clickCommand;
					// Cached TownyMapData is present and not old.
					if (getWildernessMapDataMap().containsKey(wc) && !getWildernessMapDataMap().get(wc).isOld()) {
						TownyMapData mapData = getWildernessMapDataMap().get(wc);
						symbol = mapData.getSymbol();
						hoverText = mapData.getHoverText();
						clickCommand = mapData.getClickCommand();
					// Cached TownyMapData is either not present or was considered old.
					} else {
						if (getWildernessMapDataMap().containsKey(wc))
							getWildernessMapDataMap().remove(wc);
						WildernessMapEvent wildMapEvent = new WildernessMapEvent(wc);
						BukkitTools.fireEvent(wildMapEvent);
						symbol = wildMapEvent.getMapSymbol();
						hoverText = wildMapEvent.getHoverText();
						clickCommand = wildMapEvent.getClickCommand();
						getWildernessMapDataMap().put(wc, new TownyMapData(wc, symbol, hoverText, clickCommand));
						
						Bukkit.getScheduler().runTaskLater(Towny.getPlugin(), () -> {
							if (getWildernessMapDataMap().containsKey(wc) && getWildernessMapDataMap().get(wc).isOld())
								getWildernessMapDataMap().remove(wc);
						}, 20 * 35);
					}

					townyMap[y][x] = townyMap[y][x].content(symbol)
							.clickEvent(ClickEvent.runCommand(clickCommand))
							.hoverEvent(HoverEvent.showText(hoverText));
				}
				x++;
			}
			y++;
		}

		Component[] compass = generateCompass(player);

		// Output
		TownyMessaging.sendMessage(player, ChatTools.formatTitle(translator.of("towny_map_header") + Colors.White + "(" + pos + ")"));
		Component[] map = new Component[lineHeight];
		Component[] help = generateHelp(player);
		
		int lineCount = 0;
		// Variables have been rotated to fit N/S/E/W properly
		for (int my = 0; my < lineHeight; my++) {
			map[lineCount] = Component.empty().append(compass[lineCount < compass.length ? lineCount : 0]);

			for (int mx = lineWidth - 1; mx >= 0; mx--)
				map[lineCount] = map[lineCount].append(townyMap[mx][my]);

			if (lineCount < help.length)
				map[lineCount] = map[lineCount].append(help[lineCount]);

			lineCount++;
		}
		
		for (Component component : map)
			Towny.getAdventure().player(player).sendMessage(component);

		TownBlock townblock = TownyAPI.getInstance().getTownBlock(plugin.getCache(player).getLastLocation());
		TownyMessaging.sendMsg(player, translator.of("status_towny_map_town_line", 
				(townblock != null && townblock.hasTown() ? townblock.getTownOrNull() : translator.of("status_no_town")), 
				(townblock != null && townblock.hasResident() ? townblock.getResidentOrNull() : translator.of("status_no_town"))));
	}
	
	private static Map<WorldCoord, TownyMapData> getWildernessMapDataMap() {
		return TownyUniverse.getInstance().getWildernessMapDataMap();
	}

	public static String parseSymbol(String symbol) {
		if (symbol.startsWith("\\u"))
			return parseUnicode(symbol);
//			return symbol.length() > 6 ? parseSupplementaryUnicode(symbol) : StringEscapeUtils.unescapeJava(symbol);
		else 
			return symbol.substring(0, 1);

	}

	private static String parseUnicode(String symbol) {
		// remove the "\\u" before we get the resulting unicode symbol.
		return String.valueOf(Character.toChars(Integer.parseInt(symbol.substring(2))));
	}
}
