package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.object.Resident;

public class Confirmation {
	private Resident resident;
	private Runnable handler;
	
	public Confirmation(Resident resident) {
		this.resident = resident;
	}

	public Resident getResident() {
		return resident;
	}

	public Runnable getHandler() {
		return handler;
	}

	public void setHandler(Runnable handler) {
		this.handler = handler;
	}
}
