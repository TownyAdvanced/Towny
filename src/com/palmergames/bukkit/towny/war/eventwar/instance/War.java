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
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.NameGenerator;
import com.palmergames.bukkit.towny.war.eventwar.WarBooks;
import com.palmergames.bukkit.towny.war.eventwar.WarDataBase;
import com.palmergames.bukkit.towny.war.eventwar.WarMetaDataController;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.towny.war.eventwar.events.EventWarEndEvent;
import com.palmergames.bukkit.towny.war.eventwar.events.EventWarPreStartEvent;
import com.palmergames.bukkit.towny.war.eventwar.events.EventWarStartEvent;
import com.palmergames.bukkit.util.BookFactory;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.KeyValue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
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
			TownyMessaging.sendGlobalMessage("War Event cannot function while using_economy: false in the config.yml. Economy Required.");
        	return;
		}

		this.plugin = plugin;
		this.warType = warType;
		
		/*
		 * Currently only used to add money to the war spoils.
		 */
		EventWarPreStartEvent preEvent = new EventWarPreStartEvent();
		Bukkit.getServer().getPluginManager().callEvent(preEvent);
		if (preEvent.getWarSpoils() != 0.0)
			warSpoils = preEvent.getWarSpoils();

		/*
		 * Attempts to gather the given lists of nations/towns/residents into a War
		 * with at least one pair of opposing teams.
		 */
		if (!warParticipants.gatherParticipantsForWar(nations, towns, residents)) {
			end(false);
			return;
		}	

		/*
		 * Get a name for the war.
		 */
		setWarName();
		
		/*
		 * Seed the war spoils.
		 */
		getMessenger().sendGlobalMessage(Translatable.of("msg_war_seeding_spoils_with", TownySettings.getBaseSpoilsOfWar()));			
		getMessenger().sendGlobalMessage(Translatable.of("msg_war_total_seeding_spoils", TownyEconomyHandler.getFormattedBalance(warSpoils)));
		getMessenger().sendGlobalMessage(Translatable.of("msg_war_activate_war_hud_tip"));
		
		EventWarStartEvent event = new EventWarStartEvent(warParticipants.getTowns(), warParticipants.getNations(), warSpoilsAtStart);
		Bukkit.getServer().getPluginManager().callEvent(event);
		
		/*
		 * If things have gotten this far it is reasonable to think we can start the war.
		 */
		taskManager.setupDelay(startDelay);
		
		saveWar();
	}
	
	public War(Towny plugin, UUID uuid) {
		this.plugin = plugin;
		this.warUUID = uuid;
	}
	
	public void loadWar(List<Nation> nations, List<Town> towns, List<Resident> residents, List<TownBlock> townblocks) {
		if (!warParticipants.gatherParticipantsForWar(nations, towns, residents)) {
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

		ItemStack book = BookFactory.makeBook(warName, "War Declared", WarBooks.warStartBook(this));
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident != null && warParticipants.has(resident)) {
				player.getInventory().addItem(book);
				warParticipants.addOnlineWarrior(player);
			}
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
			if (warParticipants.getGovSide().isEmpty() || warParticipants.getRebSide().isEmpty())
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
			if (warParticipants.getGovSide().isEmpty() || warParticipants.getRebSide().isEmpty())
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
		
		/*
		 * Print out stats to players
		 */
		getMessenger().sendGlobalMessage(getScoreManager().getStats());
		
		/*
		 * End the WarTimerTask
		 */
		taskManager.cancelTasks(BukkitTools.getScheduler());

		/*
		 * Kill the war huds.
		 */
		removeWarHuds(this);
		
		/*
		 * Pay out the money.
		 */
		if (endedSuccessful)
			awardSpoils();

		/*
		 * Remove this war.
		 */
		WarDataBase.removeWar(this);
		
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
					getMessenger().sendGlobalMessage(Translation.of("msg_war_surviving_nation_spoils", winningNation.getName(), amount));
				}
			} catch (ArithmeticException e) {
				TownyMessaging.sendDebugMsg("[War]   War ended with 0 nations.");
			}

			// Pay money to winning town and print message
			try {
				KeyValue<Town, Integer> winningTownScore = getScoreManager().getWinningTownScore();
				winningTownScore.key.getAccount().deposit(halfWinnings, "War - Town Winnings");
				getMessenger().sendGlobalMessage(Translation.of("MSG_WAR_WINNING_TOWN_SPOILS", winningTownScore.key.getName(), TownyEconomyHandler.getFormattedBalance(halfWinnings),  winningTownScore.value));
				
				EventWarEndEvent event = new EventWarEndEvent(getWarParticipants().getTowns(), winningTownScore.key, halfWinnings, getWarParticipants().getNations(), nationWinnings);
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
					getMessenger().sendGlobalMessage(Translation.of("msg_war_surviving_town_spoils", winningTown.getName(), amount));
				}
			} catch (ArithmeticException e) {
				TownyMessaging.sendDebugMsg("[War]   War ended with 0 nations.");
			}

			// Pay money to winning town and print message
			try {
				KeyValue<Town, Integer> winningTownScore = getScoreManager().getWinningTownScore();
				winningTownScore.key.getAccount().deposit(halfWinnings, "War - Town Winnings");
				getMessenger().sendGlobalMessage(Translation.of("MSG_WAR_WINNING_TOWN_SPOILS", winningTownScore.key.getName(), TownyEconomyHandler.getFormattedBalance(halfWinnings),  winningTownScore.value));
				
				EventWarEndEvent event = new EventWarEndEvent(getWarParticipants().getTowns(), winningTownScore.key, halfWinnings, null, townWinnings);
				Bukkit.getServer().getPluginManager().callEvent(event);
			} catch (TownyException e) {
			}
		case RIOT:
			double residentWinnings = halfWinnings / getWarParticipants().getResidents().size();
			String amount = TownyEconomyHandler.getFormattedBalance(residentWinnings);
			Resident highScore = null;
			for (Resident survivor : getWarParticipants().getResidents()) {
				survivor.getAccount().deposit(residentWinnings, "War - Riot Survivor Winnings");
				getMessenger().sendGlobalMessage("Riot survivor " + survivor.getName() + " wins " + amount);
				
				if (highScore == null) {
					highScore = survivor;
					continue;
				}
				
				if (WarMetaDataController.getScore(survivor) > WarMetaDataController.getScore(highScore))
					highScore = survivor;
			}
			// Pay out the other half to the highest scorer.
			highScore.getAccount().deposit(halfWinnings, "War - Riot High Score Winnings");
			getMessenger().sendGlobalMessage(Translation.of("msg_war_riot_highest_score", highScore.getName(), TownyEconomyHandler.getFormattedBalance(halfWinnings),  WarMetaDataController.getScore(highScore)));
			
			EventWarEndEvent event = new EventWarEndEvent(null, highScore.getTownOrNull(), halfWinnings, null, residentWinnings);
			Bukkit.getServer().getPluginManager().callEvent(event);
		default:
		}
	}

}