package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.object.Translatable;

public class NoPermissionException extends TownyException {

	private static final long serialVersionUID = 1235059431924178033L;

	public NoPermissionException() {
		super(Translatable.of("msg_err_command_disable"));
	}

}
