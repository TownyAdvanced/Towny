package com.palmergames.bukkit.towny.object.jail;

public enum JailReason {

	MAYOR("msg_jailed_mayor"), // for mayor arrests hours and bail are now set at time of jail.
	OUTLAW_DEATH("msg_jailed_outlaw"), // for outlaws arrest hours and bail are now set in configuration
	PRISONER_OF_WAR("msg_jailed_war");
	
	private final String cause;
	JailReason(String cause) {
		this.cause = cause;
	}
	public String getCause() {
		return cause;
	}
}
