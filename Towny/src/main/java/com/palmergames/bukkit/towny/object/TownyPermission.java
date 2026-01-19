package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import java.util.Locale;
import java.util.Objects;

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
		private final int mask;

		ActionType(int index, String commonName) {
			this.index = index;
			this.commonName = commonName;

			int mask = 0;
			for (int i = 0; i < 4; i++) {
				mask |= 1 << (i * 4 + index);
			}
			this.mask = mask;
		}
		
		public int getIndex() {
			return index;
		}
		
		public String getCommonName() {
			return commonName;
		}
		
		@Override
		public String toString() {
			return super.toString().toLowerCase(Locale.ROOT);
		}
	}

	public enum PermLevel {
		RESIDENT (0, 'f'), NATION (1, 'n'), ALLY (2, 'a'), OUTSIDER (3, 'o');
		
		private static final PermLevel[] values = PermLevel.values();
		
		private final int index;
		private final char shortVal;
		private final int mask;
		
		PermLevel(int index, char shortVal) {
			this.index = index;
			this.shortVal = shortVal;
			this.mask = 0b1111 << (index * 4);
		}
		
		public int getIndex() {
			return index;
		}
		
		public char getShortChar() {
			return shortVal;
		}

		@Override
		public String toString() {
			return super.toString().toLowerCase(Locale.ROOT);
		}
	}
	
	// Towny permissions are split into Action Type and Permission Level
	// So they can inherently be represented by a 2d array (or a short, for as long as there are only 4 of both)
	protected short perms;
	
	public boolean pvp, fire, explosion, mobs;

	public void reset() {
		setAll(false);
	}
	
	public void change(TownyPermissionChange permChange) {
		change(permChange.getChangeAction(), permChange.getChangeValue(), permChange.getArgs());
	}
	
	public void change(TownyPermissionChange.Action permChange, boolean toValue, Object... args) {
		// Sorted by most common to least common
		if (permChange == TownyPermissionChange.Action.SINGLE_PERM && args.length == 2) {
			set((PermLevel) args[0], (ActionType) args[1], toValue);
		}
		else if (permChange == TownyPermissionChange.Action.PERM_LEVEL && args.length == 1) {
			setPermLevel((PermLevel) args[0], toValue);
		}
		else if (permChange == TownyPermissionChange.Action.ACTION_TYPE && args.length == 1) {
			setActionType((ActionType) args[0], toValue);
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
		perms = (short) (b ? -1 : 0);
	}

	public boolean equalsNonEnvironmental(TownyPermission other) {
		return perms == other.perms;
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
		switch (s.toLowerCase(Locale.ROOT)) {
			case "denyall":
				reset();
				break;
			case "residentbuild":
				set(PermLevel.RESIDENT, ActionType.BUILD, b);
				break;
			case "residentdestroy":
				set(PermLevel.RESIDENT, ActionType.DESTROY, b);
				break;
			case "residentswitch":
				set(PermLevel.RESIDENT, ActionType.SWITCH, b);
				break;
			case "residentitemuse":
				set(PermLevel.RESIDENT, ActionType.ITEM_USE, b);
				break;
			case "outsiderbuild":
				set(PermLevel.OUTSIDER, ActionType.BUILD, b);
				break;
			case "outsiderdestroy":
				set(PermLevel.OUTSIDER, ActionType.DESTROY, b);
				break;
			case "outsiderswitch":
				set(PermLevel.OUTSIDER, ActionType.SWITCH, b);
				break;
			case "outsideritemuse":
				set(PermLevel.OUTSIDER, ActionType.ITEM_USE, b);
				break;
			case "nationbuild":
				set(PermLevel.NATION, ActionType.BUILD, b);
				break;
			case "nationdestroy":
				set(PermLevel.NATION, ActionType.DESTROY, b);
				break;
			case "nationswitch":
				set(PermLevel.NATION, ActionType.SWITCH, b);
				break;
			case "nationitemuse":
				set(PermLevel.NATION, ActionType.ITEM_USE, b);
				break;
			case "allybuild":
				set(PermLevel.ALLY, ActionType.BUILD, b);
				break;
			case "allydestroy":
				set(PermLevel.ALLY, ActionType.DESTROY, b);
				break;
			case "allyswitch":
				set(PermLevel.ALLY, ActionType.SWITCH, b);
				break;
			case "allyitemuse":
				set(PermLevel.ALLY, ActionType.ITEM_USE, b);
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
			default:
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
			String permLevelName = permLevel.name().toLowerCase(Locale.ROOT);
			
			for (ActionType actionType : ActionType.values) {
				if (getPerm(permLevel, actionType)) {

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
		int index = (permLevel.index * 4 + type.index);
		return (perms & (1 << index)) != 0;
	}

	public void set(PermLevel permLevel, ActionType type, boolean value) {
		int index = (permLevel.index * 4 + type.index);

		if (value) {
			perms |= (short) (1 << index);
		} else {
			perms &= (short) ~(1 << index);
		}
	}

	public void setPermLevel(PermLevel permLevel, boolean value) {
		if (value) {
			perms |= (short) permLevel.mask;
		} else {
			perms &= (short) ~permLevel.mask;
		}
	}

	public void setActionType(ActionType type, boolean value) {
		if (value) {
			perms |= (short) type.mask;
		} else {
			perms &= (short) ~type.mask;
		}
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
		StringBuilder output = new StringBuilder(Translation.of("status_perm_line_format", typeCommonName));
		
		for (PermLevel permLevel : PermLevel.values) {
			if (getPerm(permLevel, type)) {
				output.append(permLevel.getShortChar());
			} else {
				output.append('-');
			}
		}
		
		return output.toString();
	}
	
	public String getColourString() {
		return getColoredPermLevel(ActionType.BUILD) + " " + getColoredPermLevel(ActionType.DESTROY) + " " + getColoredPermLevel(ActionType.SWITCH) + " " + getColoredPermLevel(ActionType.ITEM_USE);
	}

	public void loadDefault(TownBlockOwner owner) {
		
		for (PermLevel permLevel : PermLevel.values) {
			for (ActionType actionType : ActionType.values) {
				set(permLevel, actionType, TownySettings.getDefaultPermission(owner, permLevel, actionType));
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

	@Override
	public int hashCode() {
		return Objects.hash(perms, explosion, fire, mobs, pvp);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TownyPermission other = (TownyPermission) obj;
		return explosion == other.explosion && fire == other.fire && mobs == other.mobs && perms == other.perms && pvp == other.pvp;
	}
}
