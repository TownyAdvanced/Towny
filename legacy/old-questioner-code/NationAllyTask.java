package com.palmergames.bukkit.towny.questioner;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class NationAllyTask extends AllyQuestionTask {

	public NationAllyTask(Resident resident, Nation nation) {
		
		super(resident, nation);
	}

	@Override
	public void run() {
		
		try {
			requestingNation.addAlly(targetNation);
			targetNation.addAlly(requestingNation);
		} catch (AlreadyRegisteredException e) {
			e.printStackTrace();
		}
		
		TownyMessaging.sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_added_ally"), requestingNation.getName()));
		TownyMessaging.sendNationMessage(requestingNation, String.format(TownySettings.getLangString("msg_accept_ally"), targetNation.getName()));
		
		TownyUniverse.getDataSource().saveNations();
		
		Towny.plugin.resetCache();
	}
}