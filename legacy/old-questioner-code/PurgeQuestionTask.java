package com.palmergames.bukkit.towny.questioner;

import org.bukkit.command.CommandSender;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.tasks.ResidentPurge;


public class PurgeQuestionTask extends TownyQuestionTask {

	protected Towny towny;
	protected CommandSender sender;
	protected long time;

	public PurgeQuestionTask(Towny plugin, CommandSender sender, long deleteTime) {

		this.towny = plugin;
		this.sender = sender;
		this.time = deleteTime;
	}

	public CommandSender getSender() {

		return sender;
	}
	
	public long time() {

		return time;
	}

	@Override
	public void run() {

		// Run a purge in it's own thread
		new ResidentPurge(towny, getSender(), time).start();
	}
}
