package ca.xshade.bukkit.towny.questioner;

import ca.xshade.bukkit.towny.AlreadyRegisteredException;
import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.TownyException;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Nation;
import ca.xshade.bukkit.util.ChatTools;

public class JoinNationTask extends ResidentNationQuestionTask {
	
	public JoinNationTask(Resident resident, Nation nation) {
		super(resident, nation);
	}

	@Override
	public void run() {
		try {
			nation.addTown(resident.getTown());
			//towny.deleteCache(resident.getName());
			universe.getDataSource().saveResident(resident);
			universe.getDataSource().saveTown(resident.getTown());
			universe.getDataSource().saveNation(nation);
			
			getUniverse().sendNationMessage(nation, ChatTools.color(String.format(TownySettings.getLangString("msg_join_nation"), resident.getTown().getName())));
		} catch (AlreadyRegisteredException e) {
			try {
				getUniverse().sendResidentMessage(resident, e.getError());
			} catch (TownyException e1) {
			}
		} catch (NotRegisteredException e) {
			// TODO somehow this person is not the town mayor
			e.printStackTrace();
		}
	}
}
