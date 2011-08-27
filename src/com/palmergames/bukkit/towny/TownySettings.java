package com.palmergames.bukkit.towny;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyPermission.PermLevel;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.util.TimeTools;
import com.palmergames.util.FileMgmt;
//import com.palmergames.util.StringMgmt;

import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;


public class TownySettings {
        
        // Nation Level
        public enum NationLevel {
                NAME_PREFIX,
                NAME_POSTFIX,
                CAPITAL_PREFIX,
                CAPITAL_POSTFIX,
                KING_PREFIX,
                KING_POSTFIX,
                UPKEEP_MULTIPLIER
        };
        // Town Level
        public enum TownLevel {
                NAME_PREFIX,
                NAME_POSTFIX,
                MAYOR_PREFIX,
                MAYOR_POSTFIX,
                TOWN_BLOCK_LIMIT,
                UPKEEP_MULTIPLIER
        };
        
        
        private static Pattern namePattern = null;      
        private static CommentedConfiguration config, newConfig;
        private static Configuration language;
    //private static Configuration permissions;
        
        private static final SortedMap<Integer,Map<TownySettings.TownLevel,Object>> configTownLevel = 
                Collections.synchronizedSortedMap(new TreeMap<Integer,Map<TownySettings.TownLevel,Object>>(Collections.reverseOrder()));
        private static final SortedMap<Integer,Map<TownySettings.NationLevel,Object>> configNationLevel = 
                Collections.synchronizedSortedMap(new TreeMap<Integer,Map<TownySettings.NationLevel,Object>>(Collections.reverseOrder()));
        
        /*
        static {                
                newTownLevel(0, "", " Town", "Mayor ", "", 16);
                newNationLevel(0, "", " Nation", "Capital: ", " City", "King ", "");
        }
        */
        
        public static void newTownLevel(int numResidents,
                        String namePrefix, String namePostfix,
                        String mayorPrefix, String mayorPostfix, 
                        int townBlockLimit, double townUpkeepMultiplier) {
                ConcurrentHashMap<TownySettings.TownLevel,Object> m = new ConcurrentHashMap<TownySettings.TownLevel,Object>();
                m.put(TownySettings.TownLevel.NAME_PREFIX, namePrefix);
                m.put(TownySettings.TownLevel.NAME_POSTFIX, namePostfix);
                m.put(TownySettings.TownLevel.MAYOR_PREFIX, mayorPrefix);
                m.put(TownySettings.TownLevel.MAYOR_POSTFIX, mayorPostfix);
                m.put(TownySettings.TownLevel.TOWN_BLOCK_LIMIT, townBlockLimit);
                m.put(TownySettings.TownLevel.UPKEEP_MULTIPLIER, townUpkeepMultiplier);
                configTownLevel.put(numResidents, m);
        }
        
        public static void newNationLevel(int numResidents, 
                        String namePrefix, String namePostfix, 
                        String capitalPrefix, String capitalPostfix,
                        String kingPrefix, String kingPostfix, double nationUpkeepMultiplier) {
                ConcurrentHashMap<TownySettings.NationLevel,Object> m = new ConcurrentHashMap<TownySettings.NationLevel,Object>();
                m.put(TownySettings.NationLevel.NAME_PREFIX, namePrefix);
                m.put(TownySettings.NationLevel.NAME_POSTFIX, namePostfix);
                m.put(TownySettings.NationLevel.CAPITAL_PREFIX, capitalPrefix);
                m.put(TownySettings.NationLevel.CAPITAL_POSTFIX, capitalPostfix);
                m.put(TownySettings.NationLevel.KING_PREFIX, kingPrefix);
                m.put(TownySettings.NationLevel.KING_POSTFIX, kingPostfix);
                m.put(TownySettings.NationLevel.UPKEEP_MULTIPLIER, nationUpkeepMultiplier);
                configNationLevel.put(numResidents, m);
        }
        
        /**
         * Loads town levels. Level format ignores lines starting with #.
         * Each line is considered a level. Each level is loaded as such:
         * 
         * numResidents:namePrefix:namePostfix:mayorPrefix:mayorPostfix:townBlockLimit
         * 
         * townBlockLimit is a required field even if using a calculated ratio.
         * 
         * @throws IOException 
         */
        
        public static void loadTownLevelConfig() throws IOException {
        ArrayList<ConfigurationNode> levels = new ArrayList<ConfigurationNode>(config.getNodeList("levels.town_level", null));
        for (ConfigurationNode level : levels) {
            newTownLevel(level.getInt("numResidents", 0), level.getString("namePrefix", ""),
                    level.getString("namePostfix", ""), level.getString("mayorPrefix", ""),
                    level.getString("mayorPostfix", ""), level.getInt("townBlockLimit", 1),
                    level.getDouble("upkeepModifier", 1.0));
        }

                /*
                //get an Iterator object for list using iterator() method.
                Iterator<String> itr = lines.iterator();

                //use hasNext() and next() methods of Iterator to iterate through the elements
                while(itr.hasNext()) {
                tokens = itr.next().split(",", 7);
                if (tokens.length >= 7)
                                        try {
                        int numResidents = Integer.parseInt(tokens[0]);
                        int townBlockLimit = Integer.parseInt(tokens[5]);
                        double townUpkeepMult = Double.valueOf(tokens[6]);
                        newTownLevel(numResidents, tokens[1], tokens[2], tokens[3], tokens[4], townBlockLimit, townUpkeepMult);
                                                if (getDebug())
                                                        // Used to know the actual values registered
                                                         System.out.println("[Towny] Debug: Added town level: "+numResidents+" "+Arrays.toString(getTownLevel(numResidents).values().toArray()));
                                                        //System.out.println("[Towny] Debug: Added town level: "+numResidents+" "+Arrays.toString(tokens));
                    } catch (Exception e) {
                        System.out.println("[Towny] Input Error: Town level ignored: " + itr.toString());
                    }
                else
                        System.out.println("[Towny] loadTownLevelConfig bad length");
            //}

                }

                /*
                //BufferedReader fin = new BufferedReader(new FileReader(filepath));
                 
        while ((line = fin.readLine()) != null)
                        if (!line.startsWith("#")) { //Ignore comment lines
                tokens = line.split(",", 6);
                if (tokens.length >= 6)
                                        try {
                        int numResidents = Integer.parseInt(tokens[0]);
                        int townBlockLimit = Integer.parseInt(tokens[5]);
                        newTownLevel(numResidents, tokens[1], tokens[2], tokens[3], tokens[4], townBlockLimit);
                                                if (getDebug())
                                                        // Used to know the actual values registered
                                                         System.out.println("[Towny] Debug: Added town level: "+numResidents+" "+Arrays.toString(getTownLevel(numResidents).values().toArray()));
                                                        //System.out.println("[Towny] Debug: Added town level: "+numResidents+" "+Arrays.toString(tokens));
                    } catch (Exception e) {
                        System.out.println("[Towny] Input Error: Town level ignored: " + line);
                    }
            }
        fin.close();
        */
        }
        
        /**
         * Loads nation levels. Level format ignores lines starting with #.
         * Each line is considered a level. Each level is loaded as such:
         * 
         * numResidents:namePrefix:namePostfix:capitalPrefix:capitalPostfix:kingPrefix:kingPostfix
         * 
         * @throws IOException 
         */

        public static void loadNationLevelConfig() throws IOException {
                ArrayList<ConfigurationNode> levels = new ArrayList<ConfigurationNode>(config.getNodeList("levels.nation_level", null));
        for (ConfigurationNode level : levels) {
            newNationLevel(level.getInt("numResidents", 0), level.getString("namePrefix", ""),
                    level.getString("namePostfix", ""), level.getString("capitalPrefix", ""),
                    level.getString("capitalPostfix", ""), level.getString("kingPrefix", ""),
                    level.getString("kingPostfix", ""), level.getDouble("upkeepModifier", 1.0));
        }

                /*
                //get an Iterator object for list using iterator() method.
                Iterator<String> itr = lines.iterator();
                
                //use hasNext() and next() methods of Iterator to iterate through the elements
                while(itr.hasNext()) {
                        tokens = itr.next().split(",", 8);
            if (tokens.length >= 8)
                                try {
                    int numResidents = Integer.parseInt(tokens[0]);
                    double upkeep = Double.valueOf(tokens[7]);
                    newNationLevel(numResidents, tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6], upkeep);
                                        if (getDebug())
                                                // Used to know the actual values registered
                                                // System.out.println("[Towny] Debug: Added nation level: "+numResidents+" "+Arrays.toString(getNationLevel(numResidents).values().toArray()));
                                                System.out.println("[Towny] Debug: Added nation level: "+numResidents+" "+Arrays.toString(tokens));
                } catch (Exception e) {
                        System.out.println("[Towny] Input Error: Nation level ignored: " + itr.toString());
                }

                        
                }
                
                /*
                String line;
                String[] tokens;
                BufferedReader fin = new BufferedReader(new FileReader(filepath));
        while ((line = fin.readLine()) != null)
                        if (!line.startsWith("#")) { //Ignore comment lines
                tokens = line.split(",", 7);
                if (tokens.length >= 7)
                                        try {
                        int numResidents = Integer.parseInt(tokens[0]);
                        newNationLevel(numResidents, tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6]);
                                                if (getDebug())
                                                        // Used to know the actual values registered
                                                        // System.out.println("[Towny] Debug: Added nation level: "+numResidents+" "+Arrays.toString(getNationLevel(numResidents).values().toArray()));
                                                        System.out.println("[Towny] Debug: Added nation level: "+numResidents+" "+Arrays.toString(tokens));
                    } catch (Exception e) {
                        System.out.println("[Towny] Input Error: Nation level ignored: " + line);
                    }
            }
        fin.close();
        */
        }
        
        public static Map<TownySettings.TownLevel,Object> getTownLevel(int numResidents) {
                return configTownLevel.get(numResidents);
        }
        
        public static Map<TownySettings.NationLevel,Object> getNationLevel(int numResidents) {
                return configNationLevel.get(numResidents);
        }
        
        public static Map<TownySettings.TownLevel,Object> getTownLevel(Town town) {
                return getTownLevel(calcTownLevel(town));
        }
        
        public static Map<TownySettings.NationLevel,Object> getNationLevel(Nation nation) {
                return getNationLevel(calcNationLevel(nation));
        }
        
        //TODO: more efficient way
        public static int calcTownLevel(Town town) {
                int n = town.getNumResidents();
                for (Integer level : configTownLevel.keySet())
                        if (n >= level)
                                return level;
        return 0;
    }
        
        //TODO: more efficient way
        public static int calcNationLevel(Nation nation) {
                int n = nation.getNumResidents();               
                for (Integer level : configNationLevel.keySet())
                        if (n >= level)
                                return level;
        return 0;
    }
        
        //////////////////////////////
        
        public static void loadConfig(String filepath, String version) throws IOException {
                
        File file = FileMgmt.CheckYMLExists(new File(filepath));
                if (file != null) {
                                
                        // read the config.yml into memory
                        config = new CommentedConfiguration(file);
                        config.load();

            setDefaults(version, file);

            config.save();

                        // Load Nation & Town level data into maps.
                        loadTownLevelConfig();
                        loadNationLevelConfig();
                }
                
        }
        
        // This will read the language entry in the config.yml to attempt to load custom languages
        // if the file is not found it will load the default from resource
        public static void loadLanguage (String filepath, String defaultRes) throws IOException {               
                
                String defaultName = filepath + FileMgmt.fileSeparator() + getString("language", defaultRes);
                
                File file = FileMgmt.unpackLanguageFile(defaultName, defaultRes);
                if (file != null) {
                                
                        // read the (language).yml into memory
                        language = new Configuration(file);
                        language.load();
                                
                }
        }

        
        private static void sendError(String msg) {
                
                System.out.println("[Towny] Error could not read " + msg);
                
        }
        
        ///////////////////////////////////
        
        // Functions to pull data from the config and language files

        private static String[] parseString(String str) {
                return parseSingleLineString(str).split("@");
        }
        
        public static String parseSingleLineString(String str) {
                return str.replaceAll("&", "\u00A7");
        }

    public static Boolean getBoolean(ConfigNodes node) {
        return config.getBoolean(node.getRoot().toLowerCase(), Boolean.parseBoolean(node.getDefault()));
    }
    /*
        public static Boolean getBoolean(String root){
        return config.getBoolean(root.toLowerCase(), true);
    }
    */

    private static Double getDouble(ConfigNodes node) {
        return config.getDouble(node.getRoot().toLowerCase(), Double.parseDouble(node.getDefault()));
    }
    /*
    private static Double getDouble(String root){
        return config.getDouble(root.toLowerCase(), 0);
    }
    */

    private static Integer getInt(ConfigNodes node) {
        return config.getInt(node.getRoot().toLowerCase(), Integer.parseInt(node.getDefault()));
    }
    /*
    private static Integer getInt(String root){
        return config.getInt(root.toLowerCase(), 0);
    }
    */

    private static Long getLong(ConfigNodes node) {
        return Long.parseLong(getString(node));
    }
    
    /*
    private static Long getLong(String root){
        return Long.parseLong(getString(root).trim());
    }
    */
    
    /*
     * Public Functions to read data from the Configuration
     * and Language data
     * 
     * 
     */

    public static String getString(ConfigNodes node) {
        return config.getString(node.getRoot().toLowerCase(), node.getDefault());
    }
    
    private static String getString(String root, String def){
        
        String data = config.getString(root.toLowerCase(), def);
        if (data == null) {
                sendError(root.toLowerCase() + " from config.yml");
                return "";
        }
        return data;
    }
    
    public static String getLangString(String root){
        
        String data = language.getString(root.toLowerCase());
        if (data == null) {
                sendError(root.toLowerCase() + " from " + config.getString("language"));
                return "";
        }
        return parseSingleLineString(data);
    }

    public static List<Integer> getIntArr(ConfigNodes node) {
        String[] strArray = getString(node.getRoot().toLowerCase(), node.getDefault()).split(",");
                List<Integer> list = new ArrayList<Integer>();
                if (strArray != null) {
                for (int ctr=0; ctr < strArray.length; ctr++)
                        if (strArray[ctr] != null)
                                list.add(Integer.parseInt(strArray[ctr]));
                }
                return list;
    }
 // read a comma delimited string into an Integer list
    /*
        public static List<Integer> getIntArr(String root) {
                
                String[] strArray = getString(root.toLowerCase()).split(",");
                List<Integer> list = new ArrayList<Integer>();
                if (strArray != null) {
                for (int ctr=0; ctr < strArray.length; ctr++)
                        if (strArray[ctr] != null)
                                list.add(Integer.parseInt(strArray[ctr]));
                }       
                return list;
        }
        */

    private static List<String> getStrArr(ConfigNodes node) {
        String[] strArray = getString(node.getRoot().toLowerCase(), node.getDefault()).split(",");
                List<String> list = new ArrayList<String>();
                if (strArray != null) {
                for (int ctr=0; ctr < strArray.length; ctr++)
                        if (strArray[ctr] != null)
                                list.add(strArray[ctr].trim());
                }
                return list;
    }
        // read a comma delimited string into a trimmed list.
    /*
        public static List<String> getStrArr(String root) {
                
                String[] strArray = getString(root.toLowerCase()).split(",");
                List<String> list = new ArrayList<String>();
                if (strArray != null) {
                for (int ctr=0; ctr < strArray.length; ctr++)
                        if (strArray[ctr] != null)
                                list.add(strArray[ctr].trim());
                }
                return list;
        }
        */
    
    public static void addComment(String root, String...comments) {
        newConfig.addComment(root.toLowerCase(), comments);
    }
    
    /**
    * Builds a new config reading old config data.
    */
        private static void setDefaults(String version, File file) {
        	
        	newConfig = new CommentedConfiguration(file);
        	
            for (ConfigNodes root : ConfigNodes.values()) {
            	if (root.getComments().length > 0)
            		addComment(root.getRoot(), root.getComments());
            	
            	if (root == ConfigNodes.LEVELS)
            		setDefaultLevels();
            	else if ((root.getRoot() == ConfigNodes.LEVELS_TOWN_LEVEL.getRoot()) || (root.getRoot() == ConfigNodes.LEVELS_NATION_LEVEL.getRoot())) {
            		// Do nothing here as setDefaultLevels configured town and nation levels.
            	} else if (root == ConfigNodes.VERSION){
            		setNewProperty(ConfigNodes.VERSION.getRoot(), version);
            	} else if (root == ConfigNodes.LAST_RUN_VERSION) {
            		setNewProperty(ConfigNodes.LAST_RUN_VERSION.getRoot(), getLastRunVersion(version));
            	} else
            		setNewProperty(root.getRoot(), (config.getProperty(root.getRoot().toLowerCase()) != null) ? config.getString(root.getRoot().toLowerCase()) : root.getDefault());
            }
            
            config = newConfig;
            newConfig = null;
        }

    private static void setDefaultLevels() {
        addComment(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), "",
                "############################################################",
                "# +------------------------------------------------------+ #",
                "# |                 default Town levels                  | #",
                "# +------------------------------------------------------+ #",
                "############################################################", "");
        List<ConfigurationNode> townLevels = new ArrayList<ConfigurationNode>(config.getNodeList(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), null));
        if (townLevels.isEmpty() || townLevels.size() == 0) {
            List<HashMap<String, Object>> levels = new ArrayList<HashMap<String, Object>>();
            HashMap<String, Object> level = new HashMap<String, Object>();
            level.put("numResidents", 0);
            level.put("namePrefix", "");
            level.put("namePostfix", " Ruins");
            level.put("mayorPrefix", "Spirit ");
            level.put("mayorPostfix", "");
            level.put("townBlockLimit", 1);
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("namePrefix", "");
            level.put("namePostfix", " (Settlement)");
            level.put("mayorPrefix", "Hermit ");
            level.put("mayorPostfix", "");
            level.put("townBlockLimit", 16);
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 2);
            level.put("namePrefix", "");
            level.put("namePostfix", " (Hamlet)");
            level.put("mayorPrefix", "Chief ");
            level.put("mayorPostfix", "");
            level.put("townBlockLimit", 32);
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 6);
            level.put("namePrefix", "");
            level.put("namePostfix", " (Village)");
            level.put("mayorPrefix", "Baron Von ");
            level.put("mayorPostfix", "");
            level.put("townBlockLimit", 96);
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 10);
            level.put("namePrefix", "");
            level.put("namePostfix", " (Town)");
            level.put("mayorPrefix", "Viscount ");
            level.put("mayorPostfix", "");
            level.put("townBlockLimit", 160);
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 14);
            level.put("namePrefix", "");
            level.put("namePostfix", " (Large Town)");
            level.put("mayorPrefix", "Count Von ");
            level.put("mayorPostfix", "");
            level.put("townBlockLimit", 224);
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 20);
            level.put("namePrefix", "");
            level.put("namePostfix", " (City)");
            level.put("mayorPrefix", "Earl ");
            level.put("mayorPostfix", "");
            level.put("townBlockLimit", 320);
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 24);
            level.put("namePrefix", "");
            level.put("namePostfix", " (Large City)");
            level.put("mayorPrefix", "Duke ");
            level.put("mayorPostfix", "");
            level.put("townBlockLimit", 384);
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 28);
            level.put("namePrefix", "");
            level.put("namePostfix", " (Metropolis)");
            level.put("mayorPrefix", "Lord ");
            level.put("mayorPostfix", "");
            level.put("townBlockLimit", 448);
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            newConfig.setProperty(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), levels);
        } else
        	newConfig.setProperty(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot(), config.getProperty(ConfigNodes.LEVELS_TOWN_LEVEL.getRoot()));
        
        addComment(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), "",
                "############################################################",
                "# +------------------------------------------------------+ #",
                "# |                default Nation levels                 | #",
                "# +------------------------------------------------------+ #",
                "############################################################", "");
        ArrayList<ConfigurationNode> nationLevels = new ArrayList<ConfigurationNode>(config.getNodeList(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), null));
        if (nationLevels.isEmpty() || nationLevels.size() == 0) {
            List<HashMap<String, Object>> levels = new ArrayList<HashMap<String, Object>>();
            HashMap<String, Object> level = new HashMap<String, Object>();
            level.put("numResidents", 0);
            level.put("namePrefix", "Land of ");
            level.put("namePostfix", " (Nation)");
            level.put("capitalPrefix", "");
            level.put("capitalPostfix", "");
            level.put("kingPrefix", "Leader ");
            level.put("kingPostfix", "");
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 10);
            level.put("namePrefix", "Federation of ");
            level.put("namePostfix", " (Nation)");
            level.put("capitalPrefix", "");
            level.put("capitalPostfix", "");
            level.put("kingPrefix", "Count ");
            level.put("kingPostfix", "");
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 20);
            level.put("namePrefix", "Dominion of ");
            level.put("namePostfix", " (Nation)");
            level.put("capitalPrefix", "");
            level.put("capitalPostfix", "");
            level.put("kingPrefix", "Duke ");
            level.put("kingPostfix", "");
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 30);
            level.put("namePrefix", "Kingdom of ");
            level.put("namePostfix", " (Nation)");
            level.put("capitalPrefix", "");
            level.put("capitalPostfix", "");
            level.put("kingPrefix", "King ");
            level.put("kingPostfix", "");
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 40);
            level.put("namePrefix", "The ");
            level.put("namePostfix", " Empire");
            level.put("capitalPrefix", "");
            level.put("capitalPostfix", "");
            level.put("kingPrefix", "Emperor ");
            level.put("kingPostfix", "");
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            level.put("numResidents", 60);
            level.put("namePrefix", "The ");
            level.put("namePostfix", " Realm");
            level.put("capitalPrefix", "");
            level.put("capitalPostfix", "");
            level.put("kingPrefix", "God Emperor ");
            level.put("kingPostfix", "");
            level.put("upkeepModifier", 1.0);
            levels.add(new HashMap<String, Object>(level));
            level.clear();
            newConfig.setProperty(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), levels);
        } else
        	newConfig.setProperty(ConfigNodes.LEVELS_NATION_LEVEL.getRoot(), config.getProperty(ConfigNodes.LEVELS_NATION_LEVEL.getRoot()));
    }


    ///////////////////////////////////
    
        public static String[] getRegistrationMsg(String name) {                
                return parseString(String.format(getLangString("MSG_REGISTRATION"), name));
        }

        public static String[] getNewTownMsg(String who, String town) {
                return parseString(String.format(getLangString("MSG_NEW_TOWN"), who, town));
        }

        public static String[] getNewNationMsg(String who, String nation) {
                return parseString(String.format(getLangString("MSG_NEW_NATION"), who, nation));
        }

        public static String[] getJoinTownMsg(String who) {
                return parseString(String.format(getLangString("MSG_JOIN_TOWN"), who));
        }

        public static String[] getJoinNationMsg(String who) {
                return parseString(String.format(getLangString("MSG_JOIN_NATION"), who));
        }

        public static String[] getNewMayorMsg(String who) {
                return parseString(String.format(getLangString("MSG_NEW_MAYOR"), who));
        }

        public static String[] getNewKingMsg(String who, String nation) {
                return parseString(String.format(getLangString("MSG_NEW_KING"), who, nation));
        }
        
        public static String[] getJoinWarMsg(TownyObject obj) {
                return parseString(String.format(getLangString("MSG_WAR_JOIN"), obj.getName()));
        }

        public static String[] getWarTimeEliminatedMsg(String who) {
                return parseString(String.format(getLangString("MSG_WAR_ELIMINATED"), who));
        }
        
        public static String[] getWarTimeForfeitMsg(String who) {
                return parseString(String.format(getLangString("MSG_WAR_FORFEITED"), who));
        }
        
        public static String[] getWarTimeLoseTownBlockMsg(WorldCoord worldCoord, String town) {
                return parseString(String.format(getLangString("MSG_WAR_LOSE_BLOCK"), worldCoord.toString(), town));
        }
        
        public static String[] getWarTimeScoreMsg(Town town, int n) {
                return parseString(String.format(getLangString("MSG_WAR_SCORE"), town.getName(), n));
        }
        
        public static String[] getCouldntPayTaxesMsg(TownyObject obj, String type) {
                return parseString(String.format(getLangString("MSG_COULDNT_PAY_TAXES"), obj.getName(), type));
        }
        
        public static String getPayedTownTaxMsg() {
                return getLangString("MSG_PAYED_TOWN_TAX");
        }
        
        public static String getPayedResidentTaxMsg() {
                return getLangString("MSG_PAYED_RESIDENT_TAX");
        }
        
        public static String getTaxExemptMsg() {
                return getLangString("MSG_TAX_EXEMPT");
        }
        
        public static String[] getDelResidentMsg(Resident resident) {
                return parseString(String.format(getLangString("MSG_DEL_RESIDENT"), resident.getName()));
        }
        
        public static String[] getDelTownMsg(Town town) {
                return parseString(String.format(getLangString("MSG_DEL_TOWN"), town.getName()));
        }
        
        public static String[] getDelNationMsg(Nation nation) {
                return parseString(String.format(getLangString("MSG_DEL_NATION"), nation.getName()));
        }
        
        public static String[] getBuyResidentPlotMsg(String who, String owner, Double price) {
                return parseString(String.format(getLangString("MSG_BUY_RESIDENT_PLOT"), who, owner, price));
        }
        
        public static String[] getPlotForSaleMsg(String who, WorldCoord worldCoord) {
                return parseString(String.format(getLangString("MSG_PLOT_FOR_SALE"), who, worldCoord.toString()));
        }
        
        public static String[] getMayorAbondonMsg() {
                return parseString(getLangString("MSG_MAYOR_ABANDON"));
        }
        
        public static String getNotPermToNewTownLine() {
                return parseSingleLineString(getLangString("MSG_ADMIN_ONLY_CREATE_TOWN"));
        }
        
        public static String getNotPermToNewNationLine() {
                return parseSingleLineString(getLangString("MSG_ADMIN_ONLY_CREATE_NATION"));
        }
        
        ////////////////////////////////////////////

        public static String getKingPrefix(Resident resident) {
                try {
                        return (String)getNationLevel(resident.getTown().getNation()).get(TownySettings.NationLevel.KING_PREFIX);
                } catch (NotRegisteredException e) {
                        System.out.println("[Towny] Error: Could not read getKingPrefix.");
                        return "";
                }
        }

        public static String getMayorPrefix(Resident resident) {
                try {
                        return (String)getTownLevel(resident.getTown()).get(TownySettings.TownLevel.MAYOR_PREFIX);
                } catch (NotRegisteredException e) {
                        System.out.println("[Towny] Error: Could not read getMayorPrefix.");
                        return "";
                }
        }

        public static String getCapitalPostfix(Town town) {
                try {
                        return (String)getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_POSTFIX);
                } catch (NotRegisteredException e) {
                        System.out.println("[Towny] Error: Could not read getCapitalPostfix.");
                        return "";
                }
        }

        public static String getTownPostfix(Town town) {
                try {
                        return (String)getTownLevel(town).get(TownySettings.TownLevel.NAME_POSTFIX);
                } catch (Exception e) {
                        System.out.println("[Towny] Error: Could not read getTownPostfix.");
                        return "";
                }
        }
        
        public static String getNationPostfix(Nation nation) {
                try {
                        return (String)getNationLevel(nation).get(TownySettings.NationLevel.NAME_POSTFIX);
                } catch (Exception e) {
                        System.out.println("[Towny] Error: Could not read getNationPostfix.");
                        return "";
                }
        }
        
        public static String getNationPrefix(Nation nation) {
                try {
                        return (String)getNationLevel(nation).get(TownySettings.NationLevel.NAME_PREFIX);
                } catch (Exception e) {
                        System.out.println("[Towny] Error: Could not read getNationPrefix.");
                        return "";
                }
        }
        
        public static String getTownPrefix(Town town) {
                try {
                        return (String)getTownLevel(town).get(TownySettings.TownLevel.NAME_PREFIX);
                } catch (Exception e) {
                        System.out.println("[Towny] Error: Could not read getTownPrefix.");
                        return "";
                }
        }
        
        public static String getCapitalPrefix(Town town) {
                try {
                        return (String)getNationLevel(town.getNation()).get(TownySettings.NationLevel.CAPITAL_PREFIX);
                } catch (NotRegisteredException e) {
                        System.out.println("[Towny] Error: Could not read getCapitalPrefix.");
                        return "";
                }
        }
        
        public static String getKingPostfix(Resident resident) {
                try {
                        return (String)getNationLevel(resident.getTown().getNation()).get(TownySettings.NationLevel.KING_POSTFIX);
                } catch (NotRegisteredException e) {
                        System.out.println("[Towny] Error: Could not read getKingPostfix.");
                        return "";
                }
        }
        
        public static String getMayorPostfix(Resident resident) {
                try {
                        return (String)getTownLevel(resident.getTown()).get(TownySettings.TownLevel.MAYOR_POSTFIX);
                } catch (NotRegisteredException e) {
                        System.out.println("[Towny] Error: Could not read getMayorPostfix.");
                        return "";
                }
        }
        
        public static String getNPCPrefix() {
                return getString(ConfigNodes.FILTERS_NPC_PREFIX.getRoot(), ConfigNodes.FILTERS_NPC_PREFIX.getDefault());
        }

        // Towny commands
        /*
        public static List<String> getResidentCommands() {
                return getStrArr("commands.resident.aliases", "res, p, player");
        }
        
        public static List<String> getTownCommands() {
                return getStrArr("commands.town.aliases", "t");
        }
        
        public static List<String> getNationCommands() {
                return getStrArr("commands.nation.aliases", "n, nat");
        }
        
        public static List<String> getWorldCommands() {
                return getStrArr("commands.townyworld.aliases", "tw");
        }
        
        public static List<String> getPlotCommands() {
                return getStrArr("commands.plot.aliases", "");
        }
        
        public static List<String> getTownyCommands() {
                return getStrArr("commands.towny.aliases", "");
        }
        
        public static List<String> getTownyAdminCommands() {
                return getStrArr("commands.townyadmin.aliases", "ta");
        }
        
        public static List<String> getTownChatCommands() {
                return getStrArr("commands.townchat.aliases", "tc");
        }
        
        public static List<String> getNationChatCommands() {
                return getStrArr("commands.nationchat.aliases", "nc");
        }
        
        public static String getFirstCommand(List<String> commands) {
                if (commands.size() > 0)
                        return commands.get(0);
                else
                        return "/<unknown>";
        }
        */
        
        ///////////////////////////////
        public static long getInactiveAfter() {
        String time = getString(ConfigNodes.RES_SETTING_INACTIVE_AFTER_TIME);
        if (Pattern.matches(".*[a-zA-Z].*", time)) {
            return TimeTools.secondsFromDhms(time);
        }
                return getLong(ConfigNodes.RES_SETTING_INACTIVE_AFTER_TIME);
        }

        public static String getLoadDatabase() {
                return getString(ConfigNodes.PLUGIN_DATABASE_LOAD);
        }

        public static String getSaveDatabase() {
                return getString(ConfigNodes.PLUGIN_DATABASE_SAVE);
        }
        
        /*
        public static boolean isFirstRun() {
                return getBoolean("FIRST_RUN");
        }
        */

        public static int getMaxTownBlocks(Town town) {
                int ratio = getTownBlockRatio();
                if (ratio == 0)
                        return town.getBonusBlocks() + (Integer)getTownLevel(town).get(TownySettings.TownLevel.TOWN_BLOCK_LIMIT);
                else
                        return town.getBonusBlocks() + town.getNumResidents()*ratio;
        }

    public static int getTownBlockRatio() {
        return getInt(ConfigNodes.TOWN_TOWN_BLOCK_RATIO);
    }

        public static int getTownBlockSize() {
                return getInt(ConfigNodes.TOWN_TOWN_BLOCK_SIZE);
        }

        public static boolean getFriendlyFire() {
                return getBoolean(ConfigNodes.GTOWN_SETTINGS_FRIENDLY_FIRE);
        }

        public static boolean isTownCreationAdminOnly() {
                return getBoolean(ConfigNodes.PERMS_TOWN_CREATION_ADMIN_ONLY);
        }
        
        public static boolean isNationCreationAdminOnly() {
                return getBoolean(ConfigNodes.PERMS_NATION_CREATION_ADMIN_ONLY);
        }

        public static boolean isUsingIConomy() {
                return getBoolean(ConfigNodes.PLUGIN_USING_ICONOMY);
        }

    public static void setUsingIConomy(boolean newSetting) {
        setProperty(ConfigNodes.PLUGIN_USING_ICONOMY.getRoot(), newSetting);
    }
        
        public static boolean isUsingEssentials() {
                return getBoolean(ConfigNodes.PLUGIN_USING_ESSENTIALS);
        }

    public static void setUsingEssentials(boolean newSetting) {
        setProperty(ConfigNodes.PLUGIN_USING_ESSENTIALS.getRoot(), newSetting);
    }

        public static double getNewTownPrice() {
                return getDouble(ConfigNodes.ECO_PRICE_NEW_TOWN);
        }
        
        public static double getNewNationPrice() {
                return getDouble(ConfigNodes.ECO_PRICE_NEW_NATION);
        }

        public static boolean getUnclaimedZoneBuildRights() {
                return getBoolean(ConfigNodes.UNCLAIMED_ZONE_BUILD);
        }
        
        public static boolean getUnclaimedZoneDestroyRights() {
                return getBoolean(ConfigNodes.UNCLAIMED_ZONE_DESTROY);
        }
        
        public static boolean getUnclaimedZoneItemUseRights() {
                return getBoolean(ConfigNodes.UNCLAIMED_ZONE_ITEM_USE);
        }

        public static boolean getDebug() {
                return getBoolean(ConfigNodes.PLUGIN_DEBUG_MODE);
        }

    public static void setDebug(boolean b) {
        setProperty(ConfigNodes.PLUGIN_DEBUG_MODE.getRoot(), b);
    }

        public static boolean getShowTownNotifications() {
                return getBoolean(ConfigNodes.GTOWN_SETTINGS_SHOW_TOWN_NOTIFICATIONS);
        }

        public static String getUnclaimedZoneName() {
                return getLangString("UNCLAIMED_ZONE_NAME");
        }

    public static String getModifyChatFormat() {
        return getString(ConfigNodes.FILTERS_MODIFY_CHAT_FORMAT);
    }
    
    public static int getMaxTitleLength() {
        return getInt(ConfigNodes.FILTERS_MODIFY_CHAT_MAX_LGTH);
    }

    public static boolean isUsingModifyChat() {
        return getBoolean(ConfigNodes.FILTERS_MODIFY_CHAT_ENABLE);
    }

    public static String getKingColour() {
        return getString(ConfigNodes.FILTERS_COLOUR_KING);
    }
    public static String getMayorColour() {
        return getString(ConfigNodes.FILTERS_COLOUR_MAYOR);
    }
        
        public static long getDeleteTime() {
        String time = getString(ConfigNodes.RES_SETTING_DELETE_OLD_RESIDENTS_TIME);
        if (Pattern.matches(".*[a-zA-Z].*", time)) {
            return TimeTools.secondsFromDhms(time);
        }
                return getLong(ConfigNodes.RES_SETTING_DELETE_OLD_RESIDENTS_TIME);
        }

        public static boolean isDeletingOldResidents() {
                return getBoolean(ConfigNodes.RES_SETTING_DELETE_OLD_RESIDENTS_ENABLE);
        }

        public static int getWarTimeWarningDelay() {
                return getInt(ConfigNodes.WARTIME_WARNING_DELAY);
        }

        public static int getWarzoneTownBlockHealth() {
                return getInt(ConfigNodes.WARTIME_TOWN_BLOCK_HP);
        }
        
        public static int getWarzoneHomeBlockHealth() {
                return getInt(ConfigNodes.WARTIME_HOME_BLOCK_HP);
        }


        public static String[] getWarTimeLoseTownBlockMsg(WorldCoord worldCoord) {
                return getWarTimeLoseTownBlockMsg(worldCoord, "");
        }
        
        public static String getDefaultTownName() {
                return getString(ConfigNodes.RES_SETTING_DEFAULT_TOWN_NAME);
        }       
        
        public static int getWarPointsForTownBlock() {
                return getInt(ConfigNodes.WARTIME_POINTS_TOWNBLOCK);
        }
        
        public static int getWarPointsForTown() {
                return getInt(ConfigNodes.WARTIME_POINTS_TOWN);
        }
        
        public static int getWarPointsForNation() {
                return getInt(ConfigNodes.WARTIME_POINTS_NATION);
        }

        public static int getWarPointsForKill() {
                return getInt(ConfigNodes.WARTIME_POINTS_KILL);
        }
        
        public static int getMinWarHeight() {
                return getInt(ConfigNodes.WARTIME_MIN_HEIGHT);
        }
        
        public static List<String> getWorldMobRemovalEntities() {
                if (getDebug()) System.out.println("[Towny] Debug: Reading World Mob removal entities. ");
                return getStrArr(ConfigNodes.PROT_MOB_REMOVE_WORLD);
        }
        
        public static List<String> getTownMobRemovalEntities() {
                if (getDebug()) System.out.println("[Towny] Debug: Reading Town Mob removal entities. ");
                return getStrArr(ConfigNodes.PROT_MOB_REMOVE_TOWN);
        }
        
        public static long getMobRemovalSpeed() {
        String time = getString(ConfigNodes.PROT_MOB_REMOVE_SPEED);
        if (Pattern.matches(".*[a-zA-Z].*", time)) {
            return TimeTools.secondsFromDhms(time);
        }
                return getLong(ConfigNodes.PROT_MOB_REMOVE_SPEED);
        }
        
        /*
        public static boolean isRemovingWorldMobs() {
                return getBoolean("protection.MOB_REMOVAL_WORLD");
        }
        
        public static boolean isRemovingTownMobs() {
                return getBoolean("protection.MOB_REMOVAL_TOWN");
        }
        */

        public static int getHealthRegenSpeed() {
                return getInt(ConfigNodes.GTOWN_SETTINGS_REGEN_SPEED);
        }
        
        public static boolean hasHealthRegen() {
                return getBoolean(ConfigNodes.GTOWN_SETTINGS_REGEN_ENABLE);
        }
        
        public static boolean hasTownLimit() {
                return getTownLimit() == 0;
        }
        
        public static int getTownLimit() {
                return getInt(ConfigNodes.TOWN_LIMIT);
        }
        
        public static double getNationNeutralityCost() {
                return getDouble(ConfigNodes.ECO_PRICE_NATION_NEUTRALITY);
        }
        
        public static boolean isAllowingOutposts() {
                return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_OUTPOSTS);
        }
        
        public static double getOutpostCost() {
                return getDouble(ConfigNodes.ECO_PRICE_OUTPOST);
        } 
        
        public static List<Integer> getSwitchIds() {
                return getIntArr(ConfigNodes.PROT_SWITCH_ID);
        }
        
        public static List<Integer> getUnclaimedZoneIgnoreIds() {
                return getIntArr(ConfigNodes.UNCLAIMED_ZONE_IGNORE);
        }
        
        public static List<Integer> getItemUseIds() {
                return getIntArr(ConfigNodes.PROT_ITEM_USE_ID);
        }
        
        public static boolean isUnclaimedZoneIgnoreId(int id) {
                return getUnclaimedZoneIgnoreIds().contains(id);
        }

        public static boolean isSwitchId(int id) {
                return getSwitchIds().contains(id);
        }
        
        public static boolean isItemUseId(int id) {
                return getItemUseIds().contains(id);
        }
        
        private static void setProperty(String root, Object value) {
                config.setProperty(root.toLowerCase(), value);
        }
        
        private static void setNewProperty(String root, Object value) {
                newConfig.setProperty(root.toLowerCase(), value);
        }
              
        public static Object getProperty(String root) {
                return config.getProperty(root.toLowerCase());

        }

        public static double getClaimPrice() {
                return getDouble(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK);
        }

        public static boolean getUnclaimedZoneSwitchRights() {
                return getBoolean(ConfigNodes.UNCLAIMED_ZONE_SWITCH);
        }

        public static String getUnclaimedPlotName() {
                return getLangString("UNCLAIMED_PLOT_NAME");
        }

        public static long getDayInterval() {
        String time = getString(ConfigNodes.PLUGIN_DAY_INTERVAL);
        if (Pattern.matches(".*[a-zA-Z].*", time)) {
            return TimeTools.secondsFromDhms(time);
        }
                return getLong(ConfigNodes.PLUGIN_DAY_INTERVAL);
        }
        
        public static boolean isAllowingTownSpawn() {
                return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN);
        }
        
        public static boolean isAllowingPublicTownSpawnTravel() {
                return getBoolean(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN_TRAVEL);
        }

    public static List<String> getDisallowedTownSpawnZones() {
        if (getDebug()) System.out.println("[Towny] Debug: Reading disallowed town spawn zones. ");
        return getStrArr(ConfigNodes.GTOWN_SETTINGS_PREVENT_TOWN_SPAWN_IN);
    }
        
        public static boolean isTaxingDaily() {
                return getBoolean(ConfigNodes.GTOWN_SETTINGS_DAILY_TAXES);
        }
        
        public static boolean isBackingUpDaily() {
                return getBoolean(ConfigNodes.PLUGIN_DAILY_BACKUPS);
        }
        
        public static double getTownSpawnTravelPrice() {
                return getDouble(ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL);
        }
        
        public static double getTownPublicSpawnTravelPrice() {
                return getDouble(ConfigNodes.ECO_PRICE_TOWN_PUBLIC_SPAWN_TRAVEL);
        }
        
        public static double getBaseSpoilsOfWar() {
                return getDouble(ConfigNodes.WARTIME_BASE_SPOILS);
        }
        
        public static double getWartimeDeathPrice() {
                return getDouble(ConfigNodes.ECO_PRICE_DEATH_WARTIME);
        }
        
        public static double getDeathPrice() {
                return getDouble(ConfigNodes.ECO_PRICE_DEATH);
        }
        
        public static double getWartimeTownBlockLossPrice() {
                return getDouble(ConfigNodes.WARTIME_TOWN_BLOCK_LOSS_PRICE);
        }
        
        public static boolean isDevMode() {
                return getBoolean(ConfigNodes.PLUGIN_DEV_MODE_ENABLE);
        }

    public static void setDevMode(boolean choice) {
        setProperty(ConfigNodes.PLUGIN_DEV_MODE_DEV_NAME.getRoot(), choice);
    }

    public static String getDevName() {
        return getString(ConfigNodes.PLUGIN_DEV_MODE_DEV_NAME);
    }
        
        /*
        public static boolean isPvEWithinNonPvPZones() {
                return getBoolean("PVE_IN_NON_PVP_TOWNS");
        }
        */

        public static boolean isDeclaringNeutral() {
                return getBoolean(ConfigNodes.WARTIME_NATION_CAN_BE_NEUTRAL);
        }

    public static void setDeclaringNeutral(boolean choice) {
        setProperty(ConfigNodes.WARTIME_NATION_CAN_BE_NEUTRAL.getRoot(), choice);
    }

        public static boolean isRemovingOnMonarchDeath() {
                return getBoolean(ConfigNodes.WARTIME_REMOVE_ON_MONARCH_DEATH);
        }

        public static double getTownUpkeepCost(Town town) {
                double multiplier;
                
                if (town != null)
                        multiplier = Double.valueOf(getTownLevel(town).get(TownySettings.TownLevel.UPKEEP_MULTIPLIER).toString());
                else
                        multiplier = 1.0;
                        
                return getTownUpkeep() * multiplier;
        }

    public static double getTownUpkeep() {
        return getDouble(ConfigNodes.ECO_PRICE_TOWN_UPKEEP);
    }

    public static double getNationUpkeep() {
        return getDouble(ConfigNodes.ECO_PRICE_NATION_UPKEEP);
    }
        
        public static double getNationUpkeepCost(Nation nation) {
                double multiplier;
                
                if (nation != null)
                        multiplier = Double.valueOf(getNationLevel(nation).get(TownySettings.NationLevel.UPKEEP_MULTIPLIER).toString());
                else
                        multiplier = 1.0;
                
                return getNationUpkeep() * multiplier;
        }
        
        public static String getFlatFileBackupType() {
                return getString(ConfigNodes.PLUGIN_FLATFILE_BACKUP);
        }
        

        public static boolean isForcingPvP() {
                return getBoolean(ConfigNodes.NWS_FORCE_PVP_ON);
        }

    public static boolean isPlayerTramplingCropsDisabled() {
        return getBoolean(ConfigNodes.NWS_DISABLE_PLAYER_CROP_TRAMPLING);
    }

    public static boolean isCreatureTramplingCropsDisabled() {
        return getBoolean(ConfigNodes.NWS_DISABLE_CREATURE_CROP_TRAMPLING);
    }

    public static boolean isWorldMonstersOn() {
        return getBoolean(ConfigNodes.NWS_WORLD_MONSTERS_ON);
    }

        public static boolean isForcingExplosions() {
                return getBoolean(ConfigNodes.NWS_FORCE_EXPLOSIONS_ON);
        }
        
        public static boolean isForcingMonsters() {
                return getBoolean(ConfigNodes.NWS_FORCE_TOWN_MONSTERS_ON);
        }
        
        public static boolean isForcingFire() {
                return getBoolean(ConfigNodes.NWS_FORCE_FIRE_ON);
        }

        public static boolean isTownRespawning() {
                return getBoolean(ConfigNodes.GTOWN_SETTINGS_TOWN_RESPAWN);
        }
        
        public static boolean isTownyUpdating(String currentVersion) {
                if (isTownyUpToDate(currentVersion))
                        return false;
                else
                        return true; //Assume
        }
        
        public static boolean isTownyUpToDate(String currentVersion) {
                return currentVersion.equals(getLastRunVersion(currentVersion));
        }

        public static String getLastRunVersion(String currentVersion) {
                return getString(ConfigNodes.LAST_RUN_VERSION.getRoot(), currentVersion);
        }

    public static void setLastRunVersion(String currentVersion) {
        setProperty(ConfigNodes.LAST_RUN_VERSION.getRoot(), currentVersion);
        config.save();
    }

        public static int getMinDistanceFromTownHomeblocks() {
                return getInt(ConfigNodes.TOWN_MIN_DISTANCE_FROM_TOWN_HOMEBLOCK);
        }
        
        public static int getMaxDistanceBetweenHomeblocks() {
                return getInt(ConfigNodes.TOWN_MAX_DISTANCE_BETWEEN_HOMEBLOCKS);
        }

        public static int getMaxPlotsPerResident() {
                return getInt(ConfigNodes.TOWN_MAX_PLOTS_PER_RESIDENT);
        }

    public static boolean getPermFlag_Resident_Friend_Build() {
        return getBoolean(ConfigNodes.FLAGS_RES_FR_BUILD);
    }
    public static boolean getPermFlag_Resident_Friend_Destroy() {
        return getBoolean(ConfigNodes.FLAGS_RES_FR_DESTROY);
    }
    public static boolean getPermFlag_Resident_Friend_ItemUse() {
        return getBoolean(ConfigNodes.FLAGS_RES_FR_ITEM_USE);
    }
    public static boolean getPermFlag_Resident_Friend_Switch() {
        return getBoolean(ConfigNodes.FLAGS_RES_FR_SWITCH);
    }

    public static boolean getPermFlag_Resident_Ally_Build() {
        return getBoolean(ConfigNodes.FLAGS_RES_ALLY_BUILD);
    }
    public static boolean getPermFlag_Resident_Ally_Destroy() {
        return getBoolean(ConfigNodes.FLAGS_RES_ALLY_DESTROY);
    }
    public static boolean getPermFlag_Resident_Ally_ItemUse() {
        return getBoolean(ConfigNodes.FLAGS_RES_ALLY_ITEM_USE);
    }
    public static boolean getPermFlag_Resident_Ally_Switch() {
        return getBoolean(ConfigNodes.FLAGS_RES_ALLY_SWITCH);
    }

    public static boolean getPermFlag_Resident_Outsider_Build() {
        return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_BUILD);
    }
    public static boolean getPermFlag_Resident_Outsider_Destroy() {
        return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_DESTROY);
    }
    public static boolean getPermFlag_Resident_Outsider_ItemUse() {
        return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_ITEM_USE);
    }
    public static boolean getPermFlag_Resident_Outsider_Switch() {
        return getBoolean(ConfigNodes.FLAGS_RES_OUTSIDER_SWITCH);
    }

    public static boolean getPermFlag_Town_Resident_Build() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_RES_BUILD);
    }
    public static boolean getPermFlag_Town_Resident_Destroy() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_RES_DESTROY);
    }
    public static boolean getPermFlag_Town_Resident_ItemUse() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_RES_ITEM_USE);
    }
    public static boolean getPermFlag_Town_Resident_Switch() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_RES_SWITCH);
    }

    public static boolean getPermFlag_Town_Ally_Build() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_BUILD);
    }
    public static boolean getPermFlag_Town_Ally_Destroy() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_DESTROY);
    }
    public static boolean getPermFlag_Town_Ally_ItemUse() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_ITEM_USE);
    }
    public static boolean getPermFlag_Town_Ally_Switch() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_ALLY_SWITCH);
    }

    public static boolean getPermFlag_Town_Outsider_Build() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_BUILD);
    }
    public static boolean getPermFlag_Town_Outsider_Destroy() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_DESTROY);
    }
    public static boolean getPermFlag_Town_Outsider_ItemUse() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_ITEM_USE);
    }
    public static boolean getPermFlag_Town_Outsider_Switch() {
        return getBoolean(ConfigNodes.FLAGS_TOWN_OUTSIDER_SWITCH);
    }

        public static boolean getDefaultResidentPermission(TownBlockOwner owner, ActionType type) {
                if (owner instanceof Resident)
                        switch (type) {
                                case BUILD: return getPermFlag_Resident_Friend_Build();
                                case DESTROY: return getPermFlag_Resident_Friend_Destroy();
                                case SWITCH: return getPermFlag_Resident_Friend_Switch();
                                case ITEM_USE: return getPermFlag_Resident_Friend_ItemUse();
                                default: throw new UnsupportedOperationException();
                        }
                else if (owner instanceof Town)
                        switch (type) {
                                case BUILD: return getPermFlag_Town_Resident_Build();
                                case DESTROY: return getPermFlag_Town_Resident_Destroy();
                                case SWITCH: return getPermFlag_Town_Resident_Switch();
                                case ITEM_USE: return getPermFlag_Town_Resident_ItemUse();
                                default: throw new UnsupportedOperationException();
                        }
                else
                        throw new UnsupportedOperationException();
        }
        
        public static boolean getDefaultAllyPermission(TownBlockOwner owner, ActionType type) {
                if (owner instanceof Resident)
                        switch (type) {
                                case BUILD: return getPermFlag_Resident_Ally_Build();
                                case DESTROY: return getPermFlag_Resident_Ally_Destroy();
                                case SWITCH: return getPermFlag_Resident_Ally_Switch();
                                case ITEM_USE: return getPermFlag_Resident_Ally_ItemUse();
                                default: throw new UnsupportedOperationException();
                        }
                else if (owner instanceof Town)
                        switch (type) {
                                case BUILD: return getPermFlag_Town_Ally_Build();
                                case DESTROY: return getPermFlag_Town_Ally_Destroy();
                                case SWITCH: return getPermFlag_Town_Ally_Switch();
                                case ITEM_USE: return getPermFlag_Town_Ally_ItemUse();
                                default: throw new UnsupportedOperationException();
                        }
                else
                        throw new UnsupportedOperationException();
        }
        
        public static boolean getDefaultOutsiderPermission(TownBlockOwner owner, ActionType type) {
                if (owner instanceof Resident)
                        switch (type) {
                                case BUILD: return getPermFlag_Resident_Outsider_Build();
                                case DESTROY: return getPermFlag_Resident_Outsider_Destroy();
                                case SWITCH: return getPermFlag_Resident_Outsider_Switch();
                                case ITEM_USE: return getPermFlag_Resident_Outsider_ItemUse();
                                default: throw new UnsupportedOperationException();
                        }
                else if (owner instanceof Town)
                        switch (type) {
                                case BUILD: return getPermFlag_Town_Outsider_Build();
                                case DESTROY: return getPermFlag_Town_Outsider_Destroy();
                                case SWITCH: return getPermFlag_Town_Outsider_Switch();
                                case ITEM_USE: return getPermFlag_Town_Outsider_ItemUse();
                                default: throw new UnsupportedOperationException();
                        }
                else
                        throw new UnsupportedOperationException();
        }
        
        public static boolean getDefaultPermission(TownBlockOwner owner, PermLevel level, ActionType type) {
                switch (level) {
                        case RESIDENT: return getDefaultResidentPermission(owner, type);
                        case ALLY: return getDefaultAllyPermission(owner, type);
                        case OUTSIDER: return getDefaultOutsiderPermission(owner, type);
                        default: throw new UnsupportedOperationException();
                }
        }

        public static boolean isLogging() {
                return getBoolean(ConfigNodes.PLUGIN_LOGGING);
        }

        public static boolean isUsingQuestioner() {
                return getBoolean(ConfigNodes.PLUGIN_USING_QUESTIONER);
        }
    public static void setUsingQuestioner(boolean newSetting) {
        setProperty(ConfigNodes.PLUGIN_USING_QUESTIONER.getRoot(), newSetting);
    }
        
        public static boolean isAppendingToLog() {
                return !getBoolean(ConfigNodes.PLUGIN_RESET_LOG_ON_BOOT);
        }

        public static boolean isUsingPermissions() {
                return getBoolean(ConfigNodes.PLUGIN_USING_PERMISSIONS);
        }
    public static void setUsingPermissions(boolean newSetting) {
        setProperty(ConfigNodes.PLUGIN_USING_PERMISSIONS.getRoot(), newSetting);
    }
        
        public static String filterName(String input) {
                return input.replaceAll(getNameFilterRegex(), "_").replaceAll(getNameRemoveRegex(), "");
        }

    public static String getNameFilterRegex() {
        return getString(ConfigNodes.FILTERS_REGEX_NAME_FILTER_REGEX);
    }

    public static String getNameCheckRegex() {
        return getString(ConfigNodes.FILTERS_REGEX_NAME_CHECK_REGEX);
    }

    public static String getNameRemoveRegex() {
        return getString(ConfigNodes.FILTERS_REGEX_NAME_REMOVE_REGEX);
    }

    public static boolean isUsingCheatProtection() {
        return getBoolean(ConfigNodes.PROT_CHEAT);
    }
    
    public static int getTeleportWarmupTime() {
        return getInt(ConfigNodes.GTOWN_SETTINGS_SPAWN_TIMER);
    }

        public static boolean isValidRegionName(String name) {
                
                if ((name.toLowerCase() == "spawn")
                                || (name.equalsIgnoreCase("list"))
                                || (name.equalsIgnoreCase("new"))
                                || (name.equalsIgnoreCase("here"))
                                || (name.equalsIgnoreCase("new"))
                                || (name.equalsIgnoreCase("help"))
                                || (name.equalsIgnoreCase("?"))
                                || (name.equalsIgnoreCase("leave"))
                                || (name.equalsIgnoreCase("withdraw"))
                                || (name.equalsIgnoreCase("deposit"))
                                || (name.equalsIgnoreCase("set"))
                                || (name.equalsIgnoreCase("toggle"))
                                || (name.equalsIgnoreCase("mayor"))
                                || (name.equalsIgnoreCase("assistant"))
                                || (name.equalsIgnoreCase("kick"))
                                || (name.equalsIgnoreCase("add"))
                                || (name.equalsIgnoreCase("claim"))
                                || (name.equalsIgnoreCase("unclaim"))
                                || (name.equalsIgnoreCase("title")))
                        return false;
                
                return isValidName(name);
        }
                
        public static boolean isValidName(String name) {
                
                try {
                        if (TownySettings.namePattern == null)
                                namePattern = Pattern.compile(getNameCheckRegex());
                        return TownySettings.namePattern.matcher(name).find();
                } catch (PatternSyntaxException e) {
                        e.printStackTrace();
                        return false;
                }
        }

        /*
        public static boolean isSavingOnLoad() {
                return getBoolean("SAVE_ON_LOAD");
        }
        */
        
        /*
        public static boolean isAllowingResidentPlots() {
                return getBoolean("ALLOW_RESIDENT_PLOTS");
        }
        */
}
