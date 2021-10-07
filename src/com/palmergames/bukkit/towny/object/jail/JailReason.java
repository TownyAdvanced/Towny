package com.palmergames.bukkit.towny.object.jail;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Translation;

public enum JailReason {

	MAYOR(Translation.of("msg_jailed_mayor"), 1), // hours is unused, always specified by the mayor when jailing. 
	OUTLAW_DEATH(Translation.of("msg_jailed_outlaw"), TownySettings.getJailedOutlawJailHours()),
	PRISONER_OF_WAR(Translation.of("msg_jailed_war"), 99);
	
	private final String cause;
	private final int hours;
	JailReason(String cause, int hours) {
		this.cause = cause;
		this.hours = hours;
	}
	public String getCause() {
		return cause;
	}
	public int getHours() {
		return hours;
	}
}
