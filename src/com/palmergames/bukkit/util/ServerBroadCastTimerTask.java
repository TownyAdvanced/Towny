package com.palmergames.bukkit.util;

import java.util.TimerTask;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.palmergames.bukkit.towny.TownyMessaging;

public class ServerBroadCastTimerTask extends TimerTask {

	private JavaPlugin plugin;
	private String msg;

	public ServerBroadCastTimerTask(JavaPlugin plugin, String msg) {

		this.plugin = plugin;
		this.msg = msg;
	}

	@Override
	public void run() {

		for (Player player : plugin.getServer().getOnlinePlayers())
			TownyMessaging.sendMessage(player, msg);
	}

}
