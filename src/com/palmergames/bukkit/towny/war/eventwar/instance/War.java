package com.palmergames.bukkit.towny.war.eventwar.instance;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.NameGenerator;
import com.palmergames.bukkit.towny.war.eventwar.WarBooks;
import com.palmergames.bukkit.towny.war.eventwar.WarDataBase;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.towny.war.eventwar.events.EventWarEndEvent;
import com.palmergames.bukkit.towny.war.eventwar.events.EventWarStartEvent;
import com.palmergames.bukkit.util.BookFactory;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.KeyValue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class War {
	
	private Towny plugin;
	// War Managers
	private WarZoneManager warZoneManager = new WarZoneManager(this);
	private WarParticipants warParticipants = new WarParticipants(this);
	private ScoreManager scoreManager = new ScoreManager(this);
	private WarTaskManager taskManager = new WarTaskManager(this);
	private WarMessenger messenger = new WarMessenger(this);
	private List<String> errorMsgs = new ArrayList<>();
	
	private WarType warType;
	private String warName;	
	private UUID warUUID = UUID.randomUUID();
	
	public double warSpoilsAtStart = 0.0;
	double warSpoils = 0.0;

	/**
	 * Creates a new War instance.
	 * 
	 * @param plugin - Towny plugin.
	 * @param startDelay - int startup delay in seconds.
	 * @param nations - List&lt;Nation&gt; which will be tested and added to war.
	 * @param towns - List&lt;Town&gt; which will be tested and added to war.
	 * @param residents - List&lt;Resident&gt; which will be tested and added to war.
	 * @param warType - WarType of the war being started.
	 */
	public War(Towny plugin, int startDelay, List<Nation> nations, List<Town> towns, List<Resident> residents, WarType warType) {

		
		if (!TownySettings.isUsingEconomy()) {
			addErrorMsg("War Event cannot function while using_economy: false in the config.yml. Economy Required.");
			end(false);
        	return;
		}

		this.plugin = plugin;
		this.warType = warType;

		/*
		 * Attempts to gather the given lists of nations/towns/residents into a War
		 * with at least one pair of opposing teams.
		 */
		warParticipants.gatherParticipantsForWar(nations, towns, residents);
		
		/*
		 * A warmup period is used for civil war and riots, so sides are settled.
		 * 
		 */
		int warmup;
		switch (warType) {
			case RIOT:
			case CIVILWAR:
				warmup = 120;
				getMessenger().sendGlobalMessage("There are 120 seconds to choose between the government and anti-government sides.");
				break;
			case NATIONWAR:
			case TOWNWAR:
			case WORLDWAR:
			default:
				warmup = 0;
		}
		
		Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
			/*
			 * Make sure that we have enough people/towns/nations involved
			 * for the give WarType.
			 */
			if (!warParticipants.verifyTwoEnemies()) {

				addErrorMsg("Failed to get the correct number of teams for war to happen! Good-bye!");
				end(false);

			} else {
				
				/*
				 * Some minor errors could have been recorded, report them to the people who did enter the war.
				 */
				if (!errorMsgs.isEmpty())
					WarMessenger.reportMinorErrors();
				
				/*
				 * Get a name for the war.
				 */
				setWarName();
				
				/*
				 * Seed the war spoils.
				 */
				warSpoils = warType.baseSpoils;
				getMessenger().sendGlobalMessage(Translatable.of("msg_war_seeding_spoils_with", TownyEconomyHandler.getFormattedBalance(warSpoils)));			
				getMessenger().sendGlobalMessage(Translatable.of("msg_war_activate_war_hud_tip"));
				
				EventWarStartEvent event = new EventWarStartEvent(warParticipants.getTowns(), warParticipants.getNations(), warSpoilsAtStart);
				Bukkit.getServer().getPluginManager().callEvent(event);
				
				/*
				 * If things have gotten this far it is reasonable to think we can start the war.
				 */
				taskManager.setupDelay(startDelay);
				
				saveWar();
			}
		}, warmup * 20);
	}

	public War(Towny plugin, UUID uuid) {
		this.plugin = plugin;
		this.warUUID = uuid;
	}
	
	public void loadWar(List<Nation> nations, List<Town> towns, List<Resident> residents, List<TownBlock> townblocks) {

		warParticipants.gatherParticipantsForWar(nations, towns, residents);

		if (!warParticipants.verifyTwoEnemies()) {
			addErrorMsg("Failed to get the correct number of teams for war to happen! Good-bye!");
			end(false);
			return;
		}
		
		for (TownBlock tb : townblocks) {
			if (tb.isHomeBlock())
				warZoneManager.addWarZone(tb.getWorldCoord(), TownySettings.getWarzoneHomeBlockHealth());
			else 
				warZoneManager.addWarZone(tb.getWorldCoord(), TownySettings.getWarzoneTownBlockHealth());
		}
		
		taskManager.setupDelay(0);
	}
	
	public void saveWar() {
		TownyUniverse.getInstance().getDataSource().saveWar(this);
	}

	/*
	 * War Start and End Methods.
	 */

	/**
	 * Start the war.
	 * 
	 * Starts the timer taxks.
	 */
	public void start() {
		
		warParticipants.outputParticipants(warType, warName);

		// Start the WarTimerTask if the war type allows for using townblock HP system.
		if (warType.hasTownBlockHP)
			taskManager.scheduleWarTimerTask(this.plugin);

		// Give the entire server a War Declared book,
		// Add war participants to the onlineWarriors (if they aren't already.)
		ItemStack book = BookFactory.makeBook(warName, "War Declared", WarBooks.warStartBook(this));
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.getInventory().addItem(book);
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident != null && warParticipants.has(resident))
				warParticipants.addOnlineWarrior(player);
		}
		
		checkEnd();
		TownyUniverse.getInstance().addWar(this);
	}

	/**
	 * Checks if the end has been reached.
	 */
	public void checkEnd() {

		switch(warType) {
		case WORLDWAR:
		case NATIONWAR:
			if (warParticipants.getNations().size() <= 1)
				end(true);
			else if (CombatUtil.areAllAllies(warParticipants.getNations()))
				end(true);
			break;
		case CIVILWAR:
			if (warParticipants.getTowns().size() <= 1)
				end(true);
			else if (warParticipants.getGovSide().isEmpty() || warParticipants.getRebSide().isEmpty())
				end(true);
			break;
		case TOWNWAR:
			if (warParticipants.getTowns().size() <= 1)
				end(true);
			// TODO: Handle town neutrality.
			break;
		case RIOT:
			if (warParticipants.getResidents().size() <= 1)
				end(true);
			else if (warParticipants.getGovSide().isEmpty() || warParticipants.getRebSide().isEmpty())
				end(true);
			break;
		}
	}

	/**
	 * Ends the war.
	 * 
	 * Send the stats to all the players, toggle all the war HUDS.
	 * @param endedSuccessful - False if something has caused the war to finish before it should have.
	 */
	public void end(boolean endedSuccessful) {
	
		if (endedSuccessful) {
			/*
			 * Print out stats to players
			 */
			getMessenger().sendPlainGlobalMessage(getScoreManager().getStats());
			
			/*
			 * Pay out the money.
			 */
			awardSpoils();
			
			/*
			 * Consider takeovers.
			 */
			handleAftermath();
			
		} else {
			// War ended suddenly, send any available errors.
			if (!errorMsgs.isEmpty()) 
				WarMessenger.reportErrors();
		}
		
		/*
		 * End the WarTimerTask
		 */
		taskManager.cancelTasks(BukkitTools.getScheduler());

		/*
		 * Kill the war huds.
		 */
		removeWarHuds(this);
					
		/*
		 * Remove this war.
		 */
		WarDataBase.removeWar(this);
		TownyUniverse.getInstance().removeWar(this);
		
	}

	/*
	 * Getters and Setters
	 */

	public Towny getPlugin() {
		return plugin;
	}

	public WarZoneManager getWarZoneManager() {
		return warZoneManager;
	}
	
	public WarType getWarType() {
		return warType;
	}
	
	public void setWarType(WarType type) {
		this.warType = type;
	}

	public WarParticipants getWarParticipants() {
		return warParticipants;
	}
	
	public ScoreManager getScoreManager() {
		return scoreManager;
	}

	public WarMessenger getMessenger() {
		return messenger;
	}

	public double getWarSpoils() {

		return warSpoils;
	}
	
	public void setWarSpoils(double spoils) {
		warSpoils = spoils;
	}
	
	public String getWarName() {
		return warName;
	}
	
	public void setWarName(String name) {
		this.warName = name;
	}
	
	public UUID getWarUUID() {
		return warUUID;
	}
	
	public void setUUID(UUID uuid) {
		this.warUUID = uuid;
	}
	
	public void addErrorMsg(String string) {
		errorMsgs.add(string);
	}
	
	public List<String> getErrorMsgs() {
		return errorMsgs;
	}

	/**
	 * Picks out a name (sometimes a randomly generated one,) for the war.
	 */
	private void setWarName() {
		
		String warName = null;
		switch (warType) {
		case WORLDWAR:
			warName = String.format("World War of %s", NameGenerator.getRandomWarName());
			break;
		case NATIONWAR:			
			warName = String.format("War of %s", NameGenerator.getRandomWarName());
			break;
		case TOWNWAR:
			warName = String.format("%s - %s Skirmish", warParticipants.getTowns().get(0), warParticipants.getTowns().get(1));
			break;
		case CIVILWAR:
			warName = String.format("%s Civil War", warParticipants.getNations().get(0));
			break;
		case RIOT:
			warName = String.format("%s Riot", warParticipants.getTowns().get(0));
			break;
		}
		this.warName = warName;
	}

	/*
	 * Private End of War Methods
	 */
	
	/**
	 * Remove all the war huds.
	 * @param war
	 */
	private void removeWarHuds(War war) {
		new BukkitRunnable() {

			@Override
			public void run() {
				plugin.getHUDManager().toggleAllWarHUD(war);
			}
			
		}.runTask(plugin);		
	}
	
	/**
	 * Pay out the money to the winner(s).
	 */
	private void awardSpoils() {
		double halfWinnings = getWarSpoils() / 2.0;
		switch (warType) {
		case WORLDWAR:
		case NATIONWAR:
			double nationWinnings = 0;
			try {
				nationWinnings = halfWinnings / getWarParticipants().getNations().size(); // Again, might leave residue.
				String amount = TownyEconomyHandler.getFormattedBalance(nationWinnings);
				for (Nation winningNation : getWarParticipants().getNations()) {
					winningNation.getAccount().deposit(nationWinnings, "War - Surviving Nation Winnings");
					getMessenger().sendGlobalMessage(Translatable.of("msg_war_surviving_nation_spoils", winningNation.getName(), amount));
				}
			} catch (ArithmeticException e) {
				TownyMessaging.sendDebugMsg("[War]   War ended with 0 nations.");
			}

			// Pay money to winning town and print message
			try {
				KeyValue<TownyObject, Integer> winningTownScore = getScoreManager().getWinningScore();
				Town winningTown = ((Town)winningTownScore.key);
				winningTown.getAccount().deposit(halfWinnings, "War - Town Winnings");
				getMessenger().sendGlobalMessage(Translatable.of("MSG_WAR_WINNING_TOWN_SPOILS", winningTown.getName(), TownyEconomyHandler.getFormattedBalance(halfWinnings),  winningTownScore.value));
				
				EventWarEndEvent event = new EventWarEndEvent(getWarParticipants().getTowns(), winningTown, halfWinnings, getWarParticipants().getNations(), nationWinnings);
				Bukkit.getServer().getPluginManager().callEvent(event);
			} catch (TownyException e) {
			}
		case CIVILWAR:
		case TOWNWAR:
			double townWinnings = 0;
			// Compute war spoils
			try {
				townWinnings = halfWinnings / getWarParticipants().getTowns().size(); // Again, might leave residue.
				String amount = TownyEconomyHandler.getFormattedBalance(townWinnings);
				for (Town winningTown : getWarParticipants().getTowns()) {
					winningTown.getAccount().deposit(townWinnings, "War - Surviving Town Winnings");
					getMessenger().sendGlobalMessage(Translatable.of("msg_war_surviving_town_spoils", winningTown.getName(), amount));
				}
			} catch (ArithmeticException e) {
				TownyMessaging.sendDebugMsg("[War]   War ended with 0 towns.");
			}

			// Pay money to winning town and print message
			try {
				KeyValue<TownyObject, Integer> winningTownScore = getScoreManager().getWinningScore();
				Town winningTown = ((Town)winningTownScore.key);
				winningTown.getAccount().deposit(halfWinnings, "War - Town Winnings");
				getMessenger().sendGlobalMessage(Translatable.of("MSG_WAR_WINNING_TOWN_SPOILS", winningTown.getName(), TownyEconomyHandler.getFormattedBalance(halfWinnings),  winningTownScore.value));
				
				EventWarEndEvent event = new EventWarEndEvent(getWarParticipants().getTowns(), winningTown, halfWinnings, null, townWinnings);
				Bukkit.getServer().getPluginManager().callEvent(event);
			} catch (TownyException e) {
			}
		case RIOT:
			double residentWinnings = halfWinnings / getWarParticipants().getResidents().size();
			String amount = TownyEconomyHandler.getFormattedBalance(residentWinnings);
			for (Resident survivor : getWarParticipants().getResidents()) {
				survivor.getAccount().deposit(residentWinnings, "War - Riot Survivor Winnings");
				getMessenger().sendGlobalMessage("Riot survivor " + survivor.getName() + " wins " + amount);
			}
			try {
				KeyValue<TownyObject, Integer> winningResidentScore = getScoreManager().getWinningScore();
				Resident winningResident = ((Resident)winningResidentScore.key);
				// Pay out the other half to the highest scorer.
				winningResident.getAccount().deposit(halfWinnings, "War - Riot High Score Winnings");
				getMessenger().sendGlobalMessage(Translatable.of("msg_war_riot_highest_score", winningResident.getName(), TownyEconomyHandler.getFormattedBalance(halfWinnings),  winningResidentScore.value));
				
				EventWarEndEvent event = new EventWarEndEvent(null, winningResident.getTownOrNull(), halfWinnings, null, residentWinnings);
				Bukkit.getServer().getPluginManager().callEvent(event);
			} catch (TownyException e) {
			}
		default:
		}
	}


	/**
	 * Handles the Town Conquering.
	 */
	private void handleAftermath() {
		if (warType.hasTownConquering) {
			switch (warType) {
			case RIOT:
				Resident newMayor = null;
				try {
					newMayor = ((Resident) getScoreManager().getWinningScore().key);
				} catch (TownyException ignored) {}
				if (newMayor.isMayor() || warParticipants.getGovSide().contains(newMayor)) {
					TownyMessaging.sendTownMessagePrefixed(newMayor.getTownOrNull(), Translatable.of("msg_mayor_retains_control_of_the_city", newMayor, newMayor.getTownOrNull()));
				} else {
					Town town = newMayor.getTownOrNull();
					town.setMayor(newMayor);
					town.save();
					TownyMessaging.sendTownMessagePrefixed(town, Translatable.of("msg_new_mayor_has_become_the_new_mayor_of", newMayor, town));
				}
				break;
			case CIVILWAR:
				Town newCapital = null;
				try {
					newCapital = ((Town) getScoreManager().getWinningScore().key);
				} catch (TownyException ignored) {}
				if (newCapital.isCapital() || warParticipants.getGovSide().contains(newCapital)) {
					TownyMessaging.sendNationMessagePrefixed(newCapital.getNationOrNull(), Translatable.of("msg_king_retains_control_of_the_nation", newCapital.getMayor(), newCapital.getNationOrNull()));
				} else {
					Nation nation = newCapital.getNationOrNull();
					nation.setCapital(newCapital);
					nation.save();
					TownyMessaging.sendNationMessagePrefixed(newCapital.getNationOrNull(), Translatable.of("msg_new_king_has_become_the_new_king_of", newCapital.getMayor(), newCapital.getNationOrNull()));
				}
				break;
			case TOWNWAR:
				Town remainingTown = null;
				try {
					remainingTown = ((Town) getScoreManager().getWinningScore().key);
				} catch (TownyException ignored) {}
				
				for (Town town : warParticipants.getTowns()) {
					if (town.equals(remainingTown))
						continue;
					TownyMessaging.sendTownMessagePrefixed(remainingTown, Translatable.of("msg_town_has_fallen_and_is_being_absorbed_by_the_winning_town", town, remainingTown));
					TownyUniverse.getInstance().getDataSource().mergeTown(remainingTown, town);
				}
				break;

			// nation war and world war handle conquering mid-war in WarZoneManager.
			case NATIONWAR:
			case WORLDWAR:
			default:
				break;
			}
		}

		
	}

}