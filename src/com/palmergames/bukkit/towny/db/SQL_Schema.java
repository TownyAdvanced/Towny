package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ElgarL
 */
public class SQL_Schema {

    private static String tb_prefix = TownySettings.getSQLTablePrefix().toUpperCase();

    private static String getWORLDS() {

        return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "WORLDS ("
                + "`name` VARCHAR(32) NOT NULL,"
                + "`towns` mediumtext NOT NULL,"
                + "`claimable` bool NOT NULL DEFAULT '0',"
                + "`pvp` bool NOT NULL DEFAULT '0',"
                + "`forcepvp` bool NOT NULL DEFAULT '0',"
                + "`forcetownmobs` bool NOT NULL DEFAULT '0',"
                + "`worldmobs` bool NOT NULL DEFAULT '0',"
                + "`firespread` bool NOT NULL DEFAULT '0',"
                + "`forcefirespread` bool NOT NULL DEFAULT '0',"
                + "`explosions` bool NOT NULL DEFAULT '0',"
                + "`forceexplosions` bool NOT NULL DEFAULT '0',"
                + "`endermanprotect` bool NOT NULL DEFAULT '0',"
                + "`disableplayertrample` bool NOT NULL DEFAULT '0',"
                + "`disablecreaturetrample` bool NOT NULL DEFAULT '0',"
                + "`unclaimedZoneBuild` bool NOT NULL DEFAULT '0',"
                + "`unclaimedZoneDestroy` bool NOT NULL DEFAULT '0',"
                + "`unclaimedZoneSwitch` bool NOT NULL DEFAULT '0',"
                + "`unclaimedZoneItemUse` bool NOT NULL DEFAULT '0',"
                + "`unclaimedZoneName` mediumtext NOT NULL,"
                + "`unclaimedZoneIgnoreIds` mediumtext NOT NULL,"
                + "`usingPlotManagementDelete` bool NOT NULL DEFAULT '0',"
                + "`plotManagementDeleteIds` mediumtext NOT NULL,"
                + "`usingPlotManagementMayorDelete` bool NOT NULL DEFAULT '0',"
                + "`plotManagementMayorDelete` mediumtext NOT NULL,"
                + "`usingPlotManagementRevert` bool NOT NULL DEFAULT '0',"
                + "`plotManagementRevertSpeed` long NOT NULL,"
                + "`plotManagementIgnoreIds` mediumtext NOT NULL,"
                + "`usingPlotManagementWildRegen` bool NOT NULL DEFAULT '0',"
                + "`plotManagementWildRegenEntities` mediumtext NOT NULL,"
                + "`plotManagementWildRegenSpeed` long NOT NULL,"
                + "`usingTowny` bool NOT NULL DEFAULT '0',"
                + "PRIMARY KEY (`name`)"
                + ")";
    }

    private static String getNATIONS() {

        return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "NATIONS ("
                + "`name` VARCHAR(32) NOT NULL,"
                + "`towns` mediumtext NOT NULL,"
                + "`capital` mediumtext NOT NULL,"
                + "`assistants` mediumtext NOT NULL,"
                + "`tag` mediumtext NOT NULL,"
                + "`allies` mediumtext NOT NULL,"
                + "`enemies` mediumtext NOT NULL,"
                + "`taxes` float NOT NULL,"
                + "`neutral` bool NOT NULL DEFAULT '0',"
                + "`uuid` VARCHAR(36) DEFAULT NULL,"
                + "PRIMARY KEY (`name`)"
                + ")";
    }

    private static String getTOWNS() {

        return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "TOWNS ("
                + "`name` VARCHAR(32) NOT NULL,"
                + "`residents` mediumtext,"
                + "`mayor` mediumtext,"
                + "`nation` mediumtext NOT NULL,"
                + "`assistants` text DEFAULT NULL,"
                + "`townBoard` mediumtext DEFAULT NULL,"
                + "`tag` mediumtext DEFAULT NULL,"
                + "`protectionStatus` mediumtext DEFAULT NULL,"
                + "`bonus` int(11) DEFAULT 0,"
                + "`purchased` int(11)  DEFAULT 0,"
                + "`taxpercent` bool NOT NULL DEFAULT '0',"
                + "`taxes` float DEFAULT 0,"
                + "`hasUpkeep` bool NOT NULL DEFAULT '0',"
                + "`plotPrice` float DEFAULT NULL,"
                + "`plotTax` float DEFAULT NULL,"
                + "`commercialPlotPrice` float DEFAULT NULL,"
                + "`commercialPlotTax` float NOT NULL,"
                + "`embassyPlotPrice` float NOT NULL,"
                + "`embassyPlotTax` float NOT NULL,"
                + "`open` bool NOT NULL DEFAULT '0',"
                + "`public` bool NOT NULL DEFAULT '0',"
                + "`admindisabledpvp` bool NOT NULL DEFAULT '0',"
                + "`homeblock` mediumtext NOT NULL,"
                //+ "`townBlocks` mediumtext NOT NULL,"
                + "`spawn` mediumtext NOT NULL,"
                + "`outpostSpawns` mediumtext DEFAULT NULL,"
                + "`jailSpawns` mediumtext DEFAULT NULL,"
                + "`outlaws` mediumtext DEFAULT NULL,"
                + "`uuid` VARCHAR(36) DEFAULT NULL,"
                + "PRIMARY KEY (`name`)"
                + ")";
    }

    private static String getRESIDENTS() {

        return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "RESIDENTS ("
                + " `name` VARCHAR(16) NOT NULL,"
                + "`town` mediumtext,"
                + "`town-ranks` mediumtext,"
                + "`nation-ranks` mediumtext,"
                + "`lastOnline` BIGINT NOT NULL,"
                + "`registered` BIGINT NOT NULL,"
                + "`isNPC` bool NOT NULL DEFAULT '0',"
                + "`isJailed` bool NOT NULL DEFAULT '0',"
                + "`JailSpawn` mediumint,"
                + "`JailTown` mediumtext,"
                + "`title` mediumtext,"
                + "`surname` mediumtext,"
                + "`protectionStatus` mediumtext,"
                + "`friends` mediumtext,"
                //+ "`townBlocks` mediumtext,"
                + "PRIMARY KEY (`name`)"
                + ")";
    }

    private static String getTOWNBLOCKS() {

        return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "TOWNBLOCKS ("
                + "`world` VARCHAR(32) NOT NULL,"
                + "`x` mediumint NOT NULL,"
                + "`z` mediumint NOT NULL,"
                + "`name` mediumtext,"
                + "`price` float DEFAULT '-1',"
                + "`town` mediumtext,"
                + "`resident` mediumtext,"
                + "`type` TINYINT NOT  NULL DEFAULT '0',"
                + "`outpost` bool NOT NULL DEFAULT '0',"
                + "`permissions` mediumtext NOT NULL,"
                + "`locked` bool NOT NULL DEFAULT '0',"
                + "`changed` bool NOT NULL DEFAULT '0',"
                + "PRIMARY KEY (`world`,`x`,`z`)"
                + ")";
    }

    /**
     * Create and update database schema.
     *
     * @param cntx    a database connection
     * @param db_name the name of a database
     */
    public static void initTables(Connection cntx, String db_name) {

		/*
         *  Fetch WORLDS Table schema.
		 */
        String world_create = SQL_Schema.getWORLDS();

        try {

            Statement s = cntx.createStatement();
            s.executeUpdate(world_create);
            TownyMessaging.sendDebugMsg("Table WORLDS is ok!");

        } catch (SQLException ee) {

            TownyMessaging.sendErrorMsg("Error Creating table WORLDS : " + ee.getMessage());

        }

		/*
         *  Fetch NATIONS Table schema.
		 */
        String nation_create = SQL_Schema.getNATIONS();

        try {

            Statement s = cntx.createStatement();
            s.executeUpdate(nation_create);
            TownyMessaging.sendDebugMsg("Table NATIONS is ok!");

        } catch (SQLException ee) {

            TownyMessaging.sendErrorMsg("Error Creating table NATIONS : " + ee.getMessage());

        }

		/*
         *  Fetch TOWNS Table schema.
		 */
        String town_create = SQL_Schema.getTOWNS();

        try {

            Statement s = cntx.createStatement();
            s.executeUpdate(town_create);

            TownyMessaging.sendDebugMsg("Table TOWNS is ok!");

        } catch (SQLException ee) {
            TownyMessaging.sendErrorMsg("Creating table TOWNS :" + ee.getMessage());
        }

		/*
         *  Fetch RESIDENTS Table schema.
		 */
        String resident_create = SQL_Schema.getRESIDENTS();

        try {

            Statement s = cntx.createStatement();
            s.executeUpdate(resident_create);
            TownyMessaging.sendDebugMsg("Table RESIDENTS is ok!");

        } catch (SQLException ee) {

            TownyMessaging.sendErrorMsg("Error Creating table RESIDENTS :" + ee.getMessage());

        }

		/*
         *  Fetch TOWNBLOCKS Table schema.
		 */
        String townblock_create = SQL_Schema.getTOWNBLOCKS();

        try {

            Statement s = cntx.createStatement();
            s.executeUpdate(townblock_create);
            TownyMessaging.sendDebugMsg("Table TOWNBLOCKS is ok!");

        } catch (SQLException ee) {

            TownyMessaging.sendErrorMsg("Error Creating table TOWNBLOCKS : " + ee.getMessage());

        }



		/*
         * Update the table structures for older databases.
		 *
		 * Update TOWNS.
		 */
        String town_update;
        List<String> townsSQLSchema_town = getValuesFromDefaultSchema(getTOWNS());
        for (String mysqlvalue : townsSQLSchema_town) {
            try {
                town_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "TOWNS` "
                        + "ADD COLUMN " + mysqlvalue;

                PreparedStatement ps = cntx.prepareStatement(town_update);
                ps.executeUpdate();

            } catch (SQLException ee) {
                if (ee.getErrorCode() != 1060)
                    TownyMessaging.sendErrorMsg("Error updating table TOWNS :" + ee.getMessage());
            }
        }
        TownyMessaging.sendDebugMsg("Table TOWNS is updated!");


		/*
         * Update RESIDENTS.
		 */
        String resident_update;
        List<String> townsSQLSchema_residents = getValuesFromDefaultSchema(getRESIDENTS());
        for (String mysqlvalue : townsSQLSchema_residents) {
            try {
                resident_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "RESIDENTS` "
                        + "ADD COLUMN " + mysqlvalue;

                PreparedStatement ps = cntx.prepareStatement(resident_update);
                ps.executeUpdate();

            } catch (SQLException ee) {
                if (ee.getErrorCode() != 1060)
                    TownyMessaging.sendErrorMsg("Error updating table RESIDENTS :" + ee.getMessage());
            }
        }
        TownyMessaging.sendDebugMsg("Table RESIDENTS is updated!");

		/*
		 * Update TOWNBLOCKS.
		 */
        String townblocks_update;
        List<String> townsSQLSchema_townblocks = getValuesFromDefaultSchema(getTOWNBLOCKS());
        for (String mysqlvalue : townsSQLSchema_townblocks) {
            try {
                townblocks_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "TOWNBLOCKS` "
                        + "ADD COLUMN " + mysqlvalue;

                PreparedStatement ps = cntx.prepareStatement(townblocks_update);
                ps.executeUpdate();

            } catch (SQLException ee) {
                if (ee.getErrorCode() != 1060)
                    TownyMessaging.sendErrorMsg("Error updating table TOWNBLOCKS :" + ee.getMessage());
            }
        }
        TownyMessaging.sendDebugMsg("Table TOWNBLOCKS is updated!");


		/*
		 * Update NATIONS.
		 */
        String nation_update;
        List<String> townsSQLSchema_nations = getValuesFromDefaultSchema(getNATIONS());
        for (String mysqlvalue : townsSQLSchema_nations) {
            try {
                nation_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "NATIONS` "
                        + "ADD COLUMN " + mysqlvalue;

                PreparedStatement ps = cntx.prepareStatement(nation_update);
                ps.executeUpdate();

            } catch (SQLException ee) {
                if (ee.getErrorCode() != 1060)
                    TownyMessaging.sendErrorMsg("Error updating table NATIONS :" + ee.getMessage());
            }
        }
        TownyMessaging.sendDebugMsg("Table NATIONS is updated!");
        /*
		 * Update WORLDS.
		 */
        String world_update;
        List<String> townsSQLSchema_world = getValuesFromDefaultSchema(getWORLDS());
        for (String mysqlvalue : townsSQLSchema_world) {
            try {
                world_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "WORLDS` "
                        + "ADD COLUMN " + mysqlvalue;


                PreparedStatement ps = cntx.prepareStatement(world_update);
                ps.executeUpdate();

            } catch (SQLException ee) {
                if (ee.getErrorCode() != 1060)
                    TownyMessaging.sendErrorMsg("Error updating table WORLDS :" + ee.getMessage());
            }
        }
        TownyMessaging.sendDebugMsg("Table WORLDS is updated!");

        TownyMessaging.sendDebugMsg("Checking done!");
    }

    /**
     * Call after loading to remove any old database elements we no longer need.
     *
     * @param cntx
     * @param db_name
     */
    public static void cleanup(Connection cntx, String db_name) {

		/*
		 * Update RESIDENTS.
		 */
        String resident_update;

        try {

            resident_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "RESIDENTS` "
                    + "DROP COLUMN `townBlocks`";

            Statement s = cntx.createStatement();
            s.executeUpdate(resident_update);

            TownyMessaging.sendDebugMsg("Table RESIDENTS is updated!");

        } catch (SQLException ee) {

            if (ee.getErrorCode() != 1060)
                TownyMessaging.sendErrorMsg("Error updating table RESIDENTS :" + ee.getMessage());

        }

		/*
		 * Update TOWNS.
		 */
        String towns_update;

        try {

            towns_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "TOWNS` "
                    + "DROP COLUMN `townBlocks`";

            Statement s = cntx.createStatement();
            s.executeUpdate(towns_update);

            TownyMessaging.sendDebugMsg("Table TOWNS is updated!");

        } catch (SQLException ee) {

            if (ee.getErrorCode() != 1060)
                TownyMessaging.sendErrorMsg("Error updating table TOWNS :" + ee.getMessage());
        }
    }

    /**
     * @author - Articdive Note: I've never had this much pain with sql in years
     * @param s e.getTOWNS, e.getRESIDENTS , e.getWORLDS, e.getNATIONS, e.getTOWNBLOCKS (ONLY THESE)!
     * @return List of All Variables to be used in an ADD COLUMN method i.e updatetown
     */

    private static List<String> getValuesFromDefaultSchema(String s) {
        List<String> columns = new ArrayList<String>();
        if (s.startsWith("CREATE")) {
            String[] parts = s.split(",");
            for (String part : parts) {
                // This will only work considering only one of these happens ,there will always be a residents, towns,
                // worlds, nations or townblocks database running through it, Sending any other string will return null!
                // ONLY PARSE this.getWORLDS, this.getRESIDENTS, this.getTOWNS, this.getNATIONS & this.getTOWNBLOCKS
                // NOTHING ELSE!
                String replaced = "";
                String replaced1 = part.replaceAll("CREATE TABLE IF NOT EXISTS " + tb_prefix + "RESIDENTS \\(", "");
                String replaced2 = replaced1.replaceAll("CREATE TABLE IF NOT EXISTS " + tb_prefix + "TOWNS \\(", "");
                String replaced3 = replaced2.replaceAll("CREATE TABLE IF NOT EXISTS " + tb_prefix + "WORLDS \\(", "");
                String replaced4 = replaced3.replaceAll("CREATE TABLE IF NOT EXISTS " + tb_prefix + "NATIONS \\(", "");
                replaced = replaced4.replaceAll("CREATE TABLE IF NOT EXISTS " + tb_prefix + "TOWNBLOCKS \\(", "");
                // This literally just checks if CREATE TABLES IF NOT EXISTS IS ANYWHERE IN THE SCRIPT thats it,
                // I know its a complicated way to do it but it makes sense xD, plus i think i've commented enough
                // So people understand what it does!
                String parted = "";
                String parted1 = replaced.replaceAll("PRIMARY KEY \\(`name`\\)\\)", "");
                String parted2 = parted1.replaceAll("PRIMARY KEY \\(`world`", "");
                if (parted2.length() <= 3) { // Since `x` is used once in the normal db, we make sure it doesnt have
                    // extra arguements by making it shorter than 3
                    parted2 = parted2.replaceAll("`x`", "");
                }
                parted = parted2.replaceAll("`z`\\)\\)", "");
                boolean send = true;
                if (parted.isEmpty()) { // Removed the empty lines created by the replaceall methods above
                    send = false;
                }
                if (!parted.startsWith(" ")) {
                    parted = " " + parted;
                }
                if (send) {
                    columns.add(parted);
                }
            }
        }
        return columns;
    }
}