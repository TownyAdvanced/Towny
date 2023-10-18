package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.object.Translatable;

public class ResidentNPCException extends TownyException {

	private static final long serialVersionUID = 6165509444120626464L;

	public ResidentNPCException() {
		super(Translatable.of("msg_err_resident_is_npc"));
	}
	
	public ResidentNPCException(Translatable errormsg) {
		super(errormsg);
	}

}
