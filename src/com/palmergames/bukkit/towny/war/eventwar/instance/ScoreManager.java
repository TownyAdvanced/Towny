package com.palmergames.bukkit.towny.war.eventwar.instance;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.towny.war.eventwar.db.WarMetaDataController;
import com.palmergames.bukkit.towny.war.eventwar.events.TownScoredEvent;
import com.palmergames.bukkit.towny.war.eventwar.settings.EventWarSettings;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.KeyValue;
import com.palmergames.util.KeyValueTable;

public class ScoreManager {

	private War war;
	private Hashtable<TownyObject, Integer> scores = new Hashtable<>();
	
	public ScoreManager(War war) {
		this.war = war;
	}

	public Hashtable<TownyObject, Integer> getScores() {
		return scores;
	}
	
	/*
	 * Scoring Methods
	 */
	
	/**
	 * A resident has killed another resident.
	 * @param defender - {@link Resident} dying.
	 * @param attacker - {@link Resident} killing.
	 * @param loc - {@link Location} of the death.
	 */
	public void residentScoredKillPoints(Resident defender,  Resident attacker, Location loc) {
		switch(war.getWarType()) {
		case RIOT:
			residentScored(defender, attacker);
			break;
		default:
			townScored(defender, attacker, loc);			
			break;
		}
	}
	
	public void addTown(Town town) {
		scores.put(town, WarMetaDataController.getScore(town));
	}
	
	public void addResident(Resident resident) {
		scores.put(resident, WarMetaDataController.getScore(resident));
	}

	private void residentScored(Resident defender, Resident attacker) {
		int points = war.getWarType().pointsPerKill;
		
		scores.put(attacker, scores.get(attacker) + points);
		WarMetaDataController.setScore(attacker, points);

		war.getMessenger().sendGlobalMessage(Translatable.of("MSG_WAR_SCORE_PLAYER_KILL", attacker, defender, points, attacker));
	}
	
	/**
	 * A town has scored a kill point.
	 * 
	 * @param defender - {@link Resident} dying.
	 * @param attackerRes - {@link Resident} killing.
	 * @param loc - {@link Location} of the death.
	 */
	private void townScored(Resident defender, Resident attacker, Location loc) {
		int points = war.getWarType().pointsPerKill;
		Town attackerTown = attacker.getTownOrNull();
		Town defenderTown = defender.getTownOrNull();
		if (attackerTown == null || defenderTown == null)
			return;

		Translatable pointMessage;
		TownBlock deathLoc = TownyAPI.getInstance().getTownBlock(loc);
		if (deathLoc == null)
			pointMessage = Translatable.of("MSG_WAR_SCORE_PLAYER_KILL", attacker, defender, points, attackerTown);
		else if (war.getWarZoneManager().isWarZone(deathLoc.getWorldCoord()) && attackerTown.getTownBlocks().contains(deathLoc))
			pointMessage = Translatable.of("MSG_WAR_SCORE_PLAYER_KILL_DEFENDING", attacker, defender, attacker, points, attackerTown);
		else if (war.getWarZoneManager().isWarZone(deathLoc.getWorldCoord()) && defenderTown.getTownBlocks().contains(deathLoc))
			pointMessage = Translatable.of("MSG_WAR_SCORE_PLAYER_KILL_DEFENDING", attacker, defender, defender, points, attackerTown);
		else
			pointMessage = Translatable.of("MSG_WAR_SCORE_PLAYER_KILL", attacker, defender, points, attackerTown);

		scores.put(attackerTown, scores.get(attackerTown) + points);
		WarMetaDataController.setScore(attackerTown, scores.get(attackerTown));
		
		war.getMessenger().sendGlobalMessage(pointMessage);
		TownScoredEvent event = new TownScoredEvent(attackerTown, scores.get(attackerTown), war);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}
	
	/**
	 * A town has scored.
	 * @param town - the scoring town
	 * @param n - the score to be added
	 * @param fallenObject - the {@link Object} that fell
	 * @param townBlocksFallen -  the number of fallen townblocks {@link TownBlock}s ({@link Integer})
	 */
	void townScored(Town town, int n, TownyObject fallenObject, int townBlocksFallen) {

		Translatable pointMessage = null;
		if (fallenObject instanceof Nation)
			pointMessage = Translatable.of("MSG_WAR_SCORE_NATION_ELIM", town, n, fallenObject.getName());
		else if (fallenObject instanceof Town)
			pointMessage = Translatable.of("MSG_WAR_SCORE_TOWN_ELIM", town, n, fallenObject.getName(), townBlocksFallen);
		else if (fallenObject instanceof TownBlock){
			pointMessage = Translatable.of("MSG_WAR_SCORE_TOWNBLOCK_ELIM", town, n, formattedTownBlock((TownBlock)fallenObject));
				
		}

		scores.put(town, scores.get(town) + n);
		
		// If this is a civil war and the fallen town is the capital and 
		// town conquering is true: make sure the attacking town will 
		// have the highest score, so they will take over the Nation.
		if (war.getWarType().equals(WarType.CIVILWAR) 
		&& war.getWarType().hasTownConquering 
		&& war.getWarParticipants().getRebSide().contains(town)
		&& fallenObject instanceof Town
		&& ((Town) fallenObject).isCapital()
		&& !getFirstPlace().getName().equals(town.getName())) {
			scores.put(town, getWinningScore().value + 100);
		}
		
		WarMetaDataController.setScore(town, scores.get(town));

		war.getMessenger().sendGlobalMessage(pointMessage);
		TownScoredEvent event = new TownScoredEvent(town, scores.get(town), war);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

	void processScoreOnFallenTown(Town town, Town attacker) {
		/*
		 * Count warzones that fell when the town was felled.
		 */
		int fallenTownBlocks = 0;
		for (TownBlock townBlock : town.getTownBlocks())
			if (war.getWarZoneManager().isWarZone(townBlock.getWorldCoord()))
				fallenTownBlocks++;
		
		// TODO: Another message for bulk townblock points from townblocks that did not fall until now. (Mirrors how nations' falling gives points for the eliminated towns.)
		// TODO: A config option to not pay points for townblocks which were not directly captured preventing bulk points.
		
		/*
		 * Award points for the captured town.
		 */
		townScored(attacker, EventWarSettings.getWarPointsForTown(), town, fallenTownBlocks);
	}
	

	public void sendScores(Player player) {

		sendScores(player, 10);
	}

	public void sendScores(Player player, int maxListing) {

		for (String line : getScores(maxListing, true))
			player.sendMessage(line);
	}
	
	/**
	 * Gets the scores of a {@link War}
	 * @param maxListing Maximum lines to return. Value of -1 return all.
	 * @return A list of the current scores per town sorted in descending order.
	 */
	public List<String> getScores(int maxListing, boolean title) {

		List<String> output = new ArrayList<>();
		if (title)
			output.add(ChatTools.formatTitle("War - Top Scores"));
		if (!hasAnyoneScored()) {
			output.add(ChatTools.formatCommand("None", "", ""));
			return output;
		}
		int n = 0;
		for (KeyValue<TownyObject, Integer> kv : getOrderedScores().getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			TownyObject townyObject = kv.key;
			int score = kv.value;
			if (score > 0)
				output.add(String.format(Colors.Blue + "%40s " + Colors.Gold + "|" + Colors.LightGray + " %4d", townyObject.getFormattedName(), score));
		}
		return output;
	}

	public String[] getTopThree() {
		KeyValueTable<TownyObject, Integer> kvTable = getOrderedScores();
		String[] top = new String[3];
		top[0] = kvTable.getKeyValues().size() >= 1 ? kvTable.getKeyValues().get(0).value + "-" + kvTable.getKeyValues().get(0).key : "";
		top[1] = kvTable.getKeyValues().size() >= 2 ? kvTable.getKeyValues().get(1).value + "-" + kvTable.getKeyValues().get(1).key : "";
		top[2] = kvTable.getKeyValues().size() >= 3 ? kvTable.getKeyValues().get(2).value + "-" + kvTable.getKeyValues().get(2).key : "";
		return top;
	}

	public boolean hasAnyoneScored() {
		return getOrderedScores().getKeyValues().get(0).value > 0;
	}
	
	public TownyObject getFirstPlace() {
		return getOrderedScores().getKeyValues().get(0).key;
	}
	
	public TownyObject getSecondPlace() {
		return getOrderedScores().getKeyValues().get(1).key;
	}

	public KeyValue<TownyObject, Integer> getWinningScore() {
		return getOrderedScores().getKeyValues().get(0);
	}

	/*
	 * Stats
	 */

	public List<String> getStats() {
		
		List<String> output = new ArrayList<>();
		WarParticipants participants = war.getWarParticipants();
		output.add(ChatTools.formatTitle("War Stats"));
		
		switch (war.getWarType()) {
			case WORLDWAR:
			case NATIONWAR:
				output.add(Colors.Green + Translation.of("war_stats_nations") + Colors.LightGreen + participants.getNations().size() + " / " + war.getNationsAtStart());
			case CIVILWAR:
			case TOWNWAR:
				output.add(Colors.Green + Translation.of("war_stats_towns") + Colors.LightGreen + participants.getTowns().size() + " / " + war.getTownsAtStart());
			case RIOT:
				output.add(Colors.Green + "  Residents: " + Colors.LightGreen + participants.getResidents().size() + " / " + war.getResidentsAtStart());
				break;
		}		
		if (war.getWarType().hasTownBlockHP)
			output.add(Colors.Green + Translation.of("war_stats_warzone") + Colors.LightGreen + war.getWarZoneManager().getWarZone().size() + " Town blocks");
		output.add(Colors.Green + Translation.of("war_stats_spoils_of_war") + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(war.getWarSpoils()));
		return output;
	}
	
	public void sendStats(Player player) {

		for (String line : getStats())
			player.sendMessage(line);
	}

	private KeyValueTable<TownyObject, Integer> getOrderedScores() {
		KeyValueTable<TownyObject, Integer> kvTable = new KeyValueTable<>(scores);
		kvTable.sortByValue();
		kvTable.reverse();
		return kvTable;
	}
	
	private String formattedTownBlock(TownBlock townBlock) {
		return "[" + townBlock.getTownOrNull() + "](" + townBlock.getCoord().toString() + ")";
	}
}
