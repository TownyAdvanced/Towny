package com.palmergames.bukkit.towny.war.eventwar;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;

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

//TODO: Extend a new class called TownyEvent
public class War {

	private Hashtable<WorldCoord, Integer> warZone = new Hashtable<WorldCoord, Integer>();
	private Hashtable<Town, Integer> townScores = new Hashtable<Town, Integer>();
	private List<Town> warringTowns = new ArrayList<Town>();
	private List<Nation> warringNations = new ArrayList<Nation>();
	private Towny plugin;
	private TownyUniverse universe;
	private boolean warTime = false;
	//private Timer warTimer = new Timer();
	private List<Integer> warTaskIds = new ArrayList<Integer>();
	private WarSpoils warSpoils = new WarSpoils();

	public War(Towny plugin, int startDelay) {

		this.plugin = plugin;
		this.universe = plugin.getTownyUniverse();

		setupDelay(startDelay);
	}

	/*
	 * public void setWarTimer(Timer warTimer) {
	 * this.warTimer = warTimer;
	 * }
	 * 
	 * public Timer getWarTimer() {
	 * return warTimer;
	 * }
	 */

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

	public List<Integer> getTaskIds() {

		return new ArrayList<Integer>(warTaskIds);
	}

	public Towny getPlugin() {

		return plugin;
	}

	public void setPlugin(Towny plugin) {

		this.plugin = plugin;
	}

	public void setupDelay(int delay) {

		if (delay <= 0)
			start();
		else {
			for (Long t : TimeMgmt.getCountdownDelays(delay, TimeMgmt.defaultCountdownDelays)) {
				//Schedule the warnings leading up to the start of the war event
				//warTimer.schedule(
				//              new ServerBroadCastTimerTask(plugin,
				//                              String.format("War starts in %s", TimeMgmt.formatCountdownTime(t))),
				//                              (delay-t)*1000);
				int id = BukkitTools.scheduleAsyncDelayedTask(new ServerBroadCastTimerTask(plugin, String.format("War starts in %s", TimeMgmt.formatCountdownTime(t))), TimeTools.convertToTicks((delay - t)));
				if (id == -1) {
					TownyMessaging.sendErrorMsg("Could not schedule a countdown message for war event.");
					end();
				} else
					addTaskId(id);
			}
			//warTimer.schedule(new StartWarTimerTask(universe), delay*1000);
			int id = BukkitTools.scheduleAsyncDelayedTask(new StartWarTimerTask(plugin), TimeTools.convertToTicks(delay));
			if (id == -1) {
				TownyMessaging.sendErrorMsg("Could not schedule setup delay for war event.");
				end();
			} else
				addTaskId(id);
		}
	}

	public boolean isWarTime() {

		return warTime;
	}

	public TownyUniverse getTownyUniverse() {

		return universe;
	}

	public void start() {

		warTime = true;

		//Announce

		// Seed spoils of war
		try {
			warSpoils.pay(TownySettings.getBaseSpoilsOfWar(), "Start of War - Base Spoils");
			TownyMessaging.sendMsg("[War] Seeding spoils of war with " + TownySettings.getBaseSpoilsOfWar());
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		//warTimer.scheduleAtFixedRate(new WarTimerTask(this), 0, 1000);
		int id = BukkitTools.scheduleAsyncRepeatingTask(new WarTimerTask(plugin, this), 0, TimeTools.convertToTicks(5));
		if (id == -1) {
			TownyMessaging.sendErrorMsg("Could not schedule war event loop.");
			end();
		} else
			addTaskId(id);
		checkEnd();
	}

	public void end() {

		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				sendStats(player);
		plugin.getHUDManager().toggleAllWarHUD();
		double halfWinnings;
		try {
			// Transactions might leave 1 coin. (OH noez!)
			halfWinnings = getWarSpoils().getHoldingBalance() / 2.0;

			try {
				double nationWinnings = halfWinnings / warringNations.size(); // Again, might leave residue.
				for (Nation winningNation : warringNations) {
					getWarSpoils().payTo(nationWinnings, winningNation, "War - Nation Winnings");
					TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeWinningNationSpoilsMsg(winningNation, TownyEconomyHandler.getFormattedBalance(nationWinnings)));
				}
			} catch (ArithmeticException e) {
				// A war ended with 0 nations.
			}

			try {
				KeyValue<Town, Integer> winningTownScore = getWinningTownScore();
				getWarSpoils().payTo(halfWinnings, winningTownScore.key, "War - Town Winnings");
				TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeWinningTownSpoilsMsg(winningTownScore.key, TownyEconomyHandler.getFormattedBalance(halfWinnings), winningTownScore.value));
			} catch (TownyException e) {
			}
		} catch (EconomyException e1) {
		}

	}

	public void add(Nation nation) {

		for (Town town : nation.getTowns())
			add(town);
		warringNations.add(nation);
	}

	public void add(Town town) {

		TownyMessaging.sendTownMessage(town, TownySettings.getJoinWarMsg(town));
		townScores.put(town, 0);
		warringTowns.add(town);
		for (TownBlock townBlock : town.getTownBlocks())
			if (town.isHomeBlock(townBlock))
				warZone.put(townBlock.getWorldCoord(), TownySettings.getWarzoneHomeBlockHealth());
			else
				warZone.put(townBlock.getWorldCoord(), TownySettings.getWarzoneTownBlockHealth());
	}

	public boolean isWarZone(WorldCoord worldCoord) {

		return warZone.containsKey(worldCoord);
	}

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

	public void damage(Player attackerPlayer, TownBlock townBlock) throws NotRegisteredException {

		Resident attackerResident = TownyUniverse.getDataSource().getResident(attackerPlayer.getName());
		Town attacker = attackerResident.getTown();
		WorldCoord worldCoord = townBlock.getWorldCoord();
		int hp = warZone.get(worldCoord) - 1;
		if (hp > 0) {
			warZone.put(worldCoord, hp);
			//TownyMessaging.sendMessageToMode(townBlock.getTown(), Colors.Gray + "[" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp, "");
			TownyMessaging.sendMessageToMode(townBlock.getTown(), Colors.Red + "Your town is under attack! (" + townBlock.getCoord().toString() + ") HP: " + hp, "");
			if ((hp >= 10 && hp % 10 == 0) || hp <= 5){
				launchFireworkForDamage (townBlock, attackerPlayer, Type.BALL_LARGE);
				for (Town town: townBlock.getTown().getNation().getTowns())
					if (town != townBlock.getTown())
						TownyMessaging.sendMessageToMode(town, Colors.Red + "Your nation is under attack! [" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp, "");
			}
			else
				launchFireworkForDamage (townBlock, attackerPlayer, Type.BALL);
			TownyMessaging.sendMessageToMode(attacker, Colors.Gray + "[" + townBlock.getTown().getName() + "](" + townBlock.getCoord().toString() + ") HP: " + hp, "");
		} else {
			launchFireworkForDamage (townBlock, attackerPlayer, Type.CREEPER);
			remove(attacker, townBlock);
		}
		//Call PlotAttackedEvent to update scoreboard users
		PlotAttackedEvent event = new PlotAttackedEvent(hp, townBlock);
		Bukkit.getServer().getPluginManager().callEvent(event);
	}

	private void launchFireworkForDamage(final TownBlock townblock, final Player attacker, final FireworkEffect.Type type)
	{
		BukkitTools.scheduleSyncDelayedTask(new Runnable() { 

			public void run() {
				double x = (double)townblock.getX() * Coord.getCellSize() + Coord.getCellSize()/2.0;
				double z = (double)townblock.getZ() * Coord.getCellSize() + Coord.getCellSize()/2.0;
				double y = attacker.getLocation().getY() + 20;
				Firework firework = attacker.getWorld().spawn(new Location(attacker.getWorld(), x, y, z), Firework.class);
				FireworkMeta data = (FireworkMeta) firework.getFireworkMeta();
				data.addEffects(FireworkEffect.builder().withColor(Color.RED).with(type).trail(false).withFade(Color.MAROON).build());
				firework.setFireworkMeta(data);            
				firework.detonate();
			}
		}, 0);	
	}

	public void remove(Town attacker, TownBlock townBlock) throws NotRegisteredException {

		//townScored(attacker, TownySettings.getWarPointsForTownBlock());
		townBlock.getTown().addBonusBlocks(-1);
		attacker.addBonusBlocks(1);
		try {
			if (!townBlock.getTown().payTo(TownySettings.getWartimeTownBlockLossPrice(), attacker, "War - TownBlock Loss")) {
				TownyMessaging.sendTownMessage(townBlock.getTown(), "Your town ran out of funds to support yourself in war.");
				if (townBlock.getTown().isCapital())
					remove(attacker, townBlock.getTown().getNation());
				else
					remove(attacker, townBlock.getTown());
				TownyUniverse.getDataSource().saveTown(townBlock.getTown());
				TownyUniverse.getDataSource().saveTown(attacker);
				return;
			} else
				TownyMessaging.sendTownMessage(townBlock.getTown(), "Your town lost " + TownyEconomyHandler.getFormattedBalance(TownySettings.getWartimeTownBlockLossPrice()) + ".");
		} catch (EconomyException e) {
		}
		if (townBlock.getTown().isHomeBlock(townBlock) && townBlock.getTown().isCapital()){
			remove(attacker, townBlock.getTown().getNation());
		} else if (townBlock.getTown().isHomeBlock(townBlock)){
			remove(attacker, townBlock.getTown());
		} else{
			townScored(attacker, TownySettings.getWarPointsForTownBlock(), townBlock, 0);
			remove(townBlock.getWorldCoord());
		}
		TownyUniverse.getDataSource().saveTown(townBlock.getTown());
		TownyUniverse.getDataSource().saveTown(attacker);
	}

	public void remove(TownBlock townBlock) throws NotRegisteredException {

		if (townBlock.getTown().isHomeBlock(townBlock))
			remove(townBlock.getTown());
		else
			remove(townBlock.getWorldCoord());
	}

	public void eliminate(Town town) {

		//remove(town);
		//		try {
		//			checkNation(town.getNation());
		//		} catch (NotRegisteredException e) {
		//			TownyMessaging.sendErrorMsg("[War] Error checking " + town.getName() + "'s nation.");
		//		}
		TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeEliminatedMsg(town.getName()));
		//checkEnd();
	}

	public void eliminate(Town town, String townBlocksFallen) {

		//remove(town);
		//		try {
		//			checkNation(town.getNation());
		//		} catch (NotRegisteredException e) {
		//			TownyMessaging.sendErrorMsg("[War] Error checking " + town.getName() + "'s nation.");
		//		}
		String[] message = TownySettings.getWarTimeEliminatedMsg(town.getFormattedName() + " " + townBlocksFallen);
		TownyMessaging.sendGlobalMessage(message);
		//checkEnd();
	}

	public void eliminate(Nation nation) {

		//remove(nation);
		TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeEliminatedMsg(nation.getFormattedName()));
		//checkEnd();
	}

	public void nationLeave(Nation nation) {

		remove(nation);
		for (Town town : nation.getTowns())
			remove(town);
		TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeForfeitMsg(nation.getName()));
		checkEnd();
	}

	public void townLeave(Town town) {

		remove(town);
		TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeForfeitMsg(town.getName()));
		checkEnd();
	}

	public void remove(Town attacker, Nation nation) throws NotRegisteredException {

		townScored(attacker, TownySettings.getWarPointsForNation(), nation, 0);
		warringNations.remove(nation);
		for (Town town : nation.getTowns())
			if (warringTowns.contains(town))
				remove(attacker, town);
		checkEnd();
	}

	public void remove(Nation nation) {

		warringNations.remove(nation);
		eliminate(nation);
		for (Town town : nation.getTowns())
			remove(town);
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

	public void remove(Town town) {

		int fallenTownBlocks = 0;
		warringTowns.remove(town);
		for (TownBlock townBlock : town.getTownBlocks())
			if (warZone.containsKey(townBlock.getWorldCoord())){
				fallenTownBlocks++;
				remove(townBlock.getWorldCoord());
			}
		String format = "(" + fallenTownBlocks + " town blocks captured)";
		eliminate (town, format);

		//		try {
		//			if (!townsLeft(town.getNation()))
		//				eliminate(town.getNation());
		//		} catch (NotRegisteredException e) {
		//		}
	}

	public boolean townsLeft(Nation nation) {

		return countActiveTowns(nation) > 0;
	}

	public void remove(WorldCoord worldCoord) {	
		warZone.remove(worldCoord);
	}

	public void checkEnd() {

		if (warringNations.size() <= 1)
			toggleEnd();
		else if (CombatUtil.areAllAllies(warringNations))
			toggleEnd();
	}

	public void checkTown(Town town) {

		if (countActiveWarBlocks(town) == 0)
			eliminate(town);
	}

	public void checkNation(Nation nation) {

		if (countActiveTowns(nation) == 0)
			eliminate(nation);
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

	public void toggleEnd() {

		warTime = false;
	}

	public void sendStats(Player player) {

		for (String line : getStats())
			player.sendMessage(line);
	}

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

	public void sendScores(Player player) {

		sendScores(player, 10);
	}

	public void sendScores(Player player, int maxListing) {

		for (String line : getScores(maxListing))
			player.sendMessage(line);
	}

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
		kvTable.revese();
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
		kvTable.revese();
		String[] top = new String[3];
		top[0] = kvTable.getKeyValues().size() >= 1 ? kvTable.getKeyValues().get(0).value + "-" + kvTable.getKeyValues().get(0).key : "";
		top[1] = kvTable.getKeyValues().size() >= 2 ? kvTable.getKeyValues().get(1).value + "-" + kvTable.getKeyValues().get(1).key : "";
		top[2] = kvTable.getKeyValues().size() >= 3 ? kvTable.getKeyValues().get(2).value + "-" + kvTable.getKeyValues().get(2).key : "";
		return top;
	}

	public boolean isWarringNation(Nation nation) {

		return warringNations.contains(nation);
	}

	public boolean isWarringTown(Town town) {

		return warringTowns.contains(town);
	}

	public KeyValue<Town, Integer> getWinningTownScore() throws TownyException {

		KeyValueTable<Town, Integer> kvTable = new KeyValueTable<Town, Integer>(townScores);
		kvTable.sortByValue();
		kvTable.revese();
		if (kvTable.getKeyValues().size() > 0)
			return kvTable.getKeyValues().get(0);
		else
			throw new TownyException();
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
}