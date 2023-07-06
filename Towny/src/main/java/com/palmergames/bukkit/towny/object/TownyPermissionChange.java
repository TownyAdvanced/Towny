package com.palmergames.bukkit.towny.object;

/**
 * A class that represents a permission change to a town block owner.
 * This class can be used to cache a permission change and apply it to multiple town block owners efficiently.
 */
public class TownyPermissionChange {

	// Enum represents a permission change action
	public enum Action {
		ALL_PERMS, SINGLE_PERM, PERM_LEVEL, ACTION_TYPE, RESET
	}

	private final Object[] args;
	private final Action changeAction;
	private final boolean changeValue;

	public TownyPermissionChange(Action changeAction, boolean changeValue, Object... args) {
		this.changeAction = changeAction;
		this.changeValue = changeValue;
		this.args = args;
	}
	
	public Action getChangeAction() {
		return changeAction;
	}
	
	public boolean getChangeValue() {
		return changeValue;
	}

	public Object[] getArgs() {
		return args;
	}
}
