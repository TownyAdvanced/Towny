package com.palmergames.bukkit.towny.war.eventwar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.ServerBroadCastTimerTask;
import com.palmergames.util.KeyValue;
import com.palmergames.util.KeyValueTable;
import com.palmergames.util.TimeMgmt;
import com.palmergames.util.TimeTools;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

//TODO: Extend a new class called TownyEvent
public class War {
	
	// War Data
	private static Hashtable<WorldCoord, Integer> warZone = new Hashtable<WorldCoord, Integer>();
	private Hashtable<Town, Integer> townScores = new Hashtable<Town, Integer>();
	public static List<Town> warringTowns = new ArrayList<Town>();
	public static List<Nation> warringNations = new ArrayList<Nation>();
	private WarSpoils warSpoils = new WarSpoils();
	
	private Towny plugin;
	private TownyUniverse universe;
	private boolean warTime = false;
	private List<Integer> warTaskIds = new ArrayList<Integer>();

	/**
	 * Creates a new War instance.
	 * @param plugin
	 * @param startDelay - the delay before war will begin
	 */
	public War(Towny plugin, int startDelay) {

		this.plugin = plugin;
		this.universe = plugin.getTownyUniverse();
		setupDelay(startDelay);
	}

	////START GETTERS/SETTERS ////
	
	public void addTaskId(int id) {

		warTaskIds.add(id);
	}

	public void clearTaskIds() {

		warTaskIds.clear();
	}

	public void cancelTasks(BukkitScheduler scheduler) {

		for (Integer id : getTaskIds())
			scheduler.cancelTask(id);
		clearTaskIds();
	}
	
	public void setPlugin(Towny plugin) {

		this.plugin = plugin;
	}

	public List<Integer> getTaskIds() {

		return new ArrayList<Integer>(warTaskIds);
	}

	public Towny getPlugin() {

		return plugin;
	}

	public boolean isWarTime() {

		return warTime;
	}

	public TownyUniverse getTownyUniverse() {

		return universe;
	}
	
	public WarSpoils getWarSpoils() {

		return warSpoils;
	}

	public Hashtable<Town, Integer> getTownScores()
	{
		return townScores;
	}

	public Hashtable<WorldCoord, Integer> getWarZone()
	{
		return warZone;
	}

	public List<Town> getWarringTowns()
	{
		return warringTowns;
	}
	
	public static boolean isWarZone(WorldCoord worldCoord) {

		return warZone.containsKey(worldCoord);
	}

	public boolean isWarringNation(Nation nation) {

		return warringNations.contains(nation);
	}

	public static boolean isWarringTown(Town town) {

		return warringTowns.contains(town);
	}
	
	public void toggleEnd() {

		warTime = false;
	}

	//// END GETTERS/SETTERS ////
	
	/**
	 * Creates a delay before war begins
	 * @param delay
	 */
	public void setupDelay(int delay) {

		if (delay <= 0)
			start();
		else {
			// Create a countdown timer
			for (Long t : TimeMgmt.getCountdownDelays(delay, TimeMgmt.defaultCountdownDelays)) {
				int id = BukkitTools.scheduleAsyncDelayedTask(new ServerBroadCastTimerTask(plugin, String.format("War starts in %s", TimeMgmt.formatCountdownTime(t))), TimeTools.convertToTicks((delay - t)));
				if (id == -1) {
					TownyMessaging.sendErrorMsg("Could not schedule a countdown message for war event.");
					end();
				} else
					addTaskId(id);
			}
			// Schedule set up delay
			int id = BukkitTools.scheduleAsyncDelayedTask(new StartWarTimerTask(plugin), TimeTools.convertToTicks(delay));
			if (id == -1) {
				TownyMessaging.sendErrorMsg("Could not schedule setup delay for war event.");
				end();
			} else {
				addTaskId(id);
				//start();
			}
		}
	}

	/**
	 * Start war.
	 * Seed the spoils, add the nations, start the timer task.
	 */
	public void start() {

		warTime = true;

		// Seed spoils of war		
		try {
			warSpoils.collect(TownySettings.getBaseSpoilsOfWar(), "Start of War - Base Spoils");			
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_war_seeding_spoils_with"), TownySettings.getBaseSpoilsOfWar()));			
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_war_total_seeding_spoils"), warSpoils.getHoldingBalance()));
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_war_activate_war_hud_tip")));
		} catch (EconomyException e) {
			TownyMessaging.sendErrorMsg("[War] Could not seed spoils of war.");
		}

		//Gather all nations at war
		for (Nation nation : TownyUniverse.getDataSource().getNations()) {
			if (!nation.isNeutral()) {
				add(nation);
				TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_war_join_nation"), nation.getName()));
			} else if (!TownySettings.isDeclaringNeutral()) {
				try {
					nation.setNeutral(false);
					add(nation);
					TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_war_join_forced"), nation.getName()));
				} catch (TownyException e) {
					e.printStackTrace();
				}
			}
		}

		// Start the WarTimerTask
		int id = BukkitTools.scheduleAsyncRepeatingTask(new WarTimerTask(plugin, this), 0, TimeTools.convertToTicks(5));
		if (id == -1) {
			TownyMessaging.sendErrorMsg("Could not schedule war event loop.");
			end();
		} else
			addTaskId(id);
		checkEnd();
	}

	/**
	 * End war.
	 * Send the stats to all the players, toggle all the war HUDS.
	 */
	public void end() {
		
		// Send stats to the players
		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				sendStats(player);
		
		// Toggle the war huds off for all players (This method is called from an async task so 
		// we create a sync task to use the scoreboard api)
		new BukkitRunnable() {

			@Override
			public void run() {
				plugin.getHUDManager().toggleAllWarHUD();
			}
			
		}.runTask(plugin);
		

		double halfWinnings;
		try {
			
			// Compute war spoils
			halfWinnings = getWarSpoils().getHoldingBalance() / 2.0;
			try {
				double nationWinnings = halfWinnings / warringNations.size(); // Again, might leave residue.
				for (Nation winningNation : warringNations) {
					getWarSpoils().payTo(nationWinnings, winningNation, "War - Nation Winnings");
					TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeWinningNationSpoilsMsg(winningNation, TownyEconomyHandler.getFormattedBalance(nationWinnings)));
				}
			} catch (ArithmeticException e) {
				TownyMessaging.sendDebugMsg("[War]   War ended with 0 nations.");
			}

			// Pay money to winning town and print message
			try {
				KeyValue<Town, Integer> winningTownScore = getWinningTownScore();
				getWarSpoils().payTo(halfWinnings, winningTownScore.key, "War - Town Winnings");
				TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeWinningTownSpoilsMsg(winningTownScore.key, TownyEconomyHandler.getFormattedBalance(halfWinnings), winningTownScore.value));
			} catch (TownyException e) {
			}
		} catch (EconomyException e1) {}
	}

	/**
	 * Add a nation to war, and all the towns within it.
	 * @param nation
	 */
	private void add(Nation nation) {

		for (Town town : nation.getTowns())
			if (town.getTownBlocks().size() > 0)
				add(town);
		warringNations.add(nation);
	}

	/**
	 * Add a town to war. Set the townblocks in the town to the correct health.
	 * @param town
	 */
	private void add(Town town) {

		TownyMessaging.sendTownMessage(town, TownySettings.getJoinWarMsg(town));
		townScores.put(town, 0);
		warringTowns.add(town);
		for (TownBlock townBlock : town.getTownBlocks())
			if (town.isHomeBlock(townBlock))
				warZone.put(townBlock.getWorldCoord(), TownySettings.getWarzoneHomeBlockHealth());
			else
				warZone.put(townBlock.getWorldCoord(), TownySettings.getWarzoneTownBlockHealth());
	}

	/**
	 * A town has scored.
	 * @param town - the scoring town
	 * @param n - the score to be added
	 * @param fallenObject - the object that fell
	 * @param townBlocksFallen -  the number of fallen townblocks
	 */
	public void townScored(Town town, int n, Object fallenObject, int townBlocksFallen) {

		String[] pointMessage = {"error"};
		if (fallenObject instanceof Nation)
			pointMessage = TownySettings.getWarTimeScoreNationEliminatedMsg(town, n, (Nation)fallenObject);
		else if (fallenObject instanceof Town)
			pointMessage = TownySettings.getWarTimeScoreTownEliminatedMsg(town, n, (Town)fallenObject, townBlocksFallen);
		else if (fallenObject instanceof TownBlock){	
			pointMessage = TownySettings.getWarTimeScoreTownBlockEliminatedMsg(town, n, (TownBlock)fallenObject);
		}

		townScores.put(town, townScores.get(town) + n);
		TownyMessaging.sendGlobalMessage(pointMessage);

		TownScoredEvent event = new TownScoredEvent(town, townScores.get(town));
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

	/**
	 * A town has scored and there is data on the attacking/defending town
	 * @param defenderTown
	 * @param attackerTown
	 * @param defenderPlayer
	 * @param attackerPlayer
	 * @param n - the score to be added
	 */
	public void townScored(Town defenderTown,  Town attackerTown, Player defenderPlayer, Player attackerPlayer, int n)
	{
		String[] pointMessage = {"error"};
		TownBlock deathLoc = TownyUniverse.getTownBlock(defenderPlayer.getLocation());
		if (deathLoc == null)
			pointMessage = TownySettings.getWarTimeScorePlayerKillMsg(attackerPlayer, defenderPlayer, n, attackerTown);
		else if (warZone.containsKey(deathLoc.getWorldCoord()) && attackerTown.getTownBlocks().contains(deathLoc))
			pointMessage = TownySettings.getWarTimeScorePlayerKillMsg(attackerPlayer, defenderPlayer, attackerPlayer, n, attackerTown);
		else if (warZone.containsKey(deathLoc.getWorldCoord()) && defenderTown.getTownBlocks().contains(deathLoc))
			pointMessage = TownySettings.getWarTimeScorePlayerKillMsg(attackerPlayer, defenderPlayer, defenderPlayer, n, attackerTown);
		else
			pointMessage = TownySettings.getWarTimeScorePlayerKillMsg(attackerPlayer, defenderPlayer, n, attackerTown);

		townScores.put(attackerTown, townScores.get(attackerTown) + n);
		TownyMessaging.sendGlobalMessage(pointMessage);

		TownScoredEvent event = new TownScoredEvent(attackerTown, townScores.get(attackerTown));
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

	/**
	 * Update a plot given the WarZoneData on the TownBlock
	 * @param townBlock
	 * @param wzd
	 * @throws NotRegisteredException
	 */
	public void updateWarZone (TownBlock townBlock, WarZoneData wzd) throws NotRegisteredException {
		if (!wzd.hasAttackers()) 
			healPlot(townBlock, wzd);
		else
			attackPlot(townBlock, wzd);
	}

	/**
	 * Heals a plot. Only occurs when the plot has no attackers.
	 * @param townBlock
	 * @param wzd
	 * @throws NotRegisteredException
	 */
	private void healPlot(TownBlock townBlock, WarZoneData wzd) throws NotRegisteredException {
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int healthChange = wzd.getHealthChange();
		int oldHP = warZone.get(worldCoord);
		int hp = getHealth(townBlock, healthChange);
		if (oldHP == hp)
			return;
		warZone.put(worldCoord, hp);
		String healString =  Colors.Gray + "[Heal](" + townBlock.getCoord().toString() + ") HP: " + hp + " (" + Colors.LightGreen + "+" + healthChange + Colors.Gray + ")";
		TownyMessaging.sendMessageToMode(townBlock.getTown(), healString, "");
		for (Player p : wzd.getDefenders()) {
			if (TownyUniverse.getDataSource().getResident(p.getName()).getTown() != townBlock.getTown())
				TownyMessaging.sendMessage(p, healString);
		}
		launchFireworkAtPlot (townBlock, wzd.getRandomDefender(), Type.BALL, Color.LIME);

		//Call PlotAttackedEvent to update scoreboard users
		PlotAttackedEvent event = new PlotAttackedEvent(townBlock, wzd.getAllPlayers(), hp);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

	/**
	 * There are attackers on the plot, update the health.
	 * @param townBlock
	 * @param wzd
	 * @throws NotRegisteredException
	 */
	private void attackPlot(TownBlock townBlock, WarZoneData wzd) throws NotRegisteredException {

		Player attackerPlayer = wzd.getRandomAttacker();
		Resident attackerResident = TownyUniverse.getDataSource().getResident(attackerPlayer.getName());
		Town attacker = attackerResident.getTown();

		//Health, messaging, fireworks..
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int healthChange = wzd.getHealthChange();
		int hp = getHealth(townBlock, healthChange);
		Color fwc = healthChange < 0 ? Color.RED : (healthChange > 0 ? Color.LIME : Color.GRAY);
		if (hp > 0) {
			warZone.put(worldCoord, hp);
			String healthChangeStringDef, healthChangeStringAtk;
			if (healthChange > 0) { 
				healthChangeStringDef = "(" + Colors.LightGreen + "+" + healthChange + Colors.Gray + ")";
				healthChangeStringAtk = "(" + Colors.Red + "+" + healthChange + Colors.Gray + ")";
			}
			else if (healthChange < 0) {
				healthChangeStringDef = "(" + Colors.Red + healthChange + Colors.Gray + ")";
				healthChangeStringAtk = "(" + Colors.LightGreen + healthChange + Colors.Gray + ")";
			}
			else {
				healthChangeStringDef = "(+0)";
				healthChangeStringAtk = "(+0)";
			}
			if (!townBlock.isHomeBlock()){
				TownyMessaging.sendMessageToMode(townBlock.getTown(), Colors.Gray + TownySettings.getLangString("msg_war_town_under_attack")+ " (" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
				if ((hp >= 10 && hp % 10 == 0) || hp <= 5){
					launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL_LARGE, fwc);
					for (Town town: townBlock.getTown().getNation().getTowns())
						if (town != townBlock.getTown())
							TownyMessaging.sendMessageToMode(town, Colors.Gray + TownySettings.getLangString("msg_war_nation_under_attack") + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
					for (Nation nation: townBlock.getTown().getNation().getAllies())
						if (nation != townBlock.getTown().getNation())
							TownyMessaging.sendMessageToMode(nation , Colors.Gray + String.format(TownySettings.getLangString("msg_war_nations_ally_under_attack"), townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
				}
				else
					launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL, fwc);
				for (Town attackingTown : wzd.getAttackerTowns())
					TownyMessaging.sendMessageToMode(attackingTown, Colors.Gray + "[" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringAtk, "");
			} else {
				TownyMessaging.sendMessageToMode(townBlock.getTown(), Colors.Gray + TownySettings.getLangString("msg_war_homeblock_under_attack")+" (" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
				if ((hp >= 10 && hp % 10 == 0) || hp <= 5){
					launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL_LARGE, fwc);
					for (Town town: townBlock.getTown().getNation().getTowns())
						if (town != townBlock.getTown())
							TownyMessaging.sendMessageToMode(town, Colors.Gray + String.format(TownySettings.getLangString("msg_war_nation_member_homeblock_under_attack"), townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
					for (Nation nation: townBlock.getTown().getNation().getAllies())
						if (nation != townBlock.getTown().getNation())
							TownyMessaging.sendMessageToMode(nation , Colors.Gray + String.format(TownySettings.getLangString("msg_war_nation_ally_homeblock_under_attack"), townBlock.getTown().getName()) + " [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringDef, "");
				}
				else
					launchFireworkAtPlot (townBlock, attackerPlayer, Type.BALL, fwc);
				for (Town attackingTown : wzd.getAttackerTowns())
					TownyMessaging.sendMessageToMode(attackingTown, Colors.Gray + "[" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp + " " + healthChangeStringAtk, "");
			}
		} else {
			launchFireworkAtPlot (townBlock, attackerPlayer, Type.CREEPER, fwc);
			remove(attacker, townBlock);
		}

		//Call PlotAttackedEvent to update scoreboard users
		PlotAttackedEvent event = new PlotAttackedEvent(townBlock, wzd.getAllPlayers(), hp);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

	/**
	 * Correctly returns the health of a TownBlock given the change in the health.
	 * @param townBlock
	 * @param healthChange
	 * @return
	 */
	private int getHealth(TownBlock townBlock, int healthChange) {
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int hp = warZone.get(worldCoord) + healthChange;
		boolean isHomeBlock = townBlock.isHomeBlock();
		if (isHomeBlock && hp > TownySettings.getWarzoneHomeBlockHealth())
			return TownySettings.getWarzoneHomeBlockHealth();
		else if (!isHomeBlock && hp > TownySettings.getWarzoneTownBlockHealth())
			return TownySettings.getWarzoneTownBlockHealth();
		return hp;
	}

	/**
	 * Launch a firework at a given plot
	 * @param townblock
	 * @param atPlayer
	 * @param type
	 * @param c
	 */
	private void launchFireworkAtPlot(final TownBlock townblock, final Player atPlayer, final FireworkEffect.Type type, final Color c)
	{
		// Check the config. If false, do not launch a firework.
		if (!TownySettings.getPlotsFireworkOnAttacked()) {
			return;
		}
		
		BukkitTools.scheduleSyncDelayedTask(new Runnable() { 

			public void run() {
				double x = (double)townblock.getX() * Coord.getCellSize() + Coord.getCellSize()/2.0;
				double z = (double)townblock.getZ() * Coord.getCellSize() + Coord.getCellSize()/2.0;
				double y = atPlayer.getLocation().getY() + 20;
				Firework firework = atPlayer.getWorld().spawn(new Location(atPlayer.getWorld(), x, y, z), Firework.class);
				FireworkMeta data = (FireworkMeta) firework.getFireworkMeta();
				data.addEffects(FireworkEffect.builder().withColor(c).with(type).trail(false).build());
				firework.setFireworkMeta(data);            
				firework.detonate();
			}
		}, 0);	
	}

	private void remove(Town attacker, TownBlock townBlock) throws NotRegisteredException {

		// Add bonus blocks
		if (TownySettings.getWarEventCostsTownblocks()){		
			townBlock.getTown().addBonusBlocks(-1);
			attacker.addBonusBlocks(1);
		}
		
		try {
			// Check for money loss in the defending town
			if (!townBlock.getTown().payTo(TownySettings.getWartimeTownBlockLossPrice(), attacker, "War - TownBlock Loss")) {
				TownyMessaging.sendTownMessage(townBlock.getTown(), TownySettings.getLangString("msg_war_town_ran_out_of_money"));
				TownyMessaging.sendTitleMessageToTown(townBlock.getTown(), TownySettings.getLangString("msg_war_town_removed_from_war_titlemsg"), "");
				if (townBlock.getTown().isCapital())
					remove(attacker, townBlock.getTown().getNation());
				else
					remove(attacker, townBlock.getTown());
				TownyUniverse.getDataSource().saveTown(townBlock.getTown());
				TownyUniverse.getDataSource().saveTown(attacker);
				return;
			} else
				TownyMessaging.sendTownMessage(townBlock.getTown(), String.format(TownySettings.getLangString("msg_war_town_lost_money_townblock"), TownyEconomyHandler.getFormattedBalance(TownySettings.getWartimeTownBlockLossPrice())));
		} catch (EconomyException e) {}
		
		// Check to see if this is a special TownBlock
		if (townBlock.getTown().isHomeBlock(townBlock) && townBlock.getTown().isCapital()){
			remove(attacker, townBlock.getTown().getNation());
		} else if (townBlock.getTown().isHomeBlock(townBlock)){
			remove(attacker, townBlock.getTown());
		} else{
			townScored(attacker, TownySettings.getWarPointsForTownBlock(), townBlock, 0);
			remove(townBlock.getWorldCoord());
			// Free players who are jailed in the jail plot.
			if (townBlock.getType().equals(TownBlockType.JAIL)){
				Town town = townBlock.getTown();				
				int count = 0;
				for (Resident resident : TownyUniverse.getDataSource().getResidents()){
					try {						
						if (resident.isJailed())
							if (resident.getJailTown().equals(town.toString())) 
								if (Coord.parseCoord(town.getJailSpawn(resident.getJailSpawn())).toString().equals(townBlock.getCoord().toString())){
									resident.setJailed(false);
									TownyUniverse.getDataSource().saveResident(resident);
									count++;
								}
					} catch (TownyException e) {
					}
				}
				if (count>0)
					TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_war_jailbreak"), town, count));
			}				
		}
		TownyUniverse.getDataSource().saveTown(townBlock.getTown());
		TownyUniverse.getDataSource().saveTown(attacker);
	}

	public void remove(Town attacker, Nation nation) throws NotRegisteredException {

		townScored(attacker, TownySettings.getWarPointsForNation(), nation, 0);
		warringNations.remove(nation);
		for (Town town : nation.getTowns())
			if (warringTowns.contains(town))
				remove(attacker, town);
		checkEnd();
	}

	public void remove(Town attacker, Town town) throws NotRegisteredException {

		int fallenTownBlocks = 0;
		warringTowns.remove(town);
		for (TownBlock townBlock : town.getTownBlocks())
			if (warZone.containsKey(townBlock.getWorldCoord())){
				fallenTownBlocks++;
				remove(townBlock.getWorldCoord());
			}
		townScored(attacker, TownySettings.getWarPointsForTown(), town, fallenTownBlocks);
	}
	
	private void remove(Nation nation) {

		warringNations.remove(nation);
		sendEliminateMessage(nation.getFormattedName());
		TownyMessaging.sendTitleMessageToNation(nation, TownySettings.getLangString("msg_war_nation_removed_from_war_titlemsg"), "");
		for (Town town : nation.getTowns())
			remove(town);
		checkEnd();
	}

	public void remove(Town town) {

		// If a town is removed, is a capital, and the nation has not been removed, call remove(nation) instead.
		try {
			if (town.isCapital() && warringNations.contains(town.getNation())) {
				remove(town.getNation());
				return;
			}
		} catch (NotRegisteredException e) {}
		
		int fallenTownBlocks = 0;
		warringTowns.remove(town);
		for (TownBlock townBlock : town.getTownBlocks())
			if (warZone.containsKey(townBlock.getWorldCoord())){
				fallenTownBlocks++;
				remove(townBlock.getWorldCoord());
			}
		StringBuilder sb = new StringBuilder(town.getFormattedName()).append(" (").append(fallenTownBlocks).append(TownySettings.getLangString("msg_war_append_townblocks_fallen"));
		sendEliminateMessage(sb.toString());
	}
	
	private void remove(WorldCoord worldCoord) {	
		warZone.remove(worldCoord);
	}
	
	private void sendEliminateMessage(String name) {
		TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeEliminatedMsg(name));
	}
	
	public void nationLeave(Nation nation) {

		remove(nation);
		TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeForfeitMsg(nation.getName()));
		checkEnd();
	}

	public void townLeave(Town town) {

		remove(town);
		TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeForfeitMsg(town.getName()));
		checkEnd();
	}

	public boolean townsLeft(Nation nation) {

		return countActiveTowns(nation) > 0;
	}

	public void checkEnd() {

		if (warringNations.size() <= 1)
			toggleEnd();
		else if (CombatUtil.areAllAllies(warringNations))
			toggleEnd();
	}

	public int countActiveWarBlocks(Town town) {

		int n = 0;
		for (TownBlock townBlock : town.getTownBlocks())
			if (warZone.containsKey(townBlock.getWorldCoord()))
				n++;
		return n;
	}

	public int countActiveTowns(Nation nation) {

		int n = 0;
		for (Town town : nation.getTowns())
			if (warringTowns.contains(town))
				n++;
		return n;
	}
	
	//// CALCULATE STATS ////

	public List<String> getStats() {

		List<String> output = new ArrayList<String>();
		output.add(ChatTools.formatTitle("War Stats"));
		output.add(Colors.Green + "  Nations: " + Colors.LightGreen + warringNations.size());
		output.add(Colors.Green + "  Towns: " + Colors.LightGreen + warringTowns.size() + " / " + townScores.size());
		output.add(Colors.Green + "  WarZone: " + Colors.LightGreen + warZone.size() + " Town blocks");
		try {
			output.add(Colors.Green + "  Spoils of War: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(warSpoils.getHoldingBalance()));
			return output;
		} catch (EconomyException e) {
		}
		return null;
	}
	
	public void sendStats(Player player) {

		for (String line : getStats())
			player.sendMessage(line);
	}

	//// SCORE CALCULATIONS ////
	
	/**
	 * 
	 * @param maxListing Maximum lines to return. Value of -1 return all.
	 * @return A list of the current scores per town sorted in descending order.
	 */
	public List<String> getScores(int maxListing) {

		List<String> output = new ArrayList<String>();
		output.add(ChatTools.formatTitle("War - Top Scores"));
		KeyValueTable<Town, Integer> kvTable = new KeyValueTable<Town, Integer>(townScores);
		kvTable.sortByValue();
		kvTable.reverse();
		int n = 0;
		for (KeyValue<Town, Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			Town town = (Town) kv.key;
			int score = (Integer) kv.value;
			if (score > 0)
				output.add(String.format(Colors.Blue + "%40s " + Colors.Gold + "|" + Colors.LightGray + " %4d", TownyFormatter.getFormattedName(town), score));
		}
		return output;
	}

	public String[] getTopThree() {
		KeyValueTable<Town, Integer> kvTable = new KeyValueTable<Town, Integer>(townScores);
		kvTable.sortByValue();
		kvTable.reverse();
		String[] top = new String[3];
		top[0] = kvTable.getKeyValues().size() >= 1 ? kvTable.getKeyValues().get(0).value + "-" + kvTable.getKeyValues().get(0).key : "";
		top[1] = kvTable.getKeyValues().size() >= 2 ? kvTable.getKeyValues().get(1).value + "-" + kvTable.getKeyValues().get(1).key : "";
		top[2] = kvTable.getKeyValues().size() >= 3 ? kvTable.getKeyValues().get(2).value + "-" + kvTable.getKeyValues().get(2).key : "";
		return top;
	}

	public KeyValue<Town, Integer> getWinningTownScore() throws TownyException {

		KeyValueTable<Town, Integer> kvTable = new KeyValueTable<Town, Integer>(townScores);
		kvTable.sortByValue();
		kvTable.reverse();
		if (kvTable.getKeyValues().size() > 0)
			return kvTable.getKeyValues().get(0);
		else
			throw new TownyException();
	}
	
	public void sendScores(Player player) {

		sendScores(player, 10);
	}

	public void sendScores(Player player, int maxListing) {

		for (String line : getScores(maxListing))
			player.sendMessage(line);
	}
	
}