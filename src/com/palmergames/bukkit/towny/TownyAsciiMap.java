package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.CombatUtil;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.Compass;

public class TownyAsciiMap {

	public static final int lineWidth = 27;
	public static final int halfLineWidth = lineWidth / 2;
	public static final String[] help = {
			"  " + Colors.Gray + "-" + Colors.LightGray + " = " + Translation.of("towny_map_unclaimed"),
			"  " + Colors.White + "+" + Colors.LightGray + " = " + Translation.of("towny_map_claimed"),
			"  " + Colors.White + "$" + Colors.LightGray + " = " + Translation.of("towny_map_forsale"),
			"  " + Colors.LightGreen + "+" + Colors.LightGray + " = " + Translation.of("towny_map_yourtown"),
			"  " + Colors.Yellow + "+" + Colors.LightGray + " = " + Translation.of("towny_map_yourplot"),
			"  " + Colors.Green + "+" + Colors.LightGray + " = " + Translation.of("towny_map_ally"),
			"  " + Colors.Red + "+" + Colors.LightGray + " = " + Translation.of("towny_map_enemy")};

	public static String[] generateCompass(Player player) {

		Compass.Point dir = Compass.getCompassPointForDirection(player.getLocation().getYaw());

		return new String[] {
				Colors.Black + "  -----  ",
				Colors.Black + "  -" + (dir == Compass.Point.NW ? Colors.Gold + "\\" : "-") + (dir == Compass.Point.N ? Colors.Gold : Colors.White) + "N" + (dir == Compass.Point.NE ? Colors.Gold + "/" + Colors.Black : Colors.Black + "-") + "-  ",
				Colors.Black + "  -" + (dir == Compass.Point.W ? Colors.Gold + "W" : Colors.White + "W") + Colors.LightGray + "+" + (dir == Compass.Point.E ? Colors.Gold : Colors.White) + "E" + Colors.Black + "-  ",
				Colors.Black + "  -" + (dir == Compass.Point.SW ? Colors.Gold + "/" : "-") + (dir == Compass.Point.S ? Colors.Gold : Colors.White) + "S" + (dir == Compass.Point.SE ? Colors.Gold + "\\" + Colors.Black : Colors.Black + "-") + "-  " };
	}

	public static void generateAndSend(Towny plugin, Player player, int lineHeight) {

		// Collect Sample Data
		boolean hasTown = false;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		Resident resident = townyUniverse.getResident(player.getUniqueId());
		
		if (resident == null) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_registered"));
			return;
		}
		
		if (resident.hasTown())
			hasTown = true;

		TownyWorld world;
		try {
			world = townyUniverse.getDataSource().getWorld(player.getWorld().getName());
		} catch (NotRegisteredException e1) {
			TownyMessaging.sendErrorMsg(player, "You are not in a registered world.");
			return;
		}
		if (!world.isUsingTowny()) {
			TownyMessaging.sendErrorMsg(player, "This world is not using towny.");
			return;
		}
		Coord pos = Coord.parseCoord(plugin.getCache(player).getLastLocation());

		// Generate Map 
		int halfLineHeight = lineHeight / 2;
		TextComponent[][] townyMap = new TextComponent[lineWidth][lineHeight];
		int x, y = 0;
		for (int tby = pos.getX() + (lineWidth - halfLineWidth - 1); tby >= pos.getX() - halfLineWidth; tby--) {
			x = 0;
			for (int tbx = pos.getZ() - halfLineHeight; tbx <= pos.getZ() + (lineHeight - halfLineHeight - 1); tbx++) {
				try {
					townyMap[y][x] = Component.empty();
					TownBlock townblock = world.getTownBlock(tby, tbx);
					if (!townblock.hasTown())
						throw new TownyException();
					if (x == halfLineHeight && y == halfLineWidth)
						// location
						townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GOLD);
					else if (hasTown) {
						if (resident.getTown() == townblock.getTown()) {
							// own town
							townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GREEN);
							try {
								if (resident == townblock.getResident())
									//own plot
									townyMap[y][x] = townyMap[y][x].color(NamedTextColor.YELLOW);
							} catch (NotRegisteredException e) {
							}
						} else if (resident.hasNation()) {
							if (resident.getTown().getNation().hasTown(townblock.getTown()))
								// towns
								townyMap[y][x] = townyMap[y][x].color(NamedTextColor.DARK_GREEN);
							else if (townblock.getTown().hasNation()) {
								Nation nation = resident.getTown().getNation();
								if (nation.hasAlly(townblock.getTown().getNation()))
									townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GREEN);
								else if (nation.hasEnemy(townblock.getTown().getNation()))
									// towns
									townyMap[y][x] = townyMap[y][x].color(NamedTextColor.DARK_RED);
								else
									townyMap[y][x] = townyMap[y][x].color(NamedTextColor.WHITE);
							} else
								townyMap[y][x] = townyMap[y][x].color(NamedTextColor.WHITE);
						} else
							townyMap[y][x] = townyMap[y][x].color(NamedTextColor.WHITE);
					} else
						townyMap[y][x] = townyMap[y][x].color(NamedTextColor.WHITE);

					// Registered town block
					if (townblock.getPlotPrice() != -1) {
						// override the colour if it's a shop plot for sale
						if (townblock.getType().equals(TownBlockType.COMMERCIAL))
							townyMap[y][x] = townyMap[y][x].color(NamedTextColor.BLUE);
						townyMap[y][x] = townyMap[y][x].content("$");
					} else if (townblock.isHomeBlock())
						townyMap[y][x] = townyMap[y][x].content("H");
					else
						townyMap[y][x] = townyMap[y][x].content(townblock.getType().getAsciiMapKey());
					
					TownyObject owner = townblock.getTown();
					if (townblock.hasResident())
						owner = townblock.getResident();
					
					TextComponent hoverComponent = Component.text(Translation.of("status_town") + townblock.getTown().getName() + (townblock.hasResident() ? " (" + townblock.getResident().getName() + ")" : "")).color(NamedTextColor.GREEN).append(Component.text(" (" + tby + ", " + tbx + ")").color(NamedTextColor.WHITE)).append(Component.newline())
						.append(Component.text(Translation.of("status_plot_type")).color(NamedTextColor.GREEN)).append(Component.text(townblock.getType().getName()).color(NamedTextColor.DARK_GREEN)).append(Component.newline())
						.append(Component.text(Translation.of("status_perm") + ((owner instanceof Resident) ? townblock.getPermissions().getColourString().replace("n", "t") : townblock.getPermissions().getColourString().replace("f", "r")))).append(Component.newline())
						.append(Component.text(Translation.of("status_perm") + ((owner instanceof Resident) ? townblock.getPermissions().getColourString2().replace("n", "t") : townblock.getPermissions().getColourString2().replace("f", "r")))).append(Component.newline())
						.append(Component.text(Translation.of("status_pvp") + ((!CombatUtil.preventPvP(world, townblock)) ? Translation.of("status_on"): Translation.of("status_off")) + 
							Translation.of("explosions") + ((world.isForceExpl() || townblock.getPermissions().explosion) ? Translation.of("status_on"): Translation.of("status_off")) + 
							Translation.of("firespread") + ((townblock.getTown().isFire() || world.isForceFire() || townblock.getPermissions().fire) ? Translation.of("status_on"):Translation.of("status_off")) + 
							Translation.of("mobspawns") + ((world.isForceTownMobs() || townblock.getPermissions().mobs) ?  Translation.of("status_on"): Translation.of("status_off"))));

					townyMap[y][x] = townyMap[y][x].hoverEvent(HoverEvent.showText(hoverComponent));
				} catch (TownyException e) {
					if (x == halfLineHeight && y == halfLineWidth)
						townyMap[y][x] = townyMap[y][x].color(NamedTextColor.GOLD);
					else
						townyMap[y][x] = townyMap[y][x].color(NamedTextColor.DARK_GRAY);

					// Unregistered town block
					townyMap[y][x] = townyMap[y][x].content("-").clickEvent(ClickEvent.runCommand("/towny:townyworld")).hoverEvent(HoverEvent.showText(Component.text(world.getUnclaimedZoneName()).color(NamedTextColor.DARK_RED).append(Component.text(" (" + tby + ", " + tbx + ")").color(NamedTextColor.WHITE))));
				}
				x++;
			}
			y++;
		}

		String[] compass = generateCompass(player);

		// Output
		player.sendMessage(ChatTools.formatTitle(Translation.of("towny_map_header") + Colors.White + "(" + pos.toString() + ")"));
		String line;
		int lineCount = 0;
		// Variables have been rotated to fit N/S/E/W properly
		for (int my = 0; my < lineHeight; my++) {
			line = compass[0];
			if (lineCount < compass.length)
				line = compass[lineCount];

			TextComponent compassComponent = Component.text(line);
			TextComponent fullComponent = Component.empty();
			for (int mx = lineWidth - 1; mx >= 0; mx--)
				fullComponent = fullComponent.append(townyMap[mx][my]);

			if (lineCount < help.length)
				fullComponent = fullComponent.append(Component.text(help[lineCount]));

			Towny.getAdventure().player(player).sendMessage(compassComponent.append(fullComponent));
			lineCount++;
		}

		// Current town block data
		try {
			TownBlock townblock = TownyAPI.getInstance().getTownBlock(plugin.getCache(player).getLastLocation());
			TownyMessaging.sendMsg(player, (Translation.of("town_sing") + ": " + (townblock != null && townblock.hasTown() ? townblock.getTown().getName() : Translation.of("status_no_town")) + " : " + Translation.of("owner_status") + ": " + (townblock != null && townblock.hasResident() ? townblock.getResident().getName() : Translation.of("status_no_town"))));
		} catch (TownyException e) {
			//plugin.sendErrorMsg(player, e.getError());
			// Send a blank line instead of an error, to keep the map position tidy.
			player.sendMessage("");
		}
	}
}
