package ca.xshade.bukkit.towny.questioner;

import ca.xshade.bukkit.towny.AlreadyRegisteredException;
import ca.xshade.bukkit.towny.TownyException;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.util.ChatTools;

public class JoinTownTask extends ResidentTownQuestionTask {
	
	public JoinTownTask(Resident resident, Town town) {
		super(resident, town);
	}

	@Override
	public void run() {
		try {
			town.addResident(resident);
			towny.deleteCache(resident.getName());
			universe.getDataSource().saveResident(resident);
			universe.getDataSource().saveTown(town);
			
			getUniverse().sendTownMessage(town,  ChatTools.color(String.format(TownySettings.getLangString("msg_join_town"), resident.getName())));
		} catch (AlreadyRegisteredException e) {
			try {
				getUniverse().sendResidentMessage(resident, e.getError());
			} catch (TownyException e1) {
			}
		}
	}
}
