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
	
	// PermissionsEX Event Enums
	public enum PEXEntity_Action {
        PERMISSIONS_CHANGED,
        OPTIONS_CHANGED,
        INHERITANCE_CHANGED,
        INFO_CHANGED,
        TIMEDPERMISSION_EXPIRED,
        RANK_CHANGED,
        DEFAULTGROUP_CHANGED,
        //WEIGHT_CHANGED,
        //SAVED,
        REMOVED,
    }
	
	public enum PEXSystem_Action {
        //BACKEND_CHANGED,
        RELOADED,
        WORLDINHERITANCE_CHANGED,
        DEFAULTGROUP_CHANGED,
        //DEBUGMODE_TOGGLE,
    }
	
	
}