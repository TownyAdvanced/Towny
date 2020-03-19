package com.palmergames.bukkit.towny.utils.loadHandlers;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;

import java.util.ArrayList;
import java.util.List;

public class ResidentLoadHandler implements LoadHandler<List<Resident>> {
	@Override
	public List<Resident> load(String str) {
		String[] resNames = str.split(",");
		TownyMessaging.sendErrorMsg("resi:" + resNames);
		List<Resident> retVal = new ArrayList<>();
		
		for (String resName : resNames) {
			try {
				retVal.add(TownyUniverse.getInstance().getDataSource().getResident(resName));
			} catch (NotRegisteredException ignored) {}
		}
		
		return retVal;
	}
}
