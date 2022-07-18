package com.palmergames.bukkit.towny.object.jail;

public enum JailReason {

	MAYOR("msg_jailed_mayor"), // hours and bail deprecated, books handled in own class.
	OUTLAW_DEATH("msg_jailed_outlaw"), // hours and bail deprecated, books handled in own class. 
	PRISONER_OF_WAR("msg_jailed_war");
	
	private final String cause;
	JailReason(String cause) {
		this.cause = cause;
	}
	public String getCause() {
		return cause;
	}
}
