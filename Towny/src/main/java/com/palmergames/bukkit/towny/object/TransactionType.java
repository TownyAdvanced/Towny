package com.palmergames.bukkit.towny.object;

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
