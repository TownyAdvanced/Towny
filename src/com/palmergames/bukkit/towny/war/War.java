package com.palmergames.bukkit.towny.war;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import com.palmergames.bukkit.towny.EconomyException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.MinecraftTools;
import com.palmergames.bukkit.util.ServerBroadCastTimerTask;
import com.palmergames.util.KeyValue;
import com.palmergames.util.KeyValueTable;
import com.palmergames.util.TimeMgmt;


//TODO: Extend a new class called TownyEvent
public class War {
        private Hashtable<WorldCoord,Integer> warZone = new Hashtable<WorldCoord,Integer>(); 
        private Hashtable<Town,Integer> townScores = new Hashtable<Town,Integer>();
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
        public void setWarTimer(Timer warTimer) {
                this.warTimer = warTimer;
        }

        public Timer getWarTimer() {
                return warTimer;
        }
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
                                int id = plugin.getServer().getScheduler().scheduleAsyncDelayedTask(getPlugin(),
                                                new ServerBroadCastTimerTask(plugin, String.format("War starts in %s", TimeMgmt.formatCountdownTime(t))),
                                                MinecraftTools.convertToTicks((delay-t)));
                                if (id == -1) {
                                	TownyMessaging.sendErrorMsg("Could not schedule a countdown message for war event.");
                                    end();
                                } else
                                        addTaskId(id);
                        }
                        //warTimer.schedule(new StartWarTimerTask(universe), delay*1000);
                        int id = plugin.getServer().getScheduler().scheduleAsyncDelayedTask(getPlugin(), new StartWarTimerTask(universe), MinecraftTools.convertToTicks(delay));
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
                int id = plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(getPlugin(), new WarTimerTask(this), 0, MinecraftTools.convertToTicks(5));
                if (id == -1) {
                	TownyMessaging.sendErrorMsg("Could not schedule war event loop.");
                        end();
                } else
                        addTaskId(id);
                checkEnd();
        }
        
        public void end() {
                for (Player player : TownyUniverse.getOnlinePlayers())
                        sendStats(player);
                double halfWinnings;
                try {
                        // Transactions might leave 1 coin. (OH noez!)
                        halfWinnings = getWarSpoils().getHoldingBalance() / 2.0;
                                
                        try {
                                double nationWinnings = halfWinnings / warringNations.size(); // Again, might leave residue.
                                for (Nation winningNation : warringNations) {
                                        getWarSpoils().payTo(nationWinnings, winningNation, "War - Nation Winnings");
                                        TownyMessaging.sendGlobalMessage(winningNation.getName() + " won " + nationWinnings + " " + TownyEconomyObject.getEconomyCurrency() + ".");
                                }
                        } catch (ArithmeticException e) {
                                // A war ended with 0 nations.
                        }
                        
                        try {
                                KeyValue<Town,Integer> winningTownScore = getWinningTownScore();
                                getWarSpoils().payTo(halfWinnings, winningTownScore.key, "War - Nation Winnings");
                                TownyMessaging.sendGlobalMessage(winningTownScore.key.getName() + " won " + halfWinnings + " " + TownyEconomyObject.getEconomyCurrency() + " with the score " + winningTownScore.value + ".");
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

        public void townScored(Town town, int n) {
                townScores.put(town, townScores.get(town) + n);
                TownyMessaging.sendTownMessage(town, TownySettings.getWarTimeScoreMsg(town, n));
        }
        
        public void damage(Town attacker, TownBlock townBlock) throws NotRegisteredException {
                WorldCoord worldCoord = townBlock.getWorldCoord();
                int hp = warZone.get(worldCoord) - 1;
                if (hp > 0) {
                        warZone.put(worldCoord, hp);
                        //if (hp % 10 == 0) {
                                universe.sendMessageTo(townBlock.getTown(),
                                                Colors.Gray + "["+townBlock.getTown().getName()+"]("+townBlock.getCoord().toString()+") HP: "+hp,
                                                "");
                                universe.sendMessageTo(attacker,
                                                Colors.Gray + "["+townBlock.getTown().getName()+"]("+townBlock.getCoord().toString()+") HP: "+hp,
                                                "");
                        //}
                } else
                        remove(attacker, townBlock);
        }
        
        public void remove(Town attacker, TownBlock townBlock) throws NotRegisteredException {
                townScored(attacker, TownySettings.getWarPointsForTownBlock());
                townBlock.getTown().addBonusBlocks(-1);
                attacker.addBonusBlocks(1);
                try {
                        if (!townBlock.getTown().payTo(TownySettings.getWartimeTownBlockLossPrice(), attacker, "War - TownBlock Loss")) {
                                remove(townBlock.getTown());
                                TownyMessaging.sendTownMessage(townBlock.getTown(), "Your town ran out of funds to support yourself in war.");
                        } else
                                TownyMessaging.sendTownMessage(townBlock.getTown(), "Your town lost "+TownySettings.getWartimeTownBlockLossPrice()+" "+TownyEconomyObject.getEconomyCurrency()+".");
                } catch (EconomyException e) {
                }
                if (townBlock.getTown().isHomeBlock(townBlock))
                        remove(townBlock.getTown());
                else
                        remove(townBlock.getWorldCoord());
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
                remove(town);
                try {
                    checkNation(town.getNation());
                } catch (NotRegisteredException e) {
                	TownyMessaging.sendErrorMsg("[War] Error checking "+town.getName()+"'s nation.");
                }
                TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeEliminatedMsg(town.getName()));
                checkEnd();
        }
        
        public void eliminate(Nation nation) {
                remove(nation);
                TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeEliminatedMsg(nation.getName()));
                checkEnd();
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
        
        public void remove(Town attacker, Nation nation) {
                townScored(attacker, TownySettings.getWarPointsForNation());
                warringNations.remove(nation);
        }
        
        public void remove(Nation nation) {
                warringNations.remove(nation);
        }
        
        
        public void remove(Town attacker, Town town) throws NotRegisteredException {
                townScored(attacker, TownySettings.getWarPointsForTown());
                
                for (TownBlock townBlock : town.getTownBlocks())
                        remove(townBlock.getWorldCoord());
                warringTowns.remove(town);
                try {
                        if (!townsLeft(town.getNation()))
                                eliminate(town.getNation());
                } catch (NotRegisteredException e) {
                }
        }
        
        public void remove(Town town) {
                for (TownBlock townBlock : town.getTownBlocks())
                        remove(townBlock.getWorldCoord());
                warringTowns.remove(town);
                try {
                        if (!townsLeft(town.getNation()))
                                eliminate(town.getNation());
                } catch (NotRegisteredException e) {
                }
        }
        
        public boolean townsLeft(Nation nation) {
                return warringTowns.containsAll(nation.getTowns());
        }
        
        public void remove(WorldCoord worldCoord) {
                try {
                        Town town = worldCoord.getTownBlock().getTown();
                        TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeLoseTownBlockMsg(worldCoord, town.getName()));
                        warZone.remove(worldCoord);
                } catch (NotRegisteredException e) {
                	TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeLoseTownBlockMsg(worldCoord));
                        warZone.remove(worldCoord);
                }
                
        }
        
        public void checkEnd() {
                if (warringNations.size() <= 1)
                        toggleEnd();
                else if (plugin.getTownyUniverse().areAllAllies(warringNations))
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
                output.add(Colors.Green + "  Towns: " + Colors.LightGreen + warringTowns.size() +" / " + townScores.size());
                output.add(Colors.Green + "  WarZone: " + Colors.LightGreen + warZone.size() + " Town blocks");
                try{
        output.add(Colors.Green + "  Spoils of War: " + Colors.LightGreen + warSpoils.getHoldingBalance() + " " + TownyEconomyObject.getEconomyCurrency());
        return output;
                }
                catch(EconomyException e)
                {
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
                KeyValueTable<Town,Integer> kvTable = new KeyValueTable<Town,Integer>(townScores);
                kvTable.sortByValue();
                kvTable.revese();
                int n = 0;
                for (KeyValue<Town,Integer> kv : kvTable.getKeyValues()) {
                        n++;
                        if (maxListing != -1 && n > maxListing)
                                break;
                        Town town = (Town)kv.key;
                        output.add(String.format(
                                        Colors.Blue + "%40s "+Colors.Gold+"|"+Colors.LightGray+" %4d",
                                        TownyFormatter.getFormattedName(town),
                                        (Integer)kv.value));
                }
                return output;
        }
        
        public boolean isWarringNation(Nation nation) {
                return warringNations.contains(nation);
        }
        
        public KeyValue<Town,Integer> getWinningTownScore() throws TownyException {
                KeyValueTable<Town,Integer> kvTable = new KeyValueTable<Town,Integer>(townScores);
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
}