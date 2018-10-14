package com.palmergames.bukkit.towny.questioner;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Town;

public class TownQuestionTask extends TownyQuestionTask {

	protected Player player;
	protected Town town;

	public TownQuestionTask(Player player, Town town) {

		this.player = player;
		this.town = town;
	}

	public Player getSender() {

		return player;
	}
	
	public Town getTown() {

		return town;
	}

	@Override
	public void run() {

	}

}
