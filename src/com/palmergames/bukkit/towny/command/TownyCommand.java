package com.palmergames.bukkit.towny.command; /* Localized on 2014-05-04 by Neder */

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAsciiMap;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.KeyValue;
import com.palmergames.util.KeyValueTable;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeMgmt;

public class TownyCommand extends BaseCommand implements CommandExecutor {

	// protected static TownyUniverse universe;
	private static Towny plugin;

	private static final List<String> towny_general_help = new ArrayList<String>();
	private static final List<String> towny_help = new ArrayList<String>();
	private static final List<String> towny_top = new ArrayList<String>();
	private static final List<String> towny_war = new ArrayList<String>();
	private static String towny_version;

	static {
		towny_general_help.add(ChatTools.formatTitle(TownySettings.getLangString("help_0")));
		towny_general_help.add(TownySettings.getLangString("help_1"));
		towny_general_help.add(ChatTools.formatCommand("", "/주민", "?", "") + ", " + ChatTools.formatCommand("", "/마을", "?", "") + ", " + ChatTools.formatCommand("", "/국가", "?", "") + ", " + ChatTools.formatCommand("", "/토지", "?", "") + ", " + ChatTools.formatCommand("", "/타우니", "?", ""));
		towny_general_help.add(ChatTools.formatCommand("", "/tc", "[메시지]", TownySettings.getLangString("help_2")) + ", " + ChatTools.formatCommand("", "/nc", "[메시지]", TownySettings.getLangString("help_3")).trim());
		towny_general_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리", "?", ""));
		towny_general_help.add(ChatTools.formatCommand("한글화", "§3Neder", "", "§c기존 영문 명령어도 사용가능합니다."));

		towny_help.add(ChatTools.formatTitle("/타우니"));
		towny_help.add(ChatTools.formatCommand("", "/타우니", "", "기본적인 타우니 도움말"));
		towny_help.add(ChatTools.formatCommand("", "/타우니", "지도", "주위에 있는 마을블록 지도 표시"));
		towny_help.add(ChatTools.formatCommand("", "/타우니", "가격", "마을/국가 관련 가격 보기"));
		towny_help.add(ChatTools.formatCommand("", "/타우니", "순위", "스코어보드 출력"));
		towny_help.add(ChatTools.formatCommand("", "/타우니", "시간", "다음날까지 남은 시간 보기"));
		towny_help.add(ChatTools.formatCommand("", "/타우니", "상태", "상태 표시"));
		towny_help.add(ChatTools.formatCommand("", "/타우니", "버전", "타우니 버전 표시"));
		towny_help.add(ChatTools.formatCommand("", "/타우니", "전쟁", "'/타우니 전쟁' 을 입력해서 자세하게 알아보세요"));

	}

	public TownyCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		towny_version = Colors.Green + "타우니 버전: " + Colors.LightGreen + plugin.getVersion();

		towny_war.add(ChatTools.formatTitle("/타우니 전쟁"));
		towny_war.add(ChatTools.formatCommand("", "/타우니 전쟁", "상황", ""));
		towny_war.add(ChatTools.formatCommand("", "/타우니 전쟁", "점수", ""));
		towny_war.add(ChatTools.formatCommand("", "/타우니 전쟁", "hud", ""));

		if (sender instanceof Player) {
			Player player = (Player) sender;
			System.out.println("[PLAYER_COMMAND] " + player.getName() + ": /" + commandLabel + " " + StringMgmt.join(args));
			parseTownyCommand(player, args);
		} else {
			// Console output
			if (args.length == 0)
				for (String line : towny_general_help)
					sender.sendMessage(Colors.strip(line));
			else if (args[0].equalsIgnoreCase("tree") || args[0].equalsIgnoreCase("트리"))
				plugin.getTownyUniverse().sendUniverseTree(sender);
			else if (args[0].equalsIgnoreCase("time") || args[0].equalsIgnoreCase("시간")) {
				TownyMessaging.sendMsg("다음날까지 남은 시간: " + TimeMgmt.formatCountdownTime(TownyTimerHandler.townyTime()));
			} else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("v") || args[0].equalsIgnoreCase("버전"))
				sender.sendMessage(Colors.strip(towny_version));
			else if (args[0].equalsIgnoreCase("war") || args[0].equalsIgnoreCase("전쟁")) {
				boolean war = TownyWar(StringMgmt.remFirstArg(args), null);
				if (war)
					for (String line : towny_war)
						sender.sendMessage(Colors.strip(line));
				else
					sender.sendMessage("이 월드에는 전쟁이 일어나지 않았습니다.");

				towny_war.clear();
			} else if (args[0].equalsIgnoreCase("universe") || args[0].equalsIgnoreCase("상태")) {
				for (String line : getUniverseStats())
					sender.sendMessage(Colors.strip(line));
			}

		}
		return true;
	}

	private void parseTownyCommand(Player player, String[] split) {

		if (split.length == 0) {
			for (String line : towny_general_help)
				player.sendMessage(line);

			return;
		} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help") || split[0].equalsIgnoreCase("도움말")) {
			for (String line : towny_help)
				player.sendMessage(Colors.strip(line));

			return;
		}

		try {

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY.getNode(split[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split[0].equalsIgnoreCase("map") || split[0].equalsIgnoreCase("지도"))
				if (split.length > 1 && split[1].equalsIgnoreCase("big"))
					TownyAsciiMap.generateAndSend(plugin, player, 18);
				else
				if (split.length > 1 && split[1].equalsIgnoreCase("크게"))
					TownyAsciiMap.generateAndSend(plugin, player, 18); // Additional Code by Neder / 2014-06-14
				else
					showMap(player);
			else if (split[0].equalsIgnoreCase("prices") || split[0].equalsIgnoreCase("가격")) {
				Town town = null;
				if (split.length > 1) {
					try {
						town = TownyUniverse.getDataSource().getTown(split[1]);
					} catch (NotRegisteredException x) {
						sendErrorMsg(player, x.getMessage());
						return;
					}
				} else if (split.length == 1)
					try {
						Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
						town = resident.getTown();
					} catch (NotRegisteredException x) {
					}

				for (String line : getTownyPrices(town))
					player.sendMessage(line);

			} else if (split[0].equalsIgnoreCase("top") || split[0].equalsIgnoreCase("순위")) {
				TopCommand(player, StringMgmt.remFirstArg(split));
			} else if (split[0].equalsIgnoreCase("tree") || split[0].equalsIgnoreCase("트리")) {
				consoleUseOnly(player);
			} else if (split[0].equalsIgnoreCase("time") || split[0].equalsIgnoreCase("시간")) {
				TownyMessaging.sendMsg(player, "다음날까지 남은 시간: " + TimeMgmt.formatCountdownTime(TownyTimerHandler.townyTime()));
			} else if (split[0].equalsIgnoreCase("universe") || split[0].equalsIgnoreCase("상태")) {
				for (String line : getUniverseStats())
					player.sendMessage(line);
			} else if (split[0].equalsIgnoreCase("version") || split[0].equalsIgnoreCase("v") || split[0].equalsIgnoreCase("버전")) {
				player.sendMessage(towny_version);
			} else if (split[0].equalsIgnoreCase("war") || split[0].equalsIgnoreCase("전쟁")) {
				boolean war = TownyWar(StringMgmt.remFirstArg(split), player);
				if (war)
					for (String line : towny_war)
						player.sendMessage(Colors.strip(line));
				else
					sendErrorMsg(player, "이 월드에는 전쟁이 일어나지 않았습니다.");

				towny_war.clear();
			} else if (split[0].equalsIgnoreCase("spy") || split[0].equalsIgnoreCase("스파이")) {

				if (plugin.isPermissions() && TownyUniverse.getPermissionSource().has(player, PermissionNodes.TOWNY_CHAT_SPY.getNode())) {
					if (plugin.hasPlayerMode(player, "spy"))
						plugin.removePlayerMode(player);
					else
						plugin.setPlayerMode(player, split, true);
				} else
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_command_disable"));

			} else
				sendErrorMsg(player, "서브 명령어가 잘못되었습니다.");

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}

	}

	private boolean TownyWar(String[] args, Player p) {

		if (TownyUniverse.isWarTime() && args.length > 0) {
			towny_war.clear();
			if (args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("상태"))
				towny_war.addAll(plugin.getTownyUniverse().getWarEvent().getStats());
			else if (args[0].equalsIgnoreCase("scores") || args[0].equalsIgnoreCase("점수"))
				towny_war.addAll(plugin.getTownyUniverse().getWarEvent().getScores(-1));
			else if (args[0].equalsIgnoreCase("hud") && p == null)
				towny_war.add("콘솔에서는 hud를 사용할 수 없습니다!");
			else if (args[0].equalsIgnoreCase("hud") && p != null)
				plugin.getHUDManager().toggleWarHUD(p);
		}

		return TownyUniverse.isWarTime();
	}

	private void TopCommand(Player player, String[] args) {

		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			towny_top.add(ChatTools.formatTitle("/타우니 순위"));
			towny_top.add(ChatTools.formatCommand("", "/타우니 순위", "주민 [모두/마을/국가]", ""));
			towny_top.add(ChatTools.formatCommand("", "/타우니 순위", "토지 [모두/주민/마을]", ""));
		} else if (args[0].equalsIgnoreCase("residents") || args[0].equalsIgnoreCase("주민"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("모두")) {
				List<ResidentList> list = new ArrayList<ResidentList>(TownyUniverse.getDataSource().getTowns());
				list.addAll(TownyUniverse.getDataSource().getNations());
				towny_top.add(ChatTools.formatTitle("주민 순위"));
				towny_top.addAll(getMostResidents(list, 10));
			} else if (args[1].equalsIgnoreCase("town") || args[1].equalsIgnoreCase("마을")) {
				towny_top.add(ChatTools.formatTitle("주민이 가장 많은 마을"));
				towny_top.addAll(getMostResidents(new ArrayList<ResidentList>(TownyUniverse.getDataSource().getTowns()), 10));
			} else if (args[1].equalsIgnoreCase("nation") || args[1].equalsIgnoreCase("국가")) {
				towny_top.add(ChatTools.formatTitle("주민이 가장 많은 국가"));
				towny_top.addAll(getMostResidents(new ArrayList<ResidentList>(TownyUniverse.getDataSource().getNations()), 10));
			} else
				sendErrorMsg(player, "서브 명령어가 잘못되었습니다.");
		else if (args[0].equalsIgnoreCase("land") || args[0].equalsIgnoreCase("토지"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("모두")) {
				List<TownBlockOwner> list = new ArrayList<TownBlockOwner>(TownyUniverse.getDataSource().getResidents());
				list.addAll(TownyUniverse.getDataSource().getTowns());
				towny_top.add(ChatTools.formatTitle("토지 순위"));
				towny_top.addAll(getMostLand(list, 10));
			} else if (args[1].equalsIgnoreCase("resident") || args[1].equalsIgnoreCase("주민")) {
				towny_top.add(ChatTools.formatTitle("토지를 가장 많이 소유한 주민"));
				towny_top.addAll(getMostLand(new ArrayList<TownBlockOwner>(TownyUniverse.getDataSource().getResidents()), 10));
			} else if (args[1].equalsIgnoreCase("town") || args[1].equalsIgnoreCase("마을")) {
				towny_top.add(ChatTools.formatTitle("토지를 가장 많이 소유한 마을"));
				towny_top.addAll(getMostLand(new ArrayList<TownBlockOwner>(TownyUniverse.getDataSource().getTowns()), 10));
			} else
				sendErrorMsg(player, "서브 명령어가 잘못되었습니다.");
		else
			sendErrorMsg(player, "서브 명령어가 잘못되었습니다.");

		for (String line : towny_top)
			player.sendMessage(line);

		towny_top.clear();

	}

	public List<String> getUniverseStats() {

		List<String> output = new ArrayList<String>();
		output.add("\u00A70-\u00A74###\u00A70---\u00A74###\u00A70-");
		output.add("\u00A74#\u00A7c###\u00A74#\u00A70-\u00A74#\u00A7c###\u00A74#\u00A70   \u00A76[\u00A7e타우니 " + plugin.getVersion() + "\u00A76]");
		output.add("\u00A74#\u00A7c####\u00A74#\u00A7c####\u00A74#   \u00A73By: \u00A7bChris H (Shade)/Llmdl/ElgarL");
		output.add("\u00A70-\u00A74#\u00A7c#######\u00A74#\u00A70-   \u00A73Korean Localized by \u00A7bNeder");
		output.add("\u00A70--\u00A74##\u00A7c###\u00A74##\u00A70-- " + "\u00A73주민 수: \u00A7b" + Integer.toString(TownyUniverse.getDataSource().getResidents().size()) + Colors.Gray + " | " + "\u00A73마을 수: \u00A7b" + Integer.toString(TownyUniverse.getDataSource().getTowns().size()) + Colors.Gray + " | " + "\u00A73국가 수: \u00A7b" + Integer.toString(TownyUniverse.getDataSource().getNations().size()));
		output.add("\u00A70----\u00A74#\u00A7c#\u00A74#\u00A70---- " + "\u00A73월드 수: \u00A7b" + Integer.toString(TownyUniverse.getDataSource().getWorlds().size()) + Colors.Gray + " | " + "\u00A73마을블록 수: \u00A7b" + Integer.toString(TownyUniverse.getDataSource().getAllTownBlocks().size()));
		output.add("\u00A70-----\u00A74#\u00A70----- ");
		return output;
	}

	/**
	 * Send a map of the nearby townblocks status to player Command: /towny map
	 * 
	 * @param player
	 */

	public static void showMap(Player player) {

		TownyAsciiMap.generateAndSend(plugin, player, 7);
	}

	/**
	 * Send the list of costs for Economy to player Command: /towny prices
	 * 
	 * @param town
	 */

	/*
	 * [New] Town: 100 | Nation: 500 [Upkeep] Town: 10 | Nation: 100 Town
	 * [Elden]: [Price] Plot: 100 | Outpost: 250 [Upkeep] Resident: 20 | Plot:
	 * 50 Nation [Albion]: [Upkeep] Town: 100 | Peace: 100
	 */

	// TODO: Proceduralize and make parse function for /towny prices [town]
	public List<String> getTownyPrices(Town town) {

		List<String> output = new ArrayList<String>();
		Nation nation = null;

		if (town != null)
			if (town.hasNation())
				try {
					nation = town.getNation();
				} catch (NotRegisteredException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

		output.add(ChatTools.formatTitle("가격"));
		output.add(Colors.Yellow + "[신설] " + Colors.Green + "마을: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNewTownPrice()) + Colors.Gray + " | " + Colors.Green + "국가: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNewNationPrice()));
		if (town != null)
            output.add(Colors.Yellow + "[유지비] " + Colors.Green + "마을: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getTownUpkeepCost(town)) + Colors.Gray + " | " + Colors.Green + "국가: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNationUpkeepCost(nation)));
		if (town == null)
            output.add(Colors.Yellow + "[유지비] " + Colors.Green + "마을: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getTownUpkeep()) + Colors.Gray + " | " + Colors.Green + "국가: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNationUpkeep()));
		output.add(Colors.Yellow + "마을 유지비는 다음과 비례합니다" + Colors.LightGreen + ": " + (TownySettings.isUpkeepByPlot() ? "마을블록 수" : " 마을레벨 (주민 수)."));

		if (town != null) {
			output.add(Colors.Yellow + "마을 [" + TownyFormatter.getFormattedName(town) + "]");
			output.add(Colors.Rose + "    [가격] " + Colors.Green + "토지: " + Colors.LightGreen + Double.toString(town.getPlotPrice()) + Colors.Gray + " | " + Colors.Green + "전초기지: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost()));
			output.add(Colors.Rose + "            " + Colors.Green + "상점: " + Colors.LightGreen + Double.toString(town.getCommercialPlotPrice()) + Colors.Gray + " | " + Colors.Green + "대사관: " + Colors.LightGreen + Double.toString(town.getEmbassyPlotPrice()));

			output.add(Colors.Rose + "    [세금] " + Colors.Green + "주민: " + Colors.LightGreen + Double.toString(town.getTaxes()) + (town.isTaxPercentage()? "%" : "") + Colors.Gray + " | " + Colors.Green + "주민: " + Colors.LightGreen + Double.toString(town.getPlotTax()));
			output.add(Colors.Rose + "            " + Colors.Green + "상점: " + Colors.LightGreen + Double.toString(town.getCommercialPlotTax()) + Colors.Gray + " | " + Colors.Green + "대사관: " + Colors.LightGreen + Double.toString(town.getEmbassyPlotTax()));
			
			if (nation != null) {
				output.add(Colors.Yellow + "주민 [" + TownyFormatter.getFormattedName(nation) + "]");
				output.add(Colors.Rose + "    [세금] " + Colors.Green + "마을: " + Colors.LightGreen + Double.toString(nation.getTaxes()) + Colors.Gray + " | " + Colors.Green + "중립선언: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNationNeutralityCost()));
			}
		}
		return output;
	}

	public List<String> getTopBankBalance(List<TownyEconomyObject> list, int maxListing) throws EconomyException {

		List<String> output = new ArrayList<String>();
		KeyValueTable<TownyEconomyObject, Double> kvTable = new KeyValueTable<TownyEconomyObject, Double>();
		for (TownyEconomyObject obj : list) {
			kvTable.put(obj, obj.getHoldingBalance());
		}
		kvTable.sortByValue();
		kvTable.revese();
		int n = 0;
		for (KeyValue<TownyEconomyObject, Double> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			TownyEconomyObject town = (TownyEconomyObject) kv.key;
			output.add(String.format(Colors.LightGray + "%-20s " + Colors.Gold + "|" + Colors.Blue + " %s", TownyFormatter.getFormattedName(town), TownyEconomyHandler.getFormattedBalance((Double) kv.value)));
		}
		return output;
	}

	public List<String> getMostResidents(List<ResidentList> list, int maxListing) {

		List<String> output = new ArrayList<String>();
		KeyValueTable<ResidentList, Integer> kvTable = new KeyValueTable<ResidentList, Integer>();
		for (ResidentList obj : list)
			kvTable.put(obj, obj.getResidents().size());
		kvTable.sortByValue();
		kvTable.revese();
		int n = 0;
		for (KeyValue<ResidentList, Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			ResidentList residentList = (ResidentList) kv.key;
			output.add(String.format(Colors.Blue + "%30s " + Colors.Gold + "|" + Colors.LightGray + " %10d", TownyFormatter.getFormattedName((TownyObject) residentList), (Integer) kv.value));
		}
		return output;
	}

	public List<String> getMostLand(List<TownBlockOwner> list, int maxListing) {

		List<String> output = new ArrayList<String>();
		KeyValueTable<TownBlockOwner, Integer> kvTable = new KeyValueTable<TownBlockOwner, Integer>();
		for (TownBlockOwner obj : list)
			kvTable.put(obj, obj.getTownBlocks().size());
		kvTable.sortByValue();
		kvTable.revese();
		int n = 0;
		for (KeyValue<TownBlockOwner, Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			TownBlockOwner town = (TownBlockOwner) kv.key;
			output.add(String.format(Colors.Blue + "%30s " + Colors.Gold + "|" + Colors.LightGray + " %10d", TownyFormatter.getFormattedName(town), (Integer) kv.value));
		}
		return output;
	}

	public void consoleUseOnly(Player player) {

		TownyMessaging.sendErrorMsg(player, "이 명령어는 콘솔에서만 사용할 수 있습니다.");
	}

	public void inGameUseOnly(CommandSender sender) {

		sender.sendMessage("[타우니] 입력오류: 이 명령어는 게임 내에서만 사용할 수 있습니다.");
	}

	public boolean sendErrorMsg(CommandSender sender, String msg) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			TownyMessaging.sendErrorMsg(player, msg);
		} else
			// Console
			sender.sendMessage("[타우니] 콘솔 오류: " + msg);

		return false;
	}
}
