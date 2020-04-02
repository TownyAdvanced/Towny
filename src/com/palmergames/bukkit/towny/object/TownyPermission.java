package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.Colors;

import java.util.Arrays;

public class TownyPermission {
	public enum ActionType {
		BUILD (0, "Build"),
		DESTROY (1, "Destroy"),
		SWITCH (2, "Switch"),
		ITEM_USE (3, "ItemUse");

		// This is a static copy of the values to avoid Enum.values() which copies all the values to a new array
		// Since this is MUTABLE, we don't have a public call to it, restricting it to this class
		private static final ActionType[] values = ActionType.values();
		
		private final int index;
		private final String commonName;

		ActionType(int index, String commonName) {
			this.index = index;
			this.commonName = commonName;
		}
		
		public int getIndex() {
			return index;
		}
		
		public String getCommonName() {
			return commonName;
		}
		
		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
	}

	public enum PermLevel {
		RESIDENT (0, 'f'), NATION (1, 'n'), ALLY (2, 'a'), OUTSIDER (3, 'o');
		
		private static final PermLevel[] values = PermLevel.values();
		
		private final int index;
		private final char shortVal;
		
		PermLevel(int index, char shortVal) {
			this.index = index;
			this.shortVal = shortVal;
		}
		
		public int getIndex() {
			return index;
		}
		
		public char getShortChar() {
			return shortVal;
		}

		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
	}
	
	// Towny permissions are split into Action Type and Permission Level
	// So they can inherently be represented by a 2d array
	protected boolean[][] perms;
	
	public boolean pvp, fire, explosion, mobs;

	public TownyPermission() {
		// Fill the perms array
		perms = new boolean[PermLevel.values.length][ActionType.values.length];
		
		reset();
	}

	public void reset() {
		setAll(false);
	}
	
	public void change(TownyPermissionChange permChange) {
		change(permChange.getChangeAction(), permChange.getChangeValue(), permChange.getArgs());
	}
	
	public void change(TownyPermissionChange.Action permChange, boolean toValue, Object... args) {
		// Sorted by most common to least common
		if (permChange == TownyPermissionChange.Action.SINGLE_PERM && args.length == 2) {
			perms[((PermLevel) args[0]).getIndex()][((ActionType) args[1]).getIndex()] = toValue;
		}
		else if (permChange == TownyPermissionChange.Action.PERM_LEVEL && args.length == 1) {
			Arrays.fill(perms[((PermLevel) args[0]).getIndex()], toValue);
		}
		else if (permChange == TownyPermissionChange.Action.ACTION_TYPE && args.length == 1) {
			for (PermLevel permLevel : PermLevel.values) {
				perms[permLevel.getIndex()][((ActionType) args[0]).getIndex()] = toValue;
			}
		}
		else if (permChange == TownyPermissionChange.Action.ALL_PERMS) {
			setAllNonEnvironmental(toValue);
		}
		else if (permChange == TownyPermissionChange.Action.RESET && args.length == 1) {
			TownBlock tb = (TownBlock) args[0];
			tb.setType(tb.getType());
		}
	}
	
	public void setAllNonEnvironmental(boolean b) {
		for (boolean[] permLevel : perms) {
			Arrays.fill(permLevel, b);
		}
	}

	public void setAll(boolean b) {
		setAllNonEnvironmental(b);
		
		pvp = b;
		fire = b;
		explosion = b;
		mobs = b;
	}

	// TODO Restructure how perms are saved
	public void set(String s, boolean b) {
		
		switch (s.toLowerCase()) {
			case "denyall":
				reset();
				break;
			case "residentbuild":
				perms[PermLevel.RESIDENT.getIndex()][ActionType.BUILD.getIndex()] = b;
				break;
			case "residentdestroy":
				perms[PermLevel.RESIDENT.getIndex()][ActionType.DESTROY.getIndex()] = b;
				break;
			case "residentswitch":
				perms[PermLevel.RESIDENT.getIndex()][ActionType.SWITCH.getIndex()] = b;
				break;
			case "residentitemuse":
				perms[PermLevel.RESIDENT.getIndex()][ActionType.ITEM_USE.getIndex()] = b;
				break;
			case "outsiderbuild":
				perms[PermLevel.OUTSIDER.getIndex()][ActionType.BUILD.getIndex()] = b;
				break;
			case "outsiderdestroy":
				perms[PermLevel.OUTSIDER.getIndex()][ActionType.DESTROY.getIndex()] = b;
				break;
			case "outsiderswitch":
				perms[PermLevel.OUTSIDER.getIndex()][ActionType.SWITCH.getIndex()] = b;
				break;
			case "outsideritemuse":
				perms[PermLevel.OUTSIDER.getIndex()][ActionType.ITEM_USE.getIndex()] = b;
				break;
			case "nationbuild":
				perms[PermLevel.NATION.getIndex()][ActionType.BUILD.getIndex()] = b;
				break;
			case "nationdestroy":
				perms[PermLevel.NATION.getIndex()][ActionType.DESTROY.getIndex()] = b;
				break;
			case "nationswitch":
				perms[PermLevel.NATION.getIndex()][ActionType.SWITCH.getIndex()] = b;
				break;
			case "nationitemuse":
				perms[PermLevel.NATION.getIndex()][ActionType.ITEM_USE.getIndex()] = b;
				break;
			case "allybuild":
				perms[PermLevel.ALLY.getIndex()][ActionType.BUILD.getIndex()] = b;
				break;
			case "allydestroy":
				perms[PermLevel.ALLY.getIndex()][ActionType.DESTROY.getIndex()] = b;
				break;
			case "allyswitch":
				perms[PermLevel.ALLY.getIndex()][ActionType.SWITCH.getIndex()] = b;
				break;
			case "allyitemuse":
				perms[PermLevel.ALLY.getIndex()][ActionType.ITEM_USE.getIndex()] = b;
				break;
			case "pvp":
				pvp = b;
				break;
			case "fire":
				fire = b;
				break;
			case "explosion":
				explosion = b;
				break;
			case "mobs":
				mobs = b;
				break;
		}
	}

	public void load(String s) {

		setAll(false);
		String[] tokens = s.split(",");
		for (String token : tokens)
			set(token, true);
	}

	@Override
	public String toString() {
		
		StringBuilder output = new StringBuilder();
		
		for (PermLevel permLevel : PermLevel.values) {
			String permLevelName = permLevel.name().toLowerCase();
			
			for (ActionType actionType : ActionType.values) {
				if (perms[permLevel.getIndex()][actionType.getIndex()]) {

					if (output.length() != 0) {
						output.append(',');
					}

					output.append(permLevelName).append(actionType.getCommonName());
				}
			}
		}

		if (pvp)
			output.append(output.length() > 0 ? "," : "").append("pvp");
		if (fire)
			output.append(output.length() > 0 ? "," : "").append("fire");
		if (explosion)
			output.append(output.length() > 0 ? "," : "").append("explosion");
		if (mobs)
			output.append(output.length() > 0 ? "," : "").append("mobs");
		
		if (output.length() == 0)
			return "denyAll";
		
		return output.toString();
	}
	
	public boolean getPerm(PermLevel permLevel, ActionType type) {
		return perms[permLevel.getIndex()][type.getIndex()];
	}

	// Legacy Compatibility
	public boolean getResidentPerm(ActionType type) {
		return getPerm(PermLevel.RESIDENT, type);
	}

	public boolean getOutsiderPerm(ActionType type) {
		return getPerm(PermLevel.OUTSIDER, type);
	}

	public boolean getAllyPerm(ActionType type) {
		return getPerm(PermLevel.ALLY, type);
	}

	public boolean getNationPerm(ActionType type) {
		return getPerm(PermLevel.NATION, type);
	}

	public String getColoredPermLevel(ActionType type) {
		return getColoredPermLevel(type, type.getCommonName());
	}
	
	public String getColoredPermLevel(ActionType type, String typeCommonName) {
		StringBuilder output = new StringBuilder(Colors.LightGreen).append(typeCommonName).append(" = ").append(Colors.LightGray);
		
		for (PermLevel permLevel : PermLevel.values) {
			if (perms[permLevel.getIndex()][type.getIndex()]) {
				output.append(permLevel.getShortChar());
			} else {
				output.append('-');
			}
		}
		
		return output.toString();
	}
	
	public String getColourString() {
		return getColoredPermLevel(ActionType.BUILD) + getColoredPermLevel(ActionType.DESTROY, " Destroy");
	}
	public String getColourString2() {
		return getColoredPermLevel(ActionType.SWITCH) + getColoredPermLevel(ActionType.ITEM_USE, " Item");
	}

	public void loadDefault(TownBlockOwner owner) {
		
		for (PermLevel permLevel : PermLevel.values) {
			for (ActionType actionType : ActionType.values) {
				perms[permLevel.getIndex()][actionType.getIndex()] = TownySettings.getDefaultPermission(owner, permLevel, actionType);
			}
		}

		if (owner instanceof Town) {
			pvp = TownySettings.getPermFlag_Town_Default_PVP();
			fire = TownySettings.getPermFlag_Town_Default_FIRE();
			explosion = TownySettings.getPermFlag_Town_Default_Explosion();
			mobs = TownySettings.getPermFlag_Town_Default_Mobs();
		} else {
			pvp = owner.getPermissions().pvp;
			fire = owner.getPermissions().fire;
			explosion = owner.getPermissions().explosion;
			mobs = owner.getPermissions().mobs;
		}

	}
}
