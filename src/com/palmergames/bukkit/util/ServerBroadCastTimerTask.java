package com.palmergames.bukkit.util;

import java.util.TimerTask;

import com.palmergames.bukkit.towny.war.eventwar.instance.War;

public class ServerBroadCastTimerTask extends TimerTask {

	private String msg;
	private War war;

	public ServerBroadCastTimerTask(War war, String msg) {

		this.msg = msg;
		this.war = war;
	}

	@Override
	public void run() {

		war.getMessenger().sendGlobalMessage(msg);
	}

}
