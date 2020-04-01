package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.TownyObject;
import org.bukkit.entity.Player;

public class TownSpawnConfirmation {
	private TownyObject townyObject;
	private Player player;
	private String[] split;
	private String notAffordMSG;
	private boolean outpost;
	private SpawnType spawnType;
	
	public TownSpawnConfirmation(Player player, String[] split, TownyObject townyObject, String notAffordMSG, boolean outpost, SpawnType spawnType) {
		this.townyObject = townyObject;
		this.player = player;
		this.notAffordMSG = notAffordMSG;
		this.outpost = outpost;
		this.spawnType = spawnType;
		this.split = split;
	}

	public TownyObject getTownyObject() {
		return townyObject;
	}

	public Player getPlayer() {
		return player;
	}

	public String[] getSplit() {
		return split;
	}

	public String getNotAffordMSG() {
		return notAffordMSG;
	}

	public boolean isOutpost() {
		return outpost;
	}

	public SpawnType getSpawnType() {
		return spawnType;
	}
}
