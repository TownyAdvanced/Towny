package com.palmergames.bukkit.towny.command; /* Localized on 2014-05-02 by Neder */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;

/**
 * Send a list of all general townyworld help commands to player Command:
 * /townyworld
 */

public class TownyWorldCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> townyworld_help = new ArrayList<String>();
	private static final List<String> townyworld_set = new ArrayList<String>();
	private static TownyWorld Globalworld;

	public TownyWorldCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		townyworld_help.add(ChatTools.formatTitle("/타우니월드"));
		townyworld_help.add(ChatTools.formatCommand("", "/타우니월드", "", TownySettings.getLangString("world_help_1")));
		townyworld_help.add(ChatTools.formatCommand("", "/타우니월드", TownySettings.getLangString("world_help_2"), TownySettings.getLangString("world_help_3")));
		townyworld_help.add(ChatTools.formatCommand("", "/타우니월드", "목록", TownySettings.getLangString("world_help_4")));
		townyworld_help.add(ChatTools.formatCommand("", "/타우니월드", "토글", ""));
		townyworld_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니월드", "설정 [] .. []", ""));
		townyworld_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/타우니월드", "재생성", TownySettings.getLangString("world_help_5")));

		townyworld_set.add(ChatTools.formatTitle("/타우니월드 설정"));
		townyworld_set.add(ChatTools.formatCommand("", "/타우니월드 설정", "야생이름 [이름]", ""));
		// townyworld_set.add(ChatTools.formatCommand("", "/타우니월드 설정",
		// "usingtowny [on/off]", ""));

		// if using permissions and it's active disable this command
		if (!plugin.isPermissions()) {
			townyworld_set.add(ChatTools.formatCommand("", "/타우니월드 설정", "기본값사용", ""));
			townyworld_set.add(ChatTools.formatCommand("", "/타우니월드 설정", "야생권한 [권한] .. [권한]", "건축,파괴,스위치,아이템사용"));
			townyworld_set.add(ChatTools.formatCommand("", "/타우니월드 설정", "야생예외 [id] [id] [id]", ""));
		}

		if (sender instanceof Player) {
			Player player = (Player) sender;
		}
		parseWorldCommand(sender, args);
		/*
		 * } else { // Console for (String line : townyworld_help)
		 * sender.sendMessage(Colors.strip(line)); }
		 */

		townyworld_set.clear();
		townyworld_help.clear();
		Globalworld = null;
		return true;
	}

	public void parseWorldCommand(CommandSender sender, String[] split) {

		Player player = null;

		if (sender instanceof Player) {
			player = (Player) sender;
			try {
				Globalworld = TownyUniverse.getDataSource().getWorld(player.getWorld().getName());
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_area_not_recog"));
				return;
			}
		} else {
			if (split.length == 0) {
				sender.sendMessage(String.format(TownySettings.getLangString("msg_err_invalid_property"), "월드"));
				return;
			}
			if ((!split[0].equalsIgnoreCase("?")) && !split[0].equalsIgnoreCase("list") && (!split[0].equalsIgnoreCase("목록")))
				try {
					if ((split.length >= 1)) {
						Globalworld = TownyUniverse.getDataSource().getWorld(split[split.length - 1].toLowerCase());
						split = StringMgmt.remLastArg(split);
					} else {
						sender.sendMessage(TownySettings.getLangString("msg_area_not_recog"));
						return;
					}

				} catch (NotRegisteredException e) {
					sender.sendMessage(String.format(TownySettings.getLangString("msg_err_invalid_property"), "월드"));
					return;
				}

		}

		if (split.length == 0) {
			if (player == null) {
				for (String line : TownyFormatter.getStatus(Globalworld))
					sender.sendMessage(Colors.strip(line));
			} else
				TownyMessaging.sendMessage(player, TownyFormatter.getStatus(Globalworld));

			return;
		}

		try {

			if (split[0].equalsIgnoreCase("?")) {
				if (player == null) {
					for (String line : townyworld_help)
						sender.sendMessage(line);
				} else
					for (String line : townyworld_help)
						player.sendMessage(line);
			} else if (split[0].equalsIgnoreCase("list") || split[0].equalsIgnoreCase("목록")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_LIST.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				listWorlds(player, sender);

			} else if (split[0].equalsIgnoreCase("set") || split[0].equalsIgnoreCase("설정")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_SET.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				worldSet(player, sender, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("toggle") || split[0].equalsIgnoreCase("토글")) {

				worldToggle(player, sender, StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("regen") || split[0].equalsIgnoreCase("재생성")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_REGEN.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (TownyUniverse.isWarTime()) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_war_cannot_do"));
					return;
				}

				if (!TownyUniverse.getPermissionSource().isTownyAdmin(player)) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_admin_only"));
					return;
				}

				if (TownySettings.getTownBlockSize() != 16) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_plot_regen_wrong_size"));
					return;
				}

				// Regen this chunk
				if (player != null) {
					TownyRegenAPI.regenChunk(player);
				}

			} else if (split[0].equalsIgnoreCase("undo") || split[0].equalsIgnoreCase("되돌리기")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_UNDO.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (player != null)
					try {
						TownyUniverse.getDataSource().getResident(player.getName()).regenUndo();
					} catch (NotRegisteredException e) {
						// Failed to get resident
					}

			} else {
				/*
				 * try { TownyWorld world =
				 * plugin.getTownyUniverse().getWorld(split[0]);
				 * TownyMessaging.sendMessage(player,
				 * plugin.getTownyUniverse().getStatus(world)); } catch
				 * (NotRegisteredException x) { plugin.sendErrorMsg(player,
				 * String.format(TownySettings.getLangString
				 * ("msg_err_not_registered_1"), split[0])); }
				 */
			}

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}

	public void listWorlds(Player player, CommandSender sender) {

		if (player == null) {
			sender.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("world_plu")));
		} else
			player.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("world_plu")));

		ArrayList<String> formatedList = new ArrayList<String>();
		HashMap<String, Integer> playersPerWorld = BukkitTools.getPlayersPerWorld();
		for (TownyWorld world : TownyUniverse.getDataSource().getWorlds()) {
			int numPlayers = playersPerWorld.containsKey(world.getName()) ? playersPerWorld.get(world.getName()) : 0;
			formatedList.add(Colors.LightBlue + world.getName() + Colors.Blue + " [" + numPlayers + "]" + Colors.White);
		}

		if (player == null) {
			for (String line : ChatTools.list(formatedList))
				sender.sendMessage(line);
		} else
			for (String line : ChatTools.list(formatedList))
				player.sendMessage(line);
	}

	public void worldToggle(Player player, CommandSender sender, String[] split) throws TownyException {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/타우니월드 토글"));
			player.sendMessage(ChatTools.formatCommand("", "/타우니월드 토글", "점유가능", ""));
			player.sendMessage(ChatTools.formatCommand("", "/타우니월드 토글", "타우니사용", ""));
			player.sendMessage(ChatTools.formatCommand("", "/타우니월드 토글", "pvp/강제적pvp", ""));
			player.sendMessage(ChatTools.formatCommand("", "/타우니월드 토글", "폭발/강제적폭발", ""));
			player.sendMessage(ChatTools.formatCommand("", "/타우니월드 토글", "불번짐/강제적불번짐", ""));
			player.sendMessage(ChatTools.formatCommand("", "/타우니월드 토글", "마을몹/월드몹", ""));
			player.sendMessage(ChatTools.formatCommand("", "/타우니월드 토글", "점유해제지형복구/폭발되돌리기", ""));
		} else {

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYWORLD_TOGGLE.getNode(split[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			String msg;

			if (split[0].equalsIgnoreCase("claimable") || split[0].equalsIgnoreCase("점유가능")) {

				Globalworld.setClaimable(!Globalworld.isClaimable());
				msg = String.format(TownySettings.getLangString("msg_set_claim"), Globalworld.getName(), Globalworld.isClaimable() ? "활성" : "비활성");
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("usingtowny") || split[0].equalsIgnoreCase("타우니사용")) {

				Globalworld.setUsingTowny(!Globalworld.isUsingTowny());
				plugin.resetCache();
				msg = String.format(Globalworld.isUsingTowny() ? TownySettings.getLangString("msg_set_use_towny_on") : TownySettings.getLangString("msg_set_use_towny_off"));
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("pvp")) {

				Globalworld.setPVP(!Globalworld.isPVP());
				msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), Globalworld.getName(), "월드 내 PVP",Globalworld.isPVP() ? "활성" : "비활성");
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("forcepvp") || split[0].equalsIgnoreCase("강제적pvp")) {

				Globalworld.setForcePVP(!Globalworld.isForcePVP());
				msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), Globalworld.getName(), "강제적인 마을 내 PvP 허용", Globalworld.isForcePVP() ? "강제적" : "조정 가능");
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("explosion") || split[0].equalsIgnoreCase("폭발")) {

				Globalworld.setExpl(!Globalworld.isExpl());
				msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), Globalworld.getName(), "폭발", Globalworld.isExpl() ? "활성" : "비활성");
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("forceexplosion") || split[0].equalsIgnoreCase("강제적폭발")) {

				Globalworld.setForceExpl(!Globalworld.isForceExpl());
				msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), Globalworld.getName(), "강제적인 마을 내 폭발 허용", Globalworld.isForceExpl() ? "강제적" : "조정 가능");
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("fire") || split[0].equalsIgnoreCase("불번짐")) {

				Globalworld.setFire(!Globalworld.isFire());
				msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), Globalworld.getName(), "불번짐", Globalworld.isFire() ? "활성" : "비활성");
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("forcefire") || split[0].equalsIgnoreCase("강제적불번짐")) {

				Globalworld.setForceFire(!Globalworld.isForceFire());
				msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), Globalworld.getName(), "강제적인 마을 내 불번짐 허용", Globalworld.isForceFire() ? "강제적" : "조정 가능");
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("townmobs") || split[0].equalsIgnoreCase("마을몹")) {

				Globalworld.setForceTownMobs(!Globalworld.isForceTownMobs());
				msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), Globalworld.getName(), "마을 몹 스폰", Globalworld.isForceTownMobs() ? "강제적" : "조정 가능");
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("worldmobs") || split[0].equalsIgnoreCase("월드몹")) {

				Globalworld.setWorldMobs(!Globalworld.hasWorldMobs());
				msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), Globalworld.getName(), "월드 몹 스폰", Globalworld.hasWorldMobs() ? "활성" : "비활성");
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("revertunclaim") || split[0].equalsIgnoreCase("점유해제지형복구")) {

				Globalworld.setUsingPlotManagementRevert(!Globalworld.isUsingPlotManagementRevert());
				msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), Globalworld.getName(), "점유해제시 지형 원상복구", Globalworld.isUsingPlotManagementRevert() ? "활성" : "비활성");
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else if (split[0].equalsIgnoreCase("revertexpl") || split[0].equalsIgnoreCase("폭발되돌리기")) {

				Globalworld.setUsingPlotManagementWildRevert(!Globalworld.isUsingPlotManagementWildRevert());
				msg = String.format(TownySettings.getLangString("msg_changed_world_setting"), Globalworld.getName(), "야생구역 폭발 발생시 지형 복구", Globalworld.isUsingPlotManagementWildRevert() ? "활성" : "비활성");
				if (player != null)
					TownyMessaging.sendMsg(player, msg);
				else
					TownyMessaging.sendMsg(msg);

			} else {
				msg = String.format(TownySettings.getLangString("msg_err_invalid_property"), "'" + split[0] + "'");
				if (player != null)
					TownyMessaging.sendErrorMsg(player, msg);
				else
					TownyMessaging.sendErrorMsg(msg);
				return;
			}
			
			TownyUniverse.getDataSource().saveWorld(Globalworld);
			
			//Change settings event
			TownBlockSettingsChangedEvent event = new TownBlockSettingsChangedEvent(Globalworld);
			Bukkit.getServer().getPluginManager().callEvent(event);
		}

	}

	public void worldSet(Player player, CommandSender sender, String[] split) {

		if (split.length == 0) {
			if (player == null) {
				for (String line : townyworld_set)
					sender.sendMessage(line);
			} else {
				for (String line : townyworld_set)
					player.sendMessage(line);
			}
		} else {

			if (split[0].equalsIgnoreCase("usedefault") || split[0].equalsIgnoreCase("기본값사용")) {

				Globalworld.setUsingDefault();
				plugin.resetCache();
				if (player != null)
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_usedefault"), Globalworld.getName()));
				else
					sender.sendMessage(String.format(TownySettings.getLangString("msg_usedefault"), Globalworld.getName()));

			} else if (split[0].equalsIgnoreCase("wildperm") || split[0].equalsIgnoreCase("야생권한")) {

				if (split.length < 2) {
					// set default wildperm settings (/tw set wildperm)
					Globalworld.setUsingDefault();
					if (player != null)
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_usedefault"), Globalworld.getName()));
					else
						sender.sendMessage(String.format(TownySettings.getLangString("msg_usedefault"), Globalworld.getName()));
				} else
					try {
						List<String> perms = Arrays.asList(StringMgmt.remFirstArg(split));
						Globalworld.setUnclaimedZoneBuild(perms.contains("build") || perms.contains("건축"));
						Globalworld.setUnclaimedZoneDestroy(perms.contains("destroy") || perms.contains("파괴"));
						Globalworld.setUnclaimedZoneSwitch(perms.contains("switch") || perms.contains("스위치"));
						Globalworld.setUnclaimedZoneItemUse(perms.contains("itemuse") || perms.contains("아이템사용"));

						plugin.resetCache();
						if (player != null)
							TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_wild_perms"), Globalworld.getName(), perms.toString()));
						else
							sender.sendMessage(String.format(TownySettings.getLangString("msg_set_wild_perms"), Globalworld.getName(), perms.toString()));
					} catch (Exception e) {
						if (player != null)
							TownyMessaging.sendErrorMsg(player, "예시: /타우니월드 설정 야생권한 건축 파괴");
						else
							sender.sendMessage("예시: /타우니월드 설정 야생권한 건축 파괴 <월드>");
					}

			} else if (split[0].equalsIgnoreCase("wildignore") || split[0].equalsIgnoreCase("야생예외")) {

				if (split.length < 2)
					if (player != null)
						TownyMessaging.sendErrorMsg(player, "예시: /타우니월드 설정 야생예외 SAPLING,GOLD_ORE,IRON_ORE");
					else
						sender.sendMessage("예시: /타우니월드 설정 야생예외 SAPLING,GOLD_ORE,IRON_ORE <월드>");
				else
					try {
						List<String> mats = new ArrayList<String>();
						for (String s : StringMgmt.remFirstArg(split))
							mats.add(Material.matchMaterial(s.trim().toUpperCase()).name());

						Globalworld.setUnclaimedZoneIgnore(mats);

						plugin.resetCache();
						if (player != null)
							TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_wild_ignore"), Globalworld.getName(), Globalworld.getUnclaimedZoneIgnoreMaterials()));
						else
							sender.sendMessage(String.format(TownySettings.getLangString("msg_set_wild_ignore"), Globalworld.getName(), Globalworld.getUnclaimedZoneIgnoreMaterials()));

					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_input"), " 켜기/끄기."));
					}

			} else if (split[0].equalsIgnoreCase("wildregen") || split[0].equalsIgnoreCase("야생재생성")) {

				if (split.length < 2)
					if (player != null)
						TownyMessaging.sendErrorMsg(player, "예시: /타우니월드 설정 야생재생성 Creeper,EnderCrystal,EnderDragon,Fireball,SmallFireball,LargeFireball,TNTPrimed,ExplosiveMinecart");
					else
						sender.sendMessage("예시: /타우니월드 설정 야생재생성 Creeper,EnderCrystal,EnderDragon,Fireball,SmallFireball,LargeFireball,TNTPrimed,ExplosiveMinecart <월드>");
				else {

					List<String> entities = new ArrayList<String>(Arrays.asList(StringMgmt.remFirstArg(split)));

					Globalworld.setPlotManagementWildRevertEntities(entities);

					if (player != null)
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_wild_regen"), Globalworld.getName(), Globalworld.getPlotManagementWildRevertEntities()));
					else
						sender.sendMessage(String.format(TownySettings.getLangString("msg_set_wild_regen"), Globalworld.getName(), Globalworld.getPlotManagementWildRevertEntities()));

				}

			} else if (split[0].equalsIgnoreCase("wildname") || split[0].equalsIgnoreCase("야생이름")) {

				if (split.length < 2) {
					if (player != null)
						TownyMessaging.sendErrorMsg(player, "예시: /타우니월드 설정 야생이름 Wildy");
				} else
					try {
						Globalworld.setUnclaimedZoneName(split[1]);

						if (player != null)
							TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_set_wild_name"), Globalworld.getName(), split[1]));
						else
							sender.sendMessage(String.format(TownySettings.getLangString("msg_set_wild_name"), Globalworld.getName(), split[1]));
					} catch (Exception e) {
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_input"), " 켜기/끄기."));
					}
			} else {
				if (player != null)
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "월드"));
				return;
			}

			TownyUniverse.getDataSource().saveWorld(Globalworld);
		}
	}

}
