package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.Colors;

//TODO: 12 permission so far. Anything else will expand it to include even more variables. Possibly change the data structure.
public class TownyPermission {

	public boolean residentBuild, residentDestroy, residentSwitch, residentItemUse, 
			outsiderBuild, outsiderDestroy, outsiderSwitch, outsiderItemUse, 
			allyBuild, allyDestroy, allySwitch, allyItemUse, 
			nationBuild, nationDestroy, nationSwitch, nationItemUse;
	public boolean pvp, fire, explosion, mobs;

	public TownyPermission() {

		reset();
	}

	public void reset() {

		setAll(false);
	}

	public void setAll(boolean b) {

		residentBuild = b;
		residentDestroy = b;
		residentSwitch = b;
		residentItemUse = b;
		outsiderBuild = b;
		outsiderDestroy = b;
		outsiderSwitch = b;
		outsiderItemUse = b;
		allyBuild = b;
		allyDestroy = b;
		allySwitch = b;
		allyItemUse = b;
		nationBuild = b;
		nationDestroy = b;
		nationSwitch = b;
		nationItemUse = b;

		pvp = b;
		fire = b;
		explosion = b;
		mobs = b;
	}

	public void set(String s, boolean b) {

		if (s.equalsIgnoreCase("denyAll"))
			reset();
		else if (s.equalsIgnoreCase("residentBuild"))
			residentBuild = b;
		else if (s.equalsIgnoreCase("residentDestroy"))
			residentDestroy = b;
		else if (s.equalsIgnoreCase("residentSwitch"))
			residentSwitch = b;
		else if (s.equalsIgnoreCase("residentItemUse"))
			residentItemUse = b;
		else if (s.equalsIgnoreCase("outsiderBuild"))
			outsiderBuild = b;
		else if (s.equalsIgnoreCase("outsiderDestroy"))
			outsiderDestroy = b;
		else if (s.equalsIgnoreCase("outsiderSwitch"))
			outsiderSwitch = b;
		else if (s.equalsIgnoreCase("outsiderItemUse"))
			outsiderItemUse = b;
		else if (s.equalsIgnoreCase("allyBuild"))
			allyBuild = b;
		else if (s.equalsIgnoreCase("allyDestroy"))
			allyDestroy = b;
		else if (s.equalsIgnoreCase("allySwitch"))
			allySwitch = b;
		else if (s.equalsIgnoreCase("allyItemUse"))
			allyItemUse = b;
		else if (s.equalsIgnoreCase("nationBuild"))
			nationBuild = b;
		else if (s.equalsIgnoreCase("nationDestroy"))
			nationDestroy = b;
		else if (s.equalsIgnoreCase("nationSwitch"))
			nationSwitch = b;
		else if (s.equalsIgnoreCase("nationItemUse"))
			nationItemUse = b;
		else if (s.equalsIgnoreCase("pvp"))
			pvp = b;
		else if (s.equalsIgnoreCase("fire"))
			fire = b;
		else if (s.equalsIgnoreCase("explosion"))
			explosion = b;
		else if (s.equalsIgnoreCase("mobs"))
			mobs = b;
	}

	public void load(String s) {

		setAll(false);
		String[] tokens = s.split(",");
		for (String token : tokens)
			set(token, true);
	}

	@Override
	public String toString() {

		String out = "";
		if (residentBuild)
			out += "residentBuild";
		if (residentDestroy)
			out += (out.length() > 0 ? "," : "") + "residentDestroy";
		if (residentSwitch)
			out += (out.length() > 0 ? "," : "") + "residentSwitch";
		if (residentItemUse)
			out += (out.length() > 0 ? "," : "") + "residentItemUse";
		if (outsiderBuild)
			out += (out.length() > 0 ? "," : "") + "outsiderBuild";
		if (outsiderDestroy)
			out += (out.length() > 0 ? "," : "") + "outsiderDestroy";
		if (outsiderSwitch)
			out += (out.length() > 0 ? "," : "") + "outsiderSwitch";
		if (outsiderItemUse)
			out += (out.length() > 0 ? "," : "") + "outsiderItemUse";
		if (allyBuild)
			out += (out.length() > 0 ? "," : "") + "allyBuild";
		if (allyDestroy)
			out += (out.length() > 0 ? "," : "") + "allyDestroy";
		if (allySwitch)
			out += (out.length() > 0 ? "," : "") + "allySwitch";
		if (allyItemUse)
			out += (out.length() > 0 ? "," : "") + "allyItemUse";
		if (nationBuild)
			out += (out.length() > 0 ? "," : "") + "nationBuild";
		if (nationDestroy)
			out += (out.length() > 0 ? "," : "") + "nationDestroy";
		if (nationSwitch)
			out += (out.length() > 0 ? "," : "") + "nationSwitch";
		if (nationItemUse)
			out += (out.length() > 0 ? "," : "") + "nationItemUse";
		if (pvp)
			out += (out.length() > 0 ? "," : "") + "pvp";
		if (fire)
			out += (out.length() > 0 ? "," : "") + "fire";
		if (explosion)
			out += (out.length() > 0 ? "," : "") + "explosion";
		if (mobs)
			out += (out.length() > 0 ? "," : "") + "mobs";
		if (out.length() == 0)
			out += "denyAll"; // Make the token not empty
		return out;
	}

	public enum ActionType {
		BUILD, DESTROY, SWITCH, ITEM_USE;

		@Override
		public String toString() {

			return super.toString().toLowerCase();
		}
	}

	public enum PermLevel {
		RESIDENT, NATION, ALLY, OUTSIDER;

		@Override
		public String toString() {

			return super.toString().toLowerCase();
		}
	}

	public boolean getResidentPerm(ActionType type) {

		switch (type) {
		case BUILD:
			return residentBuild;
		case DESTROY:
			return residentDestroy;
		case SWITCH:
			return residentSwitch;
		case ITEM_USE:
			return residentItemUse;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public boolean getOutsiderPerm(ActionType type) {

		switch (type) {
		case BUILD:
			return outsiderBuild;
		case DESTROY:
			return outsiderDestroy;
		case SWITCH:
			return outsiderSwitch;
		case ITEM_USE:
			return outsiderItemUse;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public boolean getAllyPerm(ActionType type) {

		switch (type) {
		case BUILD:
			return allyBuild;
		case DESTROY:
			return allyDestroy;
		case SWITCH:
			return allySwitch;
		case ITEM_USE:
			return allyItemUse;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public boolean getNationPerm(ActionType type) {

		switch (type) {
		case BUILD:
			return nationBuild;
		case DESTROY:
			return nationDestroy;
		case SWITCH:
			return nationSwitch;
		case ITEM_USE:
			return nationItemUse;
		default:
			throw new UnsupportedOperationException();
		}
	}
	public String getColourString() {

		return Colors.LightGreen + "Build = " + Colors.LightGray + (residentBuild ? "f" : "-") + (nationBuild ? "n" : "-") + (allyBuild ? "a" : "-") + (outsiderBuild ? "o" : "-") + 
				Colors.LightGreen + " Destroy = " + Colors.LightGray + (residentDestroy ? "f" : "-") + (nationDestroy ? "n" : "-") + (allyDestroy ? "a" : "-") + (outsiderDestroy ? "o" : "-"); 
	}
	public String getColourString2() {
		return Colors.LightGreen + "Switch = " + Colors.LightGray + (residentSwitch ? "f" : "-") + (nationSwitch ? "n" : "-") + (allySwitch ? "a" : "-") + (outsiderSwitch ? "o" : "-") + 
				Colors.LightGreen + " Item = " + Colors.LightGray + (residentItemUse ? "f" : "-") + (nationItemUse ? "n" : "-") + (allyItemUse ? "a" : "-") + (outsiderItemUse ? "o" : "-");
	}

	public void loadDefault(TownBlockOwner owner) {

		residentBuild = TownySettings.getDefaultPermission(owner, PermLevel.RESIDENT, ActionType.BUILD);
		residentDestroy = TownySettings.getDefaultPermission(owner, PermLevel.RESIDENT, ActionType.DESTROY);
		residentSwitch = TownySettings.getDefaultPermission(owner, PermLevel.RESIDENT, ActionType.SWITCH);
		residentItemUse = TownySettings.getDefaultPermission(owner, PermLevel.RESIDENT, ActionType.ITEM_USE);
		nationBuild = TownySettings.getDefaultPermission(owner, PermLevel.NATION, ActionType.BUILD);
		nationDestroy = TownySettings.getDefaultPermission(owner, PermLevel.NATION, ActionType.DESTROY);
		nationSwitch = TownySettings.getDefaultPermission(owner, PermLevel.NATION, ActionType.SWITCH);
		nationItemUse = TownySettings.getDefaultPermission(owner, PermLevel.NATION, ActionType.ITEM_USE);
		allyBuild = TownySettings.getDefaultPermission(owner, PermLevel.ALLY, ActionType.BUILD);
		allyDestroy = TownySettings.getDefaultPermission(owner, PermLevel.ALLY, ActionType.DESTROY);
		allySwitch = TownySettings.getDefaultPermission(owner, PermLevel.ALLY, ActionType.SWITCH);
		allyItemUse = TownySettings.getDefaultPermission(owner, PermLevel.ALLY, ActionType.ITEM_USE);
		outsiderBuild = TownySettings.getDefaultPermission(owner, PermLevel.OUTSIDER, ActionType.BUILD);
		outsiderDestroy = TownySettings.getDefaultPermission(owner, PermLevel.OUTSIDER, ActionType.DESTROY);
		outsiderItemUse = TownySettings.getDefaultPermission(owner, PermLevel.OUTSIDER, ActionType.ITEM_USE);
		outsiderSwitch = TownySettings.getDefaultPermission(owner, PermLevel.OUTSIDER, ActionType.SWITCH);
		/*
		 * pvp = owner.getPermissions().pvp;
		 * fire = owner.getPermissions().fire;
		 * explosion = owner.getPermissions().explosion;
		 * mobs = owner.getPermissions().mobs;
		 */

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
