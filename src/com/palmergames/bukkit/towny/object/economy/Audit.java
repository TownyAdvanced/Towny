package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.towny.object.TransactionType;
import org.bukkit.entity.Player;

import java.util.Date;

/**
 * An object that stores a record of when
 * and why a transaction occurred.
 */
public class Audit extends Transaction {
	
	private final Date date;
	private final String reason;
	
	public Audit(TransactionType type, Player player, int amount, String reason) {
		super(type, player, amount);
		
		// Set date to now
		date = new Date();
		this.reason = reason;
	}

	public Date getDate() {
		return date;
	}

	public String getReason() {
		return reason;
	}
}
