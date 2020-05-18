package com.palmergames.bukkit.towny.object;

import org.bukkit.entity.Player;

public class Transaction {
	private final TransactionType type;
	private final Player player;
	private final int amount;
	
	public Transaction(TransactionType type, Player player, int amount) {
		this.type = type;
		this.player = player;
		this.amount = amount;
	}

	public TransactionType getType() {
		return type;
	}

	public Player getPlayer() {
		return player;
	}

	public int getAmount() {
		return amount;
	}
}
