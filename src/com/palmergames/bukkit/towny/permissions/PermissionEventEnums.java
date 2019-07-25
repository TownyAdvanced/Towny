package com.palmergames.bukkit.towny.permissions;

/**
 * @author ElgarL
 * 
 */
public class PermissionEventEnums {

	// GroupManager Event Enums
	public enum GMUser_Action {
		USER_PERMISSIONS_CHANGED,
		USER_INHERITANCE_CHANGED,
		USER_INFO_CHANGED,
		USER_GROUP_CHANGED,
		USER_SUBGROUP_CHANGED,
		USER_ADDED,
		USER_REMOVED,
	}

	public enum GMGroup_Action {
		GROUP_PERMISSIONS_CHANGED,
		GROUP_INHERITANCE_CHANGED,
		GROUP_INFO_CHANGED,
		//GROUP_ADDED,
		GROUP_REMOVED,
	}

	public enum GMSystem_Action {
		RELOADED,
		//SAVED,
		DEFAULT_GROUP_CHANGED,
		//VALIDATE_TOGGLE,
	}
}