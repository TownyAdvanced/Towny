package com.palmergames.bukkit.towny.command;

import ca.xshade.bukkit.questioner.Questioner;
import ca.xshade.questionmanager.Option;
import ca.xshade.questionmanager.Question;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.questioner.PurgeQuestionTask;
import com.palmergames.bukkit.towny.tasks.ResidentPurge;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MemMgmt;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeTools;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Send a list of all general townyadmin help commands to player Command:
 * /townyadmin
 */

public class TownyAdminCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> ta_help = new ArrayList<String>();
	private static final List<String> ta_panel = new ArrayList<String>();
	private static final List<String> ta_unclaim = new ArrayList<String>();

	private boolean isConsole;
	private Player player;
	private CommandSender sender;

	static {
		ta_help.add(ChatTools.formatTitle("/타우니관리"));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "", TownySettings.getLangString("admin_panel_1")));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "설정 [] .. []", "'/타우니관리 설정' " + TownySettings.getLangString("res_5")));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "점유해제 [반지름]", ""));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "마을/국가", ""));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "보너스주기 [마을/플레이어] [갯수]", ""));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "토글 평화로움/전쟁/디버그/개발모드", ""));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "주민/마을/국가", ""));

		// TODO: ta_help.add(ChatTools.formatCommand("", "/타우니관리",
		// "npc rename [old name] [new name]", ""));
		// TODO: ta_help.add(ChatTools.formatCommand("", "/타우니관리",
		// "npc list", ""));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "리로드", TownySettings.getLangString("admin_panel_2")));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "초기화", ""));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "백업", ""));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "새날", TownySettings.getLangString("admin_panel_3")));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "정리 [~~~일 묵은 데이터]", ""));
		ta_help.add(ChatTools.formatCommand("", "/타우니관리", "삭제 [] .. []", "선택한 주민의 데이터 파일을 삭제합니다."));

		ta_unclaim.add(ChatTools.formatTitle("/타우니관리 점유해제"));
		ta_unclaim.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 점유해제", "", TownySettings.getLangString("townyadmin_help_1")));
		ta_unclaim.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 점유해제", "[반지름]", TownySettings.getLangString("townyadmin_help_2")));

	}

	public TownyAdminCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		this.sender = sender;

		if (sender instanceof Player) {
			player = (Player) sender;
			isConsole = false;

		} else {
			isConsole = true;
			this.player = null;
		}

		try {
			return parseTownyAdminCommand(args);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}

		return true;
	}

	private Object getSender() {

		if (isConsole)
			return sender;
		else
			return player;
	}

	public boolean parseTownyAdminCommand(String[] split) throws TownyException {

		if (split.length == 0) {
			if (getSender()==player && !TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_ADMIN.getNode()))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			buildTAPanel();
			for (String line : ta_panel) {
				sender.sendMessage(line);
			}

		} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help") || split[0].equalsIgnoreCase("도움말")) {
			for (String line : ta_help) {
				sender.sendMessage(line);
			}
		} else {

			if (split[0].equalsIgnoreCase("set") || split[0].equalsIgnoreCase("설정")) {

				adminSet(StringMgmt.remFirstArg(split));
				return true;
			} else if (split[0].equalsIgnoreCase("resident") || split[0].equalsIgnoreCase("주민")){
				
				parseAdminResidentCommand(StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("town") || split[0].equalsIgnoreCase("마을")) {

				parseAdminTownCommand(StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("nation") || split[0].equalsIgnoreCase("국가")) {

				parseAdminNationCommand(StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("toggle") || split[0].equalsIgnoreCase("토글")) {

				parseToggleCommand(StringMgmt.remFirstArg(split));
				return true;

			}

			if ((!isConsole) && (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN.getNode(split[0].toLowerCase()))))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split[0].equalsIgnoreCase("givebonus") || split[0].equalsIgnoreCase("보너스주기")) {

				giveBonus(StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("reload") || split[0].equalsIgnoreCase("리로드")) {

				reloadTowny(false);

			} else if (split[0].equalsIgnoreCase("reset") || split[0].equalsIgnoreCase("재설정")) {

				reloadTowny(true);

			} else if (split[0].equalsIgnoreCase("backup") || split[0].equalsIgnoreCase("백업")) {

				try {
					TownyUniverse.getDataSource().backup();
					TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("mag_backup_success"));

				} catch (IOException e) {
					TownyMessaging.sendErrorMsg(getSender(), "오류: " + e.getMessage());

				}

			} else if (split[0].equalsIgnoreCase("newday") || split[0].equalsIgnoreCase("새날")) {

				TownyTimerHandler.newDay();

			} else if (split[0].equalsIgnoreCase("purge") || split[0].equalsIgnoreCase("정리")) {

				purge(StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("delete") || split[0].equalsIgnoreCase("삭제")) {
				String[] newSplit = StringMgmt.remFirstArg(split);
				residentDelete(player, newSplit);
			} else if (split[0].equalsIgnoreCase("unclaim") || split[0].equalsIgnoreCase("점유해제")) {

				parseAdminUnclaimCommand(StringMgmt.remFirstArg(split));
				/*
				 * else if (split[0].equalsIgnoreCase("seed") &&
				 * TownySettings.getDebug()) seedTowny(); else if
				 * (split[0].equalsIgnoreCase("warseed") &&
				 * TownySettings.getDebug()) warSeed(player);
				 */

			} else {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_sub"));
				return false;
			}
		}

		return true;
	}

	private void giveBonus(String[] split) throws TownyException {

		Town town;

		try {
			if (split.length != 2)
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "예시: 보너스주기 [마을/플레이어] [n]"));
			try {
				town = TownyUniverse.getDataSource().getTown(split[0]);
			} catch (NotRegisteredException e) {
				town = TownyUniverse.getDataSource().getResident(split[0]).getTown();
			}
			try {
				town.setBonusBlocks(town.getBonusBlocks() + Integer.parseInt(split[1].trim()));
				TownyMessaging.sendMsg(getSender(), String.format(TownySettings.getLangString("msg_give_total"), town.getName(), split[1], town.getBonusBlocks()));
				TownyMessaging.sendTownMessagePrefixed(town, "마을블록 보너스 " + Integer.parseInt(split[1].trim()) + "개를 받았습니다.");
				TownyMessaging.sendTownMessagePrefixed(town, "만약 현실 화폐를 주고 이 마을블록 보너스를 지급받았다면 Towny를 만들고 번역하는 사람들은 이 거래를 용납하지 않는다는 것을 알아두시기 바랍니다. 지금 플레이 중인 이 서버는 마인크래프트의 EULA를 위반하고 있고, 심지어 이 서버의 관리자가 만들지 않은 Towny의 일부를 판매하고 있습니다.");
				TownyMessaging.sendTownMessagePrefixed(town, "환불받고 다른 서버로 옮기는 것을 고려해보세요.");
			} catch (NumberFormatException nfe) {
				throw new TownyException(TownySettings.getLangString("msg_error_must_be_int"));
			}
			TownyUniverse.getDataSource().saveTown(town);
		} catch (TownyException e) {
			throw new TownyException(e.getMessage());
		}

	}

	private void buildTAPanel() {

		ta_panel.clear();
		Runtime run = Runtime.getRuntime();
		ta_panel.add(ChatTools.formatTitle(TownySettings.getLangString("ta_panel_1")));
		ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "타우니" + Colors.Blue + "] " + Colors.Green + TownySettings.getLangString("ta_panel_2") + Colors.LightGreen + TownyUniverse.isWarTime() + Colors.Gray + " | " + Colors.Green + TownySettings.getLangString("ta_panel_3") + (TownyTimerHandler.isHealthRegenRunning() ? Colors.LightGreen + "켜짐" : Colors.Rose + "꺼짐") + Colors.Gray + " | " + (Colors.Green + TownySettings.getLangString("ta_panel_5") + (TownyTimerHandler.isDailyTimerRunning() ? Colors.LightGreen + "켜짐" : Colors.Rose + "꺼짐")));
		/*
		 * ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Towny" +
		 * Colors.Blue + "] " + Colors.Green +
		 * TownySettings.getLangString("ta_panel_4") +
		 * (TownySettings.isRemovingWorldMobs() ? Colors.LightGreen + "On" :
		 * Colors.Rose + "Off") + Colors.Gray + " | " + Colors.Green +
		 * TownySettings.getLangString("ta_panel_4_1") +
		 * (TownySettings.isRemovingTownMobs() ? Colors.LightGreen + "On" :
		 * Colors.Rose + "Off"));
		 * 
		 * try { TownyEconomyObject.checkEconomy(); ta_panel.add(Colors.Blue +
		 * "[" + Colors.LightBlue + "Economy" + Colors.Blue + "] " +
		 * Colors.Green + TownySettings.getLangString("ta_panel_6") +
		 * Colors.LightGreen + TownyFormatter.formatMoney(getTotalEconomy()) +
		 * Colors.Gray + " | " + Colors.Green +
		 * TownySettings.getLangString("ta_panel_7") + Colors.LightGreen +
		 * getNumBankAccounts()); } catch (Exception e) { }
		 */
		ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + TownySettings.getLangString("ta_panel_8") + Colors.Blue + "] " + Colors.Green + TownySettings.getLangString("ta_panel_9") + Colors.LightGreen + MemMgmt.getMemSize(run.totalMemory()) + Colors.Gray + " | " + Colors.Green + TownySettings.getLangString("ta_panel_10") + Colors.LightGreen + Thread.getAllStackTraces().keySet().size() + Colors.Gray + " | " + Colors.Green + TownySettings.getLangString("ta_panel_11") + Colors.LightGreen + TownyFormatter.getTime());
		ta_panel.add(Colors.Yellow + MemMgmt.getMemoryBar(50, run));

	}

	public void parseAdminUnclaimCommand(String[] split) {

		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			for (String line : ta_unclaim)
				((CommandSender) getSender()).sendMessage(line);
		} else {

			if (isConsole) {
				sender.sendMessage("[Towny] 입력오류: 이 명령어는 콘솔에서 사용할 수 없습니다.");
				return;
			}

			try {
				if (TownyUniverse.isWarTime())
					throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

				List<WorldCoord> selection;
				selection = AreaSelectionUtil.selectWorldCoordArea(null, new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player)), split);
				selection = AreaSelectionUtil.filterWildernessBlocks(selection);

				new TownClaim(plugin, player, null, selection, false, false, true).start();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		}
	}

	public void parseAdminResidentCommand(String[] split) throws TownyException {
		if (split.length == 0 || split[0].equalsIgnoreCase("?")){
			sender.sendMessage(ChatTools.formatTitle("/타우니관리 주민"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 주민", "[주민]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 주민", "[주민] 이름변경 [새 이름]", ""));
			
			return;
		}

		try {
			Resident resident = TownyUniverse.getDataSource().getResident(split[0]);

			if (split.length == 1){
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(resident, player));
				return;
			}

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT.getNode(split[1].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if(split[1].equalsIgnoreCase("rename") || split[1].equalsIgnoreCase("이름변경")){	
				if (!NameValidation.isBlacklistName(split[2])) {
					TownyUniverse.getDataSource().renamePlayer(resident, split[2]);
				} else
					TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_invalid_name"));
			}

		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		} catch (TownyException e) {	
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}
	}
	
	public void parseAdminTownCommand(String[] split) throws TownyException {

		// TODO Make this use the actual town command procedually.

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/타우니관리 마을"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 마을", "[마을]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 마을", "[마을] 추가/추방 [] .. []", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 마을", "[마을] 이름변경 [새로운 이름]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 마을", "[마을] 삭제", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 마을", "[마을] 스폰", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 마을", "[마을] 전초기지스폰 #", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 마을", "[마을] 등급", "")); 

			return;
		}

		try {
			
			Town town = TownyUniverse.getDataSource().getTown(split[0]);
			
			if (split.length == 1) {
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(town));
				return;
			}

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN.getNode(split[1].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split[1].equalsIgnoreCase("add") || split[1].equalsIgnoreCase("추가")) {
				/*
				 * if (isConsole) { sender.sendMessage(
				 * "[Towny] InputError: This command was designed for use in game only."
				 * ); return; }
				 */
				TownCommand.townAdd(getSender(), town, StringMgmt.remArgs(split, 2));

			} else if (split[1].equalsIgnoreCase("kick") || split[1].equalsIgnoreCase("추방")) {

				TownCommand.townKickResidents(getSender(), town.getMayor(), town, TownyUniverse.getValidatedResidents(getSender(), StringMgmt.remArgs(split, 2)));

			} else if (split[1].equalsIgnoreCase("delete") || split[1].equalsIgnoreCase("삭제")) {

				TownyUniverse.getDataSource().removeTown(town);
				TownyMessaging.sendMessage(sender, town + " 이(가) 삭제되었습니다.");

			} else if (split[1].equalsIgnoreCase("rename") || split[1].equalsIgnoreCase("이름변경")) {

				if (!NameValidation.isBlacklistName(split[2])) {
					TownyUniverse.getDataSource().renameTown(town, split[2]);
					TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_name"), ((getSender() instanceof Player) ? player.getName() : "CONSOLE"), town.getName()));
				} else
					TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_invalid_name"));
				
			} else if (split[1].equalsIgnoreCase("spawn") || split[1].equalsIgnoreCase("스폰")) {

				TownCommand.townSpawn(player, StringMgmt.remArgs(split, 2), town, "", false);

			} else if (split[1].equalsIgnoreCase("outpost") || split[1].equalsIgnoreCase("전초기지스폰")) {

				TownCommand.townSpawn(player, StringMgmt.remArgs(split, 2), town, "", true);

			} else if (split[1].equalsIgnoreCase("rank")) || split[1].equalsIgnoreCase("등급")) {
				
				parseAdminTownRankCommand(player, town, StringMgmt.remArgs(split, 2));
			}

		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}

	}

	private void parseAdminTownRankCommand(Player player, Town town, String[] split) throws TownyException {

		/*
		 * Does the command have enough arguments?
		 */
		if (split.length < 3)
			throw new TownyException("Eg: /타우니관리 마을 [마을] 등급 추가/제거 [주민] [등급]");

		Resident target;
		
		try {

			target = TownyUniverse.getDataSource().getResident(split[1]);
			if (!target.hasTown()) {
				throw new TownyException(TownySettings.getLangString("msg_resident_not_your_town"));
			}
			if (target.getTown() != town) {
				throw new TownyException(TownySettings.getLangString("msg_err_townadmintownrank_wrong_town"));
			}
				
		} catch (TownyException x) {
			throw new TownyException(x.getMessage());
		}

		String rank = split[2].toLowerCase();
		/*
		 * Is this a known rank?
		 */
		if (!TownyPerms.getTownRanks().contains(rank))
			throw new TownyException(String.format(TownySettings.getLangString("msg_unknown_rank_available_ranks"), rank, StringMgmt.join(TownyPerms.getTownRanks(), ",") ));

		if (split[0].equalsIgnoreCase("add") || split[0].equalsIgnoreCase("추가")) {
			try {
				if (target.addTownRank(rank)) {
					TownyMessaging.sendMsg(target, String.format(TownySettings.getLangString("msg_you_have_been_given_rank"), "마을", rank));
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_you_have_given_rank"), "마을", rank, target.getName()));
				} else {
					// Not in a town or Rank doesn't exist
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_resident_not_your_town"));
					return;
				}
			} catch (AlreadyRegisteredException e) {
				// Must already have this rank
				TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_resident_already_has_rank"), target.getName(), "마을"));
				return;
			}

		} else if (split[0].equalsIgnoreCase("remove") || split[0].equalsIgnoreCase("제거")) {
			try {
				if (target.removeTownRank(rank)) {
					TownyMessaging.sendMsg(target, String.format(TownySettings.getLangString("msg_you_have_had_rank_taken"), "마을", rank));
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_you_have_taken_rank_from"), "마을", rank, target.getName()));
				}
			} catch (NotRegisteredException e) {
				// Must already have this rank
				TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_resident_doesnt_have_rank"), target.getName(), "마을"));
				return;
			}

		} else {
			TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), split[0]));
			return;
		}

		/*
		 * If we got here we have made a change Save the altered resident
		 * data.
		 */
		TownyUniverse.getDataSource().saveResident(target);
		
	}

	public void parseAdminNationCommand(String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

			sender.sendMessage(ChatTools.formatTitle("/타우니관리 국가"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 국가", "[국가]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 국가", "[국가] 추가 [] .. []", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 국가", "[국가] 이름변경 [새로운 이름]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니관리 국가", "[국가] 삭제", ""));

			return;
		}
		try {
			Nation nation = TownyUniverse.getDataSource().getNation(split[0]);
			if (split.length == 1) {
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(nation));
				return;
			}

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION.getNode(split[1].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split[1].equalsIgnoreCase("add") || split[1].equalsIgnoreCase("추가")) {
				/*
				 * if (isConsole) { sender.sendMessage(
				 * "[Towny] InputError: This command was designed for use in game only."
				 * ); return; }
				 */
				NationCommand.nationAdd(nation, TownyUniverse.getDataSource().getTowns(StringMgmt.remArgs(split, 2)));

			} else if (split[1].equalsIgnoreCase("delete") || split[1].equalsIgnoreCase("삭제")) {

				TownyUniverse.getDataSource().removeNation(nation);

			} else if (split[1].equalsIgnoreCase("rename") || split[1].equalsIgnoreCase("이름변경")) {

				if (!NameValidation.isBlacklistName(split[2])) {
					TownyUniverse.getDataSource().renameNation(nation, split[2]);
					TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_nation_set_name"), ((getSender() instanceof Player) ? player.getName() : "CONSOLE"), nation.getName()));
				} else
					TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_invalid_name"));
			}

		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		} catch (AlreadyRegisteredException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}
	}

	public void adminSet(String[] split) throws TownyException {

		if (split.length == 0) {
			sender.sendMessage(ChatTools.formatTitle("/타우니관리 설정"));
			// TODO: player.sendMessage(ChatTools.formatCommand("",
			// "/타우니관리 설정", "king [nation] [king]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/타우니관리 설정", "촌장 [마을] " + TownySettings.getLangString("town_help_2"), ""));
			sender.sendMessage(ChatTools.formatCommand("", "/타우니관리 설정", "촌장 [마을] npc", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/타우니관리 설정", "수도 [마을]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/타우니관리 설정", "국가접두사 [주민] [접두사]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/타우니관리 설정", "국가접미사 [주민] [접미사]", ""));

			return;
		}

		if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET.getNode(split[0].toLowerCase())))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		if (split[0].equalsIgnoreCase("mayor") || split[0].equalsIgnoreCase("촌장")) {
			if (split.length < 3) {
				
				sender.sendMessage(ChatTools.formatTitle("/타우니관리 설정 mayor"));
				sender.sendMessage(ChatTools.formatCommand("예시", "/타우니관리 설정 촌장", "[마을] " + TownySettings.getLangString("town_help_2"), ""));
				sender.sendMessage(ChatTools.formatCommand("예시", "/타우니관리 설정 촌장", "[마을] npc", ""));
				
			} else
				try {
					Resident newMayor = null;
					Town town = TownyUniverse.getDataSource().getTown(split[1]);

					if (split[2].equalsIgnoreCase("npc")) {
						String name = nextNpcName();
						TownyUniverse.getDataSource().newResident(name);

						newMayor = TownyUniverse.getDataSource().getResident(name);

						newMayor.setRegistered(System.currentTimeMillis());
						newMayor.setLastOnline(0);
						newMayor.setNPC(true);

						TownyUniverse.getDataSource().saveResident(newMayor);
						TownyUniverse.getDataSource().saveResidentList();

						// set for no upkeep as an NPC mayor is assigned
						town.setHasUpkeep(false);

					} else {
						newMayor = TownyUniverse.getDataSource().getResident(split[2]);

						// set upkeep again
						town.setHasUpkeep(true);
					}

					if (!town.hasResident(newMayor))
						TownCommand.townAddResident(town, newMayor);
					// Delete the resident if the old mayor was an NPC.
					Resident oldMayor = town.getMayor();

					town.setMayor(newMayor);

					if (oldMayor.isNPC()) {
						try {
							town.removeResident(oldMayor);
							TownyUniverse.getDataSource().removeResident(oldMayor);

							TownyUniverse.getDataSource().removeResidentList(oldMayor);

						} catch (EmptyTownException e) {
							// Should never reach here as we are setting a new
							// mayor before removing the old one.
							e.printStackTrace();
						}
					}
					TownyUniverse.getDataSource().saveTown(town);
					String[] msg = TownySettings.getNewMayorMsg(newMayor.getName());
					TownyMessaging.sendTownMessage(town, msg);
					// TownyMessaging.sendMessage(player, msg);
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
				}
			
		} else if (split[0].equalsIgnoreCase("capital") || split[0].equalsIgnoreCase("수도")) {

			if (split.length < 2) {
				
				sender.sendMessage(ChatTools.formatTitle("/타우니관리 설정 수도"));
				sender.sendMessage(ChatTools.formatCommand("예시", "/타우니관리 설정 수도", "[마을 이름]", ""));
				
			} else {
				
				try {
					Town newCapital = TownyUniverse.getDataSource().getTown(split[1]);
					
			        if ((TownySettings.getNumResidentsCreateNation() > 0) && (newCapital.getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
			            TownyMessaging.sendErrorMsg(this.player, String.format(TownySettings.getLangString("msg_not_enough_residents_capital"), newCapital.getName()));
			            return;
			        }
			        
					Nation nation = newCapital.getNation();
					
					nation.setCapital(newCapital);
					plugin.resetCache();
					
					TownyMessaging.sendNationMessage(nation, TownySettings.getNewKingMsg(newCapital.getMayor().getName(), nation.getName()));
					
					TownyUniverse.getDataSource().saveNation(nation);
					TownyUniverse.getDataSource().saveNationList();
					
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
				
			}
		} else if (split[0].equalsIgnoreCase("title") || split[0].equalsIgnoreCase("국가접두사")) {
			Resident resident = null;
			// Give the resident a title
			if (split.length < 2)
				TownyMessaging.sendErrorMsg(player, "예시: /타우니관리 설정 국가접두사 bilbo Jester");
			else
				resident = TownyUniverse.getDataSource().getResident(split[1]);
			
			split = StringMgmt.remArgs(split, 2);
			if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_input_too_long"));
				return;
			}

			String title = StringMgmt.join(NameValidation.checkAndFilterArray(split));
			resident.setTitle(title + " ");
			TownyUniverse.getDataSource().saveResident(resident);

			if (resident.hasTitle()){
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_set_title"), resident.getName(), resident.getTitle()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_set_title"), resident.getName(), resident.getTitle()));
			} else {
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_clear_title_surname"), "접두사", resident.getName()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_clear_title_surname"), "접두사", resident.getName()));
			}
			
		} else if (split[0].equalsIgnoreCase("surname") || split[0].equalsIgnoreCase("국가접미사")) {
			Resident resident = null;
			// Give the resident a surname
			if (split.length < 2)
				TownyMessaging.sendErrorMsg(player, "예시: /타우니관리 설정 국가접미사 bilbo Jester");
			else
				resident = TownyUniverse.getDataSource().getResident(split[1]);
			
			split = StringMgmt.remArgs(split, 2);
			if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_input_too_long"));
				return;
			}

			String surname = StringMgmt.join(NameValidation.checkAndFilterArray(split));
			resident.setSurname(surname + " ");
			TownyUniverse.getDataSource().saveResident(resident);

			if (resident.hasSurname()){
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_set_surname"), resident.getName(), resident.getSurname()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_set_surname"), resident.getName(), resident.getSurname()));
			} else {
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_clear_title_surname"), "접미사", resident.getName()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_clear_title_surname"), "접미사", resident.getName()));
			}

		} else {
			TownyMessaging.sendErrorMsg(getSender(), String.format(TownySettings.getLangString("msg_err_invalid_property"), "administrative"));
			return;
		}
	}

	public String nextNpcName() throws TownyException {

		String name;
		int i = 0;
		do {
			name = TownySettings.getNPCPrefix() + ++i;
			if (!TownyUniverse.getDataSource().hasResident(name))
				return name;
			if (i > 100000)
				throw new TownyException(TownySettings.getLangString("msg_err_too_many_npc"));
		} while (true);
	}

	public void reloadTowny(Boolean reset) {

		if (reset) {
			TownyUniverse.getDataSource().deleteFile(plugin.getConfigPath());
		}
		TownyLogger.shutDown();
		if (plugin.load()) {
			
			// Register all child permissions for ranks
			TownyPerms.registerPermissionNodes();
			
			// Update permissions for all online players
			TownyPerms.updateOnlinePerms();
						
		}

		TownyMessaging.sendMsg(sender, TownySettings.getLangString("msg_reloaded"));
		// TownyMessaging.sendMsg(TownySettings.getLangString("msg_reloaded"));
	}

	/**
	 * Remove residents who havn't logged in for X amount of days.
	 * 
	 * @param split
	 */
	public void purge(String[] split) {

		if (split.length == 0) {
			// command was '/townyadmin purge'
			player.sendMessage(ChatTools.formatTitle("/타우니관리 정리"));
			player.sendMessage(ChatTools.formatCommand("", "/타우니관리 정리", "[~~~일 묵은 데이터]", ""));
			player.sendMessage(ChatTools.formatCommand("", "", "설정한 기간 동안 접속하지 않은 주민의 데이터를 지웁니다.", ""));

			return;
		}

		int days = 1;

		try {
			days = Integer.parseInt(split[0]);
		} catch (NumberFormatException e) {
			TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_error_must_be_int"));
			return;
		}
		
		
		// Use questioner to confirm.
		Plugin test = BukkitTools.getServer().getPluginManager().getPlugin("Questioner");

		if (this.sender instanceof Player && TownySettings.isUsingQuestioner() && test != null && test instanceof Questioner && test.isEnabled()) {
			Questioner questioner = (Questioner) test;
			questioner.loadClasses();

			List<Option> options = new ArrayList<Option>();
			options.add(new Option(TownySettings.questionerAccept(), new PurgeQuestionTask(plugin, this.sender, TimeTools.getMillis(days + "d"))));
			options.add(new Option(TownySettings.questionerDeny(), new PurgeQuestionTask(plugin, this.sender, TimeTools.getMillis(days + "d")) {

				@Override
				public void run() {

					TownyMessaging.sendMessage(getSender(), "삭제가 취소되었습니다!");
				}
			}));
			
			Question question = new Question(this.sender.getName(), "정말로 삭제하시겠습니까?", options);
			
			try {
				plugin.appendQuestion(questioner, question);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		} else {

			// Run a purge in it's own thread
			new ResidentPurge(plugin, this.sender, TimeTools.getMillis(days + "d")).start();
		}

	}

	/**
	 * Delete a resident and it's data file (if not online) Available Only to
	 * players with the 'towny.admin' permission node.
	 * 
	 * @param player
	 * @param split
	 */
	public void residentDelete(Player player, String[] split) {

		if (split.length == 0)
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
		else
			try {
				if (!TownyUniverse.getPermissionSource().isTownyAdmin(player))
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only_delete"));

				for (String name : split) {
					try {
						Resident resident = TownyUniverse.getDataSource().getResident(name);
						if (!resident.isNPC() && !BukkitTools.isOnline(resident.getName())) {
							TownyUniverse.getDataSource().removeResident(resident);
							TownyUniverse.getDataSource().removeResidentList(resident);
							TownyMessaging.sendGlobalMessage(TownySettings.getDelResidentMsg(resident));
						} else
							TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_online_or_npc"), name));
					} catch (NotRegisteredException x) {
						// This name isn't registered as a resident
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_name"), name));
					}
				}
			} catch (TownyException x) {
				// Admin only escape
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
	}

	public void parseToggleCommand(String[] split) throws TownyException {

		boolean choice;

		if (split.length == 0) {
			// command was '/타우니관리 토글'
			player.sendMessage(ChatTools.formatTitle("/타우니관리 토글"));
			player.sendMessage(ChatTools.formatCommand("", "/타우니관리 토글", "전쟁", ""));
			player.sendMessage(ChatTools.formatCommand("", "/타우니관리 토글", "평화로움", ""));
			player.sendMessage(ChatTools.formatCommand("", "/타우니관리 토글", "개발모드", ""));
			player.sendMessage(ChatTools.formatCommand("", "/타우니관리 토글", "디버그", ""));
			player.sendMessage(ChatTools.formatCommand("", "/타우니관리 토글", "마을출금/국가출금", ""));
			player.sendMessage(ChatTools.formatCommand("", "/타우니관리 토글 npc", "[주민]", ""));
			return;

		}

		if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE.getNode(split[0].toLowerCase())))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		if (split[0].equalsIgnoreCase("war") || split[0].equalsIgnoreCase("전쟁")) {
			choice = TownyUniverse.isWarTime();

			if (!choice) {
				plugin.getTownyUniverse().startWarEvent();
				TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_war_started"));
			} else {
				plugin.getTownyUniverse().endWarEvent();
				TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_war_ended"));
			}
		} else if (split[0].equalsIgnoreCase("peaceful") || split[0].equalsIgnoreCase("neutral")
				|| split[0].equalsIgnoreCase("평화로움") || split[0].equalsIgnoreCase("중립")) {

			try {
				choice = !TownySettings.isDeclaringNeutral();
				TownySettings.setDeclaringNeutral(choice);
				TownyMessaging.sendMsg(getSender(), String.format(TownySettings.getLangString("msg_nation_allow_peaceful"), choice ? "활성" : "비활성"));

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
				return;
			}

		} else if (split[0].equalsIgnoreCase("devmode") || split[0].equalsIgnoreCase("개발모드")) {
			try {
				choice = !TownySettings.isDevMode();
				TownySettings.setDevMode(choice);
				TownyMessaging.sendMsg(getSender(), "개발모드 " + (choice ? Colors.Green + "활성" : Colors.Red + "비활성"));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("debug") || split[0].equalsIgnoreCase("디버그")) {
			try {
				choice = !TownySettings.getDebug();
				TownySettings.setDebug(choice);
				TownyMessaging.sendMsg(getSender(), "디버그 모드 " + (choice ? Colors.Green + "활성" : Colors.Red + "비활성"));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("townwithdraw") || split[0].equalsIgnoreCase("마을출금")) {
			try {
				choice = !TownySettings.getTownBankAllowWithdrawls();
				TownySettings.SetTownBankAllowWithdrawls(choice);
				TownyMessaging.sendMsg(getSender(), "마을 자금 출금 " + (choice ? Colors.Green + "활성" : Colors.Red + "비활성"));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("nationwithdraw") || split[0].equalsIgnoreCase("국가출금")) {
			try {
				choice = !TownySettings.geNationBankAllowWithdrawls();
				TownySettings.SetNationBankAllowWithdrawls(choice);
				TownyMessaging.sendMsg(getSender(), "국가 자금 출금 " + (choice ? Colors.Green + "활성" : Colors.Red + "비활성"));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
			
		} else if (split[0].equalsIgnoreCase("npc")) {
			
			if (split.length != 2)
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "예시: 토글 npc [주민]"));
			
			try {
				Resident resident = TownyUniverse.getDataSource().getResident(split[1]);
				resident.setNPC(!resident.isNPC());
				
				TownyUniverse.getDataSource().saveResident(resident);
				
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_npc_flag"), resident.isNPC(), resident.getName()));
				
			} catch (NotRegisteredException x) {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[1]));
			}
			
		} else {
			// parameter error message
			// peaceful/war/townmobs/worldmobs
			TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
		}
	}
	/*
	 * private void warSeed(Player player) { Resident r1 =
	 * plugin.getTownyUniverse().newResident("r1"); Resident r2 =
	 * plugin.getTownyUniverse().newResident("r2"); Resident r3 =
	 * plugin.getTownyUniverse().newResident("r3"); Coord key =
	 * Coord.parseCoord(player); Town t1 = newTown(plugin.getTownyUniverse(),
	 * player.getWorld(), "t1", r1, key, player.getLocation()); Town t2 =
	 * newTown(plugin.getTownyUniverse(), player.getWorld(), "t2", r2, new
	 * Coord(key.getX() + 1, key.getZ()), player.getLocation()); Town t3 =
	 * newTown(plugin.getTownyUniverse(), player.getWorld(), "t3", r3, new
	 * Coord(key.getX(), key.getZ() + 1), player.getLocation()); Nation n1 =
	 * 
	 * }
	 * 
	 * public void seedTowny() { TownyUniverse townyUniverse =
	 * plugin.getTownyUniverse(); Random r = new Random(); for (int i = 0; i <
	 * 1000; i++) {
	 * 
	 * try { townyUniverse.newNation(Integer.toString(r.nextInt())); } catch
	 * (TownyException e) { } try {
	 * townyUniverse.newTown(Integer.toString(r.nextInt())); } catch
	 * (TownyException e) { } try {
	 * townyUniverse.newResident(Integer.toString(r.nextInt())); } catch
	 * (TownyException e) { } } }
	 * 
	 * private static double getTotalEconomy() { double total = 0; try { return
	 * total; } catch (Exception e) { } return total; }
	 * 
	 * private static int getNumBankAccounts() { try { return 0; } catch
	 * (Exception e) { return 0; } }
	 */
}
