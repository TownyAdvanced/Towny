package com.palmergames.bukkit.towny.war.eventwar.instance;

import java.util.List;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;

public class WarMessenger {

	private static War war;

	public WarMessenger(War war) {
		WarMessenger.war = war;
	}

	private static List<Player> getPlayers() {
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
	
	public void sendGlobalMessage(Translatable translatable) {
		for (Player player : getPlayers())
			TownyMessaging.sendMsg(player, translatable);
	}
}
