package com.palmergames.bukkit.towny.object.jail;

public enum JailReason {

	MAYOR("msg_jailed_mayor"),
	OUTLAW_DEATH("msg_jailed_outlaw"),
	PRISONER_OF_WAR("msg_jailed_war");
	
	private final String cause;
	JailReason(String cause) {
		this.cause = cause;
	}
	public String getCause() {
		return cause;
	}
}
