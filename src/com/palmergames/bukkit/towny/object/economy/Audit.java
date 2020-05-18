package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.towny.object.TransactionType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

	@Override
	public String toString() {

		String pattern = "MM/dd/yy";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		
		switch (getType()) {
			case ADD:
			case DEPOSIT:
				return ChatColor.DARK_GREEN + "+" + TownyEconomyHandler.getFormattedBalance(getAmount()) + ChatColor.RESET + " - " + simpleDateFormat.format(date);
			case SUBTRACT:
			case WITHDRAW:
				return ChatColor.RED + "-" + TownyEconomyHandler.getFormattedBalance(getAmount());
		}
		
		return "null";
	}
}
