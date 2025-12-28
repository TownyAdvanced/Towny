package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import java.util.Arrays;
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
			return super.toString().toLowerCase(Locale.ROOT);
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
			return super.toString().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * Default permissions with everything off/false.
	 */
	private static final boolean[][] DEFAULT_EMPTY_PERMS;

	/**
	 * Default permissions for residents, everything off except for residents.
	 */
	private static final boolean[][] DEFAULT_RESIDENT_PERMS;

	static {
		DEFAULT_EMPTY_PERMS = new boolean[PermLevel.values.length][ActionType.values.length];
		for (boolean[] permLevel : DEFAULT_EMPTY_PERMS) {
			Arrays.fill(permLevel, false);
		}

		DEFAULT_RESIDENT_PERMS = new boolean[PermLevel.values.length][ActionType.values.length];
		for (int i = 0; i < DEFAULT_RESIDENT_PERMS.length; i++) {
			Arrays.fill(DEFAULT_RESIDENT_PERMS[i], i == PermLevel.RESIDENT.index);
		}
	}
	
	// Towny permissions are split into Action Type and Permission Level
	// So they can inherently be represented by a 2d array
	protected boolean[][] perms = DEFAULT_EMPTY_PERMS;
	
	public boolean pvp, fire, explosion, mobs;

	/**
	 * Ensures that {@link #perms} is a uniquely instantiated array that is safe to write to.
	 */
	private void ensureInitialized() {
		if (this.perms != DEFAULT_EMPTY_PERMS && this.perms != DEFAULT_RESIDENT_PERMS) {
			return;
		}

		final boolean[][] current = this.perms;

		// Fill the perms array
		perms = new boolean[PermLevel.values.length][ActionType.values.length];
		for (int i = 0; i < perms.length; i++) {
			System.arraycopy(current[i], 0, perms[i], 0, ActionType.values.length);
		}
	}

	/**
	 * Attempts to compact the {@link #perms} array by comparing it against the cached shared array instances.
	 */
	private void compactIfPossible() {
		if (Arrays.deepEquals(this.perms, DEFAULT_RESIDENT_PERMS)) {
			this.perms = DEFAULT_RESIDENT_PERMS;
		} else if (Arrays.deepEquals(this.perms, DEFAULT_EMPTY_PERMS)) {
			this.perms = DEFAULT_EMPTY_PERMS;
		}
	}

	public void reset() {
		setAll(false);
	}
	
	public void change(TownyPermissionChange permChange) {
		change(permChange.getChangeAction(), permChange.getChangeValue(), permChange.getArgs());
	}
	
	public void change(TownyPermissionChange.Action permChange, boolean toValue, Object... args) {
		ensureInitialized();

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
		ensureInitialized();
		for (boolean[] permLevel : perms) {
			Arrays.fill(permLevel, b);
		}
	}

	public boolean equalsNonEnvironmental(TownyPermission other) {
		return Arrays.deepEquals(perms, other.perms);
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
		ensureInitialized();
		
		switch (s.toLowerCase(Locale.ROOT)) {
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
			default:
		}
	}

	public void load(String s) {

		setAll(false);
		String[] tokens = s.split(",");
		for (String token : tokens)
			set(token, true);

		compactIfPossible();
	}

	@Override
	public String toString() {
		
		StringBuilder output = new StringBuilder();
		
		for (PermLevel permLevel : PermLevel.values) {
			String permLevelName = permLevel.name().toLowerCase(Locale.ROOT);
			
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
		StringBuilder output = new StringBuilder(Translation.of("status_perm_line_format", typeCommonName));
		
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
		return getColoredPermLevel(ActionType.BUILD) + " " + getColoredPermLevel(ActionType.DESTROY) + " " + getColoredPermLevel(ActionType.SWITCH) + " " + getColoredPermLevel(ActionType.ITEM_USE);
	}

	public void loadDefault(TownBlockOwner owner) {
		ensureInitialized();
		
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

		compactIfPossible();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(perms);
		result = prime * result + Objects.hash(explosion, fire, mobs, pvp);
		return result;
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
		return explosion == other.explosion && fire == other.fire && mobs == other.mobs
				&& Arrays.deepEquals(perms, other.perms) && pvp == other.pvp;
	}
}
