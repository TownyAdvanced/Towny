package com.palmergames.bukkit.towny.object;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;


/**
 * @deprecated since 0.100.4.0 use {@link com.palmergames.bukkit.towny.object.economy.transaction.Transaction} instead.
 */
@Deprecated
public class Transaction {
	private final TransactionType type;
	private final CommandSender sender;
	private final double amount;
	
	public Transaction(TransactionType type, CommandSender sender, double amount) {
		this.type = type;
		this.sender = sender;
		this.amount = amount;
	}

	public TransactionType getType() {
		return type;
	}

	public CommandSender getCommandSender() {
		return sender;
	}

	/**
	 * @deprecated since 0.100.2.10 use {@link #getCommandSender()} instead.
	 * @return the Player or null if the command came from the console.
	 */
	@Deprecated
	@Nullable
	public Player getPlayer() {
		return sender instanceof Player player ? player : null;
	}

	public double getAmount() {
		return amount;
	}
}
