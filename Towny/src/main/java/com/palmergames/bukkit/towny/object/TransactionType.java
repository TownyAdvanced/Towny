package com.palmergames.bukkit.towny.object;


/**
 * @deprecated since 0.100.4.0 use {@link com.palmergames.bukkit.towny.object.economy.transaction.TransactionType} instead.
 */
@Deprecated
public enum TransactionType {
	DEPOSIT("Deposit"), WITHDRAW("Withdraw"), ADD("Add"), SUBTRACT("Subtract");
	
	private final String name;
	
	TransactionType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
