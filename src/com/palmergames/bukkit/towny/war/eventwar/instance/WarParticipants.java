package com.palmergames.bukkit.towny.war.eventwar.instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.eventwar.WarDataBase;
import com.palmergames.bukkit.towny.war.eventwar.WarMetaDataController;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;

public class WarParticipants {
	private War war;
	private List<Town> warringTowns = new ArrayList<>();
	private List<Nation> warringNations = new ArrayList<>();
	private List<Resident> warringResidents = new ArrayList<>();
	private List<Resident> potentialResidents = new ArrayList<>();
	private List<TownBlock> potentialTownBlocks = new ArrayList<>();
	private List<Player> onlineWarriors = new ArrayList<>();
	private List<TownyObject> govSide = new ArrayList<>();
	private List<TownyObject> rebSide = new ArrayList<>();
	private Hashtable<Resident, Integer> residentLives = new Hashtable<>();
	private List<UUID> uuidsToIgnore = new ArrayList<>();
	private int totalResidentsAtStart = 0;
	private int totalTownsAtStart = 0;
	private int totalNationsAtStart = 0;
	
	public WarParticipants(War war) {
		this.war = war;
	}
	
	public List<Nation> getNations() {
		return warringNations;
	}
	
	public List<Town> getTowns() {
		return warringTowns;
	}
	
	public List<Resident> getResidents() {
		return warringResidents;
	}
	
	public List<Player> getOnlineWarriors() {
		return onlineWarriors;
	}
	
	public enum WarSide {
		GOVERNMENT,
		ANTIGOVERNMENT;
		// Maybe we do some neutral parties here later on I don't know.
	}
	
	public List<TownyObject> getGovSide() {
		return Collections.unmodifiableList(govSide);
	}

	public List<TownyObject> getRebSide() {
		return Collections.unmodifiableList(rebSide);
	}
	
	public void addGovSide(TownyObject obj) {
		if (!govSide.contains(obj))
			govSide.add(obj);
	}
	
	public void addRebSide(TownyObject obj) {
		if (!rebSide.contains(obj))
			rebSide.add(obj);
	}

	public int getNationsAtStart() {
		return totalNationsAtStart;
	}
	
	public int getTownsAtStart() {
		return totalTownsAtStart;
	}
	
	public int getResidentsAtStart() {
		return totalResidentsAtStart;
	}
	
	private void setNationsAtStart(int n) {
		totalNationsAtStart = n;
	}
	
	private void setTownsAtStart(int n) {
		totalTownsAtStart = n;
	}
	
	private void setResidentsAtStart(int n) {
		totalResidentsAtStart = n;
	}
	
	public List<UUID> getUUIDsToIgnore() {
		return uuidsToIgnore;
	}

	public void setIgnoredUUIDs(List<UUID> uuidsToIgnore) {
		this.uuidsToIgnore = uuidsToIgnore;
	}

	public boolean has(Nation nation) {
		return warringNations.contains(nation);
	}

	public boolean has(Town town) {
		return warringTowns.contains(town);
	}
	
	public boolean has(Resident resident) {
		return warringResidents.contains(resident);
	}
	
	/**
	 * Add a nation to war, and all the towns within it.
	 * @param nation {@link Nation} to incorporate into War.
	 * @return false if conditions are not met.
	 */
	boolean add(Nation nation) {
		
		/*
		 * Do a bunch of early tests which can disqualify a nation from a war.
		 */
		if (!isNationAllowedToWar(nation))
			return false;

		int numTowns = 0;
		for (Town town : nation.getTowns()) {
			if (add(town)) {
				warringTowns.add(town);
				numTowns++;
			}
		}
		// The nation capital must be one of the valid towns for a nation to go to war.
		if (numTowns > 0 && warringTowns.contains(nation.getCapital())) {
			warringNations.add(nation);
			return true;
		} else {
			war.addErrorMsg("The nation " + nation + " did not have any towns which could go to war, removing " + nation + " from the war.");
			for (Town town : nation.getTowns()) {
				if (warringTowns.contains(town)) {
					warringTowns.remove(town);
					war.getScoreManager().getScores().remove(town);
				}
			}
			return false;
		}
	}
	
	/**
	 * Add a town to war. Set the townblocks in the town to the correct health. Add
	 * the residents to the war, give them their lives.
	 * 
	 * @param town {@link Town} to incorporate into war
	 * @return false if conditions are not met.
	 */
	boolean add(Town town) {

		/*
		 * Do a bunch of early tests which can disqualify a town from a war.
		 */
		if (!isTownAllowedToWar(town))
			return false;

		/*
		 * Add town's townblocks to the warzone. Even if TownBlock HP is not
		 * a factor we still need the plots to count towards the warzone.
		 */
		int numTownBlocks = 0;
		for (TownBlock tb : town.getTownBlocks()) {
			if (!tb.getWorld().isWarAllowed())
				continue;
			if (!potentialTownBlocks.contains(tb))
				continue;
			numTownBlocks++;
			war.getWarZoneManager().addWarZone(tb.getWorldCoord(), tb.isHomeBlock() ? TownySettings.getWarzoneHomeBlockHealth() : TownySettings.getWarzoneTownBlockHealth());
			WarMetaDataController.setWarUUID(tb, war.getWarUUID());
		}
		if (numTownBlocks == 0) {
			war.addErrorMsg("The town " + town + " does not have any land to fight over. They will not take part in the war.");
			return false;
		}

		// Put the war UUID onto the town metadata.
		WarMetaDataController.setWarUUID(town, war.getWarUUID());
		
		// Load in any previous score for the town if this isn't a RIOT war.
		if (!war.getWarType().equals(WarType.RIOT))
			war.getScoreManager().addTown(town);
		
		/*
		 * Give the players their lives, metadata.
		 */
		for (Resident resident : town.getResidents()) {
			// The resident was already removed from the war previously, or the resident
			// is not considered a potential resident (probably lost all their lives.)
			if (uuidsToIgnore.contains(resident.getUUID()) || !potentialResidents.contains(resident))
				continue;

			add(resident);
		}
			
		return true;
	}

	/**
	 * Adds a resident to the war, gives them their metadata and lives.
	 * 
	 * @param resident Resident to add to the war.
	 */
	public void add(Resident resident) {
		int lives = resident.isMayor() ? war.getWarType().mayorLives : war.getWarType().residentLives;
		// This is a resident being re-loaded into this same war, use their already
		// existing lives.
		if (WarMetaDataController.hasResidentLives(resident))
			lives = WarMetaDataController.getResidentLives(resident);

		// Load in any previous score for the resident if this is a RIOT war.
		if (war.getWarType().equals(WarType.RIOT))
			war.getScoreManager().addResident(resident);

		residentLives.put(resident, lives);
		WarMetaDataController.setResidentLives(resident, lives);
		WarMetaDataController.setWarUUID(resident, war.getWarUUID());
		if (resident.isOnline())
			addOnlineWarrior(resident.getPlayer());
		warringResidents.add(resident);
	}

	public void addOnlineWarrior(Player player) {
		if (onlineWarriors.contains(player))
			return;
		onlineWarriors.add(player);
		// Tell the player about the war hud.
		TownyMessaging.sendMsg(player, Translatable.of("msg_war_activate_war_hud_tip"));
	}

	public void removeOnlineWarrior(Player player) {
		onlineWarriors.remove(player);
	}

	/**
	 * Method for gathering the nations, towns and residents which will join a war.
	 * 
	 * @param nations - List&lt;Nation&gt; which will be tested and added to war.
	 * @param towns - List&lt;Town&gt; which will be tested and added to war.
	 * @param residents - List&lt;Resident&gt; which will be tested and added to war.
	 */
	public void gatherParticipantsForWar(List<Nation> nations, List<Town> towns, List<Resident> residents, List<TownBlock> townblocks) {
		
		potentialResidents = residents;
		if (townblocks != null)
			potentialTownBlocks = townblocks;
		/*
		 * Takes the given lists and add them to War lists, if they meet the requires
		 * set out in add(Town) and add(Nation), based on the WarType.
		 */
		switch (war.getWarType()) {
		case WORLDWAR:
		case NATIONWAR:
		case CIVILWAR:
			for (Nation nation : nations) {
				if (!nation.isNeutral() && add(nation)) {
					TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_war_join_nation", nation.getName()));
				} else if (!TownySettings.isDeclaringNeutral()) {
					nation.setNeutral(false);
					nation.save();
					if (add(nation)) {
						TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_war_join_forced", nation.getName()));
					}
				}
			}
			break;
		case TOWNWAR:
		case RIOT:
			for (Town town : towns) {
				if (!town.isNeutral() && add(town))
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_war_join", town));
					warringTowns.add(town);
			}
			break;
		}
		
		if (war.getWarType().equals(WarType.RIOT) || war.getWarType().equals(WarType.CIVILWAR))
			selectTeams();
	}
	
	private void selectTeams() {
		// Attempt to settle sides for civil war and riot.
		switch (war.getWarType()) {
		case CIVILWAR:
			for (Town town : warringTowns) {
				String sideString = WarMetaDataController.getWarSide(town);
				// Not a new war.
				if (sideString != null) {
					WarSide side = WarSide.valueOf(sideString);
					switch (side) {
					case GOVERNMENT:
						addGovSide(town);
						break;
					case ANTIGOVERNMENT:
						addRebSide(town);
						break;
					}
					// A new war where sides haven't been chosen.
				} else {
					// Capital doesn't get the option to rebel.
					if (town.isCapital()) {
						addGovSide(town);
						continue;
					}
					// Send a confirmation to the mayor of the town, if they aren't online
					// they will autojoin the govt side during the verification stage.
					// TODO: some sort of late-to-start option for choosing sides.
					if (town.getMayor().isOnline()) {
						WarUtil.confirmPlayerSide(war, town.getMayor().getPlayer());
					}
				}
			}
			break;
		case RIOT:
			for (Resident res : warringResidents) {
				String sideString = WarMetaDataController.getWarSide(res);
				// Not a new war.
				if (sideString != null) {
					WarSide side = WarSide.valueOf(sideString);
					switch (side) {
					case GOVERNMENT:
						addGovSide(res);
						break;
					case ANTIGOVERNMENT:
						addRebSide(res);
					}
					// A new war where sides haven't been chosen.
				} else {
					// Mayor doesn't get the option to rebel.
					if (res.isMayor()) {
						addGovSide(res);
						continue;
					}
					// Send a confirmation to the resident. If they aren't
					// online they will autojoin the govt side during the verification stage.
					// TODO: some sort of late-to-start option for choosing sides.
					if (res.isOnline()) {
						WarUtil.confirmPlayerSide(war, res.getPlayer());
					}
				}
			}
			break;
		case NATIONWAR:
		case WORLDWAR:
		case TOWNWAR:
		default:
			break;
		}
	}

	/**
	 * Verifies that for the WarType there are enough residents/towns/nations
	 * involved to have at least 2 sides.
	 * 
	 * @param firstRun true when this is the beginning of a new war.
	 * @return
	 */
	public boolean verifyTwoEnemies(boolean firstRun) {
		switch (war.getWarType()) {
		case WORLDWAR:
		case NATIONWAR:
			// Cannot have a war with less than 2 nations.
			if (warringNations.size() < 2) {
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_war_not_enough_nations"));
				return false;
			}
			
			// Lets make sure that at least 2 nations consider each other enemies.
			boolean enemy = false;
			for (Nation nation : warringNations) {
				for (Nation nation2 : warringNations) {
					if (nation.hasEnemy(nation2) && nation2.hasEnemy(nation)) {
						enemy = true;
						break;
					}
				}			
			}
			if (!enemy) {
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_war_no_enemies_for_war"));
				return false;
			}
			break;
		case CIVILWAR:
			if (warringNations.size() > 1) {
				war.addErrorMsg("Too many nations for a civil war!");
				return false;
			}
			
			if (warringTowns.size() == 1) {
				war.addErrorMsg("Not enough towns for a civil war!");
				return false;
			}

			// Re-evaluate sides again, because some people may have missed their chance to
			// rebel, and will be placed on the government side.
			for (Town town : warringTowns)
				if (!rebSide.contains(town)) {
					addGovSide(town);
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_you_have_sided_with_the_government"));
				} else {
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_you_have_sided_with_the_rebels"));
				}

			if (rebSide.isEmpty()) {
				war.addErrorMsg("No rebels found for a civil war.");
				return false;
			}
			
			if (firstRun) {
				for (TownyObject town : govSide) {
					WarMetaDataController.setWarSide(town, "GOVERNMENT");
					for (TownyObject reb : rebSide)
						((Town) town).addEnemy(((Town) reb));
				}
				for (TownyObject town : rebSide) {
					WarMetaDataController.setWarSide(town, "ANTIGOVERNMENT");
					for (TownyObject gov : govSide)
						((Town) town).addEnemy(((Town) gov));
				}
			}
			
			break;
		case TOWNWAR:
			if (warringTowns.size() < 2) {
				war.addErrorMsg("Not enough Towns for town vs town war!");
				return false;
			}
			warringTowns.get(0).addEnemy(warringTowns.get(1));
			warringTowns.get(1).addEnemy(warringTowns.get(0));
			
			break;
		case RIOT:
			if (warringTowns.size() > 1) {
				war.addErrorMsg("Too many towns gathered for a riot war!");
				return false;
			}
			Town town = warringTowns.get(0);
			if (town.getResidents().size() == 1) {
				war.addErrorMsg("Not enough residents for a riot war!");
				return false;
			}

			// Re-evaluate sides again, because some people may have missed their chance to
			// rebel, and will be placed on the government side.
			for (Resident res : town.getResidents())
				if (!rebSide.contains(res)) {
					addGovSide(res);
					TownyMessaging.sendMsg(res, Translatable.of("msg_you_have_sided_with_the_government"));
				} else {
					TownyMessaging.sendMsg(res, Translatable.of("msg_you_have_sided_with_the_rebels"));
				}
			
			if (rebSide.isEmpty()) {
				war.addErrorMsg("No rioters gathered.");
				return false;
			}
			if (firstRun) {
				for (TownyObject res : govSide)
					WarMetaDataController.setWarSide(res, "GOVERNMENT");
	
				for (TownyObject res : rebSide)
					WarMetaDataController.setWarSide(res, "ANTIGOVERNMENT");
			}
			break;
		}
		
		for (Town town : warringTowns) {
			town.setActiveWar(true);
			town.save();
		}
		
		for (Nation nation : warringNations) {
			nation.setActiveWar(true);
			nation.save();
		}

		if (firstRun) {
			setNationsAtStart(warringNations.size());
			setTownsAtStart(warringTowns.size());
			setResidentsAtStart(warringResidents.size());
		}
		return true;
	}

	/**
	 * Used at war start and in the /towny war participants command.
	 * 
	 * @param warType WarType of the war.
	 * @param name The formal name of the war.  
	 */
	public void outputParticipants(WarType warType, String name) {
		List<String> warParticipants = new ArrayList<>();
		
		switch (warType) {
		case WORLDWAR:
		case NATIONWAR:
		case CIVILWAR:
			Translation.of("msg_war_participants_header");
			for (Nation nation : warringNations) {
				int towns = 0;
				for (Town town : nation.getTowns())
					if (warringTowns.contains(town))
						towns++;
				warParticipants.add(Translation.of("msg_war_participants", nation.getName(), towns));
			}
			break;
		case TOWNWAR:
			warParticipants.add(Colors.translateColorCodes("&6[War] &eTown Name &f(&bResidents&f)"));
			for (Town town : warringTowns)
				warParticipants.add(Translation.of("msg_war_participants", town.getName(), town.getResidents().size()));
			break;
		case RIOT:
			warParticipants.add(Colors.translateColorCodes("&6[War] &eResident Name &f(&bLives&f) "));
			for (Resident resident : warringResidents)
				warParticipants.add(Translation.of("msg_war_participants", resident.getName(), getLives(resident)));
			break;
		}
		war.getMessenger().sendPlainGlobalMessage(ChatTools.formatTitle(name + " Participants"));
		war.getMessenger().sendPlainGlobalMessage(warParticipants);
		war.getMessenger().sendPlainGlobalMessage(ChatTools.formatTitle("----------------"));
	}
	
	/**
	 * Removes a Nation from the war.
	 * Called when a Nation voluntarily leaves a war.
	 * Called by remove(Town town). 
	 * @param nation Nation being removed from the war.
	 */
	void remove(Nation nation) {

		// remove Nation
		warringNations.remove(nation);
		
		// Send title message to nation and elimination message globally.
		TownyMessaging.sendTitleMessageToNation(nation, Translation.of("msg_war_nation_removed_from_war_titlemsg"), "");
		sendEliminateMessage(nation.getFormattedName());
		
		// Cleanup in case they were not already removed from the war.
		for (Town town : nation.getTowns())
			if (warringTowns.contains(town))
				remove(town);
		
		nation.setActiveWar(false);
		uuidsToIgnore.add(nation.getUUID());
		// Give the town a lastWarEndTime metadata
		WarMetaDataController.setLastWarTime(nation, System.currentTimeMillis());
	}

	/**
	 * Removes a Town from the war.
	 * Called when a player is killed and their Town Bank cannot pay the war penalty.
	 * Called when a Town voluntarily leaves a War.
	 * Called by remove(Nation nation).
	 * 
	 * @param town The Town being removed from the war.
	 */
	void remove(Town town) {

		// remove Town
		warringTowns.remove(town);

		// remove Residents still in the war.
		for (Resident resident : town.getResidents()) {
			if (warringResidents.contains(resident))
				remove(resident);
		}
		
		// Remove TownBlocks still in the war & count them for the eliminated message.
		int fallenTownBlocks = 0;
		for (TownBlock townBlock : town.getTownBlocks()) {
			if (war.getWarZoneManager().isWarZone(townBlock.getWorldCoord())) {
				fallenTownBlocks++;
				war.getWarZoneManager().remove(townBlock.getWorldCoord());
			}
			WarDataBase.cleanTownBlockMetaData(townBlock);
		}

		if (war.getWarType().equals(WarType.CIVILWAR)) {
			if (govSide.contains(town))
				govSide.remove(town);
			if (rebSide.contains(town))
				rebSide.remove(town);
		}
		
		for (Town enemy : town.getEnemies()) {
			town.removeEnemy(enemy);
			enemy.removeEnemy(town);
			enemy.save();
		}
		
		for (Town ally : town.getAllies()) {
			town.removeAlly(town);
			ally.removeAlly(town);
			ally.save();
		}
		town.save();

		// Disable activewar flag on Town so they can take part in another war.
		town.setActiveWar(false);
		uuidsToIgnore.add(town.getUUID());
		WarDataBase.cleanTownMetaData(town);
		// Give the town a lastWarEndTime metadata
		WarMetaDataController.setLastWarTime(town, System.currentTimeMillis());

		// Send title message to town and elimination message globally.
		TownyMessaging.sendTitleMessageToTown(town, Translation.of("msg_war_town_removed_from_war_titlemsg"), "");
		sendEliminateMessage(town.getFormattedName() + " (" + fallenTownBlocks + Translation.of("msg_war_append_townblocks_fallen"));
	}
	
	/**
	 * Removes a resident from the war.
	 * 
	 * Called by takeLife(Resident resident)
	 * Called by remove(Town town)
	 * @param resident
	 */
	public void remove(Resident resident) {
		warringResidents.remove(resident);
		uuidsToIgnore.add(resident.getUUID());
		WarDataBase.cleanResidentMetaData(resident);
		if (war.getWarType().equals(WarType.RIOT)) {
			if (govSide.contains(resident))
				govSide.remove(resident);
			if (rebSide.contains(resident))
				rebSide.remove(resident);
		}
	}

	public int getLives(Resident resident) {
		return residentLives.get(resident);
	}
	
	/**
	 * Takes a life from the resident, removes them from the war if they have none remaining.
	 * 
	 * @param resident Resident losing a life.
	 */
	public void takeLife(Resident resident) {
		residentLives.put(resident, residentLives.get(resident) - 1);
		WarMetaDataController.decrementResidentLives(resident);
	}

	private void sendEliminateMessage(String name) {
		war.getMessenger().sendGlobalMessage(Translatable.of("msg_war_eliminated", name));
	}

	/*
	 * Voluntary leaving section. (UNUSED AS OF YET)
	 * 
	 * TODO: set up some leave commands because these are unused!
	 */

	@Deprecated
	public void nationLeave(Nation nation) {

		remove(nation);
		war.getMessenger().sendGlobalMessage(Translatable.of("MSG_WAR_FORFEITED", nation));
		war.checkEnd();
	}

	@Deprecated
	public void townLeave(Town town) {

		remove(town);
		war.getMessenger().sendGlobalMessage(Translatable.of("MSG_WAR_FORFEITED", town));
		war.checkEnd();
	}

	private boolean isNationAllowedToWar(Nation nation) {
		/*
		 * With the instanced war system, Nations can only have one on-going war.
		 */
		if (nation.hasActiveWar()) {
			war.addErrorMsg("The nation " + nation + " is already involved in a war. They will not take part in the war.");
			return false;
		}

		if (!WarUtil.eligibleForWar(war.getWarType(), nation)) {
			war.addErrorMsg("The nation " + nation + " fought too recently. They will not take part in the war.");
			return false;
		}

		if (uuidsToIgnore.contains(nation.getUUID())) {
			war.addErrorMsg("The nation " + nation + " was already removed from this war. They will remain removed.");
			return false;
		}

		if (nation.getEnemies().size() < 1)
			return false;

		int enemies = 0;
		for (Nation enemy : nation.getEnemies()) {
			if (enemy.hasEnemy(nation))
				enemies++;
		}
		if (enemies < 1)
			return false;

		return true;
	}

	private boolean isTownAllowedToWar(Town town) {
		/*
		 * With the instanced war system, Towns can only have one on-going war.
		 */
		if (town.hasActiveWar()) {
			war.addErrorMsg("The town " + town + " is already involved in a war. They will not take part in the war.");
			return false;
		}

		if (!WarUtil.eligibleForWar(war.getWarType(), town)) {
			war.addErrorMsg("The town " + town + " fought too recently. They will not take part in the war.");
			return false;
		}

		if (uuidsToIgnore.contains(town.getUUID())) {
			war.addErrorMsg("The town " + town + " was already removed from this war. They will remain removed.");
			return false;
		}

		/*
		 * Homeblocks are absolutely required for a war.
		 */
		if (!town.hasHomeBlock()) {
			war.addErrorMsg("The town " + town + " does not have a homeblock. They will not take part in the war.");
			return false;
		}

		if (!town.getHomeBlockOrNull().getWorld().isWarAllowed()) {
			war.addErrorMsg("The town " + town + " exists in a world with war disabled. They will not take part in the war.");
			return false;
		}

		return true;
	}

}
