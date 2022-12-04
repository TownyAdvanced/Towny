package com.palmergames.bukkit.towny.object.jail;

public enum UnJailReason {

	LEFT_TOWN("Left Town"),
	PARDONED("Pardoned"),
	ESCAPE("Escaped"),
	BAIL("Paid Bail"),
	SENTENCE_SERVED("Sentence Served"),
	JAILBREAK("JailBreak"),
	JAIL_DELETED(""),
	OUT_OF_SPACE("Released For Space"),
	INSUFFICIENT_FUNDS("Insufficient Funds"),
	ADMIN("Freed by an Admin");
	
	private final String cause;
	UnJailReason(String cause) {
		this.cause = cause;
	}
	public String getCause() {
		return cause;
	}
}
