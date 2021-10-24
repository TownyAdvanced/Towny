package com.palmergames.bukkit.towny.war.eventwar.instance;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;

public class WarMessenger {

	private War war;

	public WarMessenger(War war) {
		this.war = war;
	}

	private List<Player> getPlayers() {
		return war.getWarParticipants().getOnlineWarriors();
	}

	public void sendGlobalMessage(String msg) {
		for (Player player : getPlayers())
			player.sendMessage(Translation.of("default_towny_prefix") + msg);
	}

	public void sendGlobalMessage(List<String> lines) {
		for (String line : lines)
			sendGlobalMessage(line);
	}
	
	public void sendPlainGlobalMessage(String msg) {
		for (Player player : getPlayers())
			player.sendMessage(msg);
	}
	
	public void sendPlainGlobalMessage(List<String> lines) {
		for (Player player : getPlayers())
			for (String line : lines)
				player.sendMessage(line);
	}
	
	public void sendPlainGlobalMessage(Translatable translatable) {
		for (Player player : getPlayers())
			TownyMessaging.sendMessage(player, translatable);
	}
	
	public void sendGlobalMessage(Translatable translatable) {
		for (Player player : getPlayers())
			TownyMessaging.sendMsg(player, translatable);
	}
	
	public void reportErrors() {
		for (Player player : Bukkit.getOnlinePlayers())
			if (player.isOp() || TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player)) {
				TownyMessaging.sendErrorMsg(player, "The war could not be started, this may be helpful:");
				for (String line : war.getErrorMsgs())
					TownyMessaging.sendErrorMsg(player, line);
			}
	}
	
	public void reportMinorErrors() {
		for (Player player : getPlayers())
			for (String line : war.getErrorMsgs())
				TownyMessaging.sendErrorMsg(player, line);
	}

	public void announceWarBeginning() {
		switch (war.getWarType()) {
		case WORLDWAR:
		case NATIONWAR:
			for (Nation nation : war.getWarParticipants().getNations()) 
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_you_have_joined_a_war_of_type", war.getWarType().getName()));
			break;
		case CIVILWAR:
		case TOWNWAR:
			for (Town town : war.getWarParticipants().getTowns())
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_you_have_joined_a_war_of_type", war.getWarType().getName()));
			break;
		case RIOT:
			for (Resident res : war.getWarParticipants().getResidents())
				TownyMessaging.sendMsg(res, Translatable.of("msg_you_have_joined_a_war_of_type", war.getWarType().getName()));
			break;
		}
	}
}
