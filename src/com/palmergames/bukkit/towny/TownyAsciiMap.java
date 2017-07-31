package com.palmergames.bukkit.towny; /* Localized on 2014-05-05 by Neder */

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.Compass;

public class TownyAsciiMap {

	public static final int lineWidth = 27;
	public static final int halfLineWidth = lineWidth / 2;
	public static final String[] help = {
			"  " + Colors.Gray + "-" + Colors.LightGray + " = 점유해제됨",
			"  " + Colors.White + "+" + Colors.LightGray + " = 점유됨",
			"  " + Colors.White + "$" + Colors.LightGray + " = 판매중",
			"  " + Colors.LightGreen + "+" + Colors.LightGray + " = 속해있는 마을",
			"  " + Colors.Yellow + "+" + Colors.LightGray + " = 소유한 토지",
			"  " + Colors.Green + "+" + Colors.LightGray + " = 동맹국",
			"  " + Colors.Red + "+" + Colors.LightGray + " = 적국" };

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
		Resident resident;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
			if (resident.hasTown())
				hasTown = true;
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		TownyWorld world;
		try {
			world = TownyUniverse.getDataSource().getWorld(player.getWorld().getName());
		} catch (NotRegisteredException e1) {
			TownyMessaging.sendErrorMsg(player, "이 월드는 등록된 월드가 아닙니다.");
			return;
		}
		if (!world.isUsingTowny()) {
			TownyMessaging.sendErrorMsg(player, "이 월드는 타우니를 사용하지 않습니다.");
			return;
		}
		Coord pos = Coord.parseCoord(plugin.getCache(player).getLastLocation());

		// Generate Map 
		int halfLineHeight = lineHeight / 2;
		String[][] townyMap = new String[lineWidth][lineHeight];
		int x, y = 0;
		for (int tby = pos.getX() + (lineWidth - halfLineWidth - 1); tby >= pos.getX() - halfLineWidth; tby--) {
			x = 0;
			for (int tbx = pos.getZ() - halfLineHeight; tbx <= pos.getZ() + (lineHeight - halfLineHeight - 1); tbx++) {
				try {
					TownBlock townblock = world.getTownBlock(tby, tbx);
					//TODO: possibly claim outside of towns
					if (!townblock.hasTown())
						throw new TownyException();
					if (x == halfLineHeight && y == halfLineWidth)
						// location
						townyMap[y][x] = Colors.Gold;
					else if (hasTown) {
						if (resident.getTown() == townblock.getTown()) {
							// own town
							townyMap[y][x] = Colors.LightGreen;
							try {
								if (resident == townblock.getResident())
									//own plot
									townyMap[y][x] = Colors.Yellow;
							} catch (NotRegisteredException e) {
							}
						} else if (resident.hasNation()) {
							if (resident.getTown().getNation().hasTown(townblock.getTown()))
								// towns
								townyMap[y][x] = Colors.Green;
							else if (townblock.getTown().hasNation()) {
								Nation nation = resident.getTown().getNation();
								if (nation.hasAlly(townblock.getTown().getNation()))
									townyMap[y][x] = Colors.Green;
								else if (nation.hasEnemy(townblock.getTown().getNation()))
									// towns
									townyMap[y][x] = Colors.Red;
								else
									townyMap[y][x] = Colors.White;
							} else
								townyMap[y][x] = Colors.White;
						} else
							townyMap[y][x] = Colors.White;
					} else
						townyMap[y][x] = Colors.White;

					// Registered town block
					if (townblock.getPlotPrice() != -1) {
						// override the colour if it's a shop plot for sale
						if (townblock.getType().equals(TownBlockType.COMMERCIAL))
							townyMap[y][x] = Colors.Blue;
						townyMap[y][x] += "$";
					} else if (townblock.isHomeBlock())
						townyMap[y][x] += "H";
					else
						townyMap[y][x] += townblock.getType().getAsciiMapKey();
				} catch (TownyException e) {
					if (x == halfLineHeight && y == halfLineWidth)
						townyMap[y][x] = Colors.Gold;
					else
						townyMap[y][x] = Colors.Gray;

					// Unregistered town block
					townyMap[y][x] += "-";
				}
				x++;
			}
			y++;
		}

		String[] compass = generateCompass(player);

		// Output
		player.sendMessage(ChatTools.formatTitle("타우니 지도 " + Colors.White + "(" + pos.toString() + ")"));
		String line;
		int lineCount = 0;
		// Variables have been rotated to fit N/S/E/W properly
		for (int my = 0; my < lineHeight; my++) {
			line = compass[0];
			if (lineCount < compass.length)
				line = compass[lineCount];

			for (int mx = lineWidth - 1; mx >= 0; mx--)
				line += townyMap[mx][my];

			if (lineCount < help.length)
				line += help[lineCount];

			player.sendMessage(line);
			lineCount++;
		}

		// Current town block data
		try {
			TownBlock townblock = world.getTownBlock(pos);
			TownyMessaging.sendMsg(player, ("마을: " + (townblock.hasTown() ? townblock.getTown().getName() : "없음") + " : " + "소유자: " + (townblock.hasResident() ? townblock.getResident().getName() : "없음")));
		} catch (TownyException e) {
			//plugin.sendErrorMsg(player, e.getError());
			// Send a blank line instead of an error, to keep the map position tidy.
			player.sendMessage("");
		}
	}
}
