package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ElgarL
 */
public class SQL_Schema {

    private static final String tb_prefix = TownySettings.getSQLTablePrefix().toUpperCase();

	private static String getJAILS() {
		return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "JAILS ("
			+ "`uuid` VARCHAR(36) NOT NULL,"
			+ "PRIMARY KEY (`uuid`)" + ")";
	}

	private static List<String> getJailsColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`townBlock` mediumtext NOT NULL");
		columns.add("`spawns`  mediumtext DEFAULT NULL");
		return columns;
	}

	private static String getPLOTGROUPS() {
		return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "PLOTGROUPS ("
			+ "`uuid` VARCHAR(36) NOT NULL,"
			+ "PRIMARY KEY (`uuid`)" + ")";
	}

	private static List<String> getPlotGroupColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`groupName` mediumtext NOT NULL");
		columns.add("`groupPrice` float DEFAULT NULL");
		columns.add("`town` VARCHAR(36) NOT NULL");
		return columns;
	}

	private static String getRESIDENTS() {
		return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "RESIDENTS (" 
			+ " `uuid` VARCHAR(36) NOT NULL,"
			+ "PRIMARY KEY (`uuid`)" + ")";
	}

	private static List<String> getResidentColumns(){
		List<String> columns = new ArrayList<>();
		columns.add("`name` VARCHAR(32) NOT NULL");
		columns.add("`town` VARCHAR(36) DEFAULT NULL");
		columns.add("`town-ranks` mediumtext");
		columns.add("`nation-ranks` mediumtext");
		columns.add("`lastOnline` BIGINT NOT NULL");
		columns.add("`registered` BIGINT NOT NULL");
		columns.add("`joinedTownAt` BIGINT NOT NULL");
		columns.add("`isNPC` bool NOT NULL DEFAULT '0'");
		columns.add("`jailUUID` VARCHAR(36) DEFAULT NULL");
		columns.add("`jailCell` mediumint");
		columns.add("`jailHours` mediumint");
		columns.add("`title` mediumtext");
		columns.add("`surname` mediumtext");
		columns.add("`protectionStatus` mediumtext");
		columns.add("`friends` mediumtext");
		columns.add("`metadata` text DEFAULT NULL");
		columns.add("`uuid` VARCHAR(36) NOT NULL");
		return columns;
	}

	private static String getHIBERNATEDRESIDENTS() {
		return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "HIBERNATEDRESIDENTS ("
			+ "`uuid` VARCHAR(36) NOT NULL,"
			+ "PRIMARY KEY (`uuid`)" + ")";
	}

	private static List<String> getHibernatedResidentsColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`registered` BIGINT DEFAULT NULL");
		return columns;
	}

	private static String getTOWNS() {
		return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "TOWNS (" 
			+ "`uuid` VARCHAR(36) NOT NULL,"
			+ "PRIMARY KEY (`uuid`)" + ")";
	}

	private static List<String> getTownColumns() {
	List<String> columns = new ArrayList<>();
		columns.add("`name` VARCHAR(32) NOT NULL");
		columns.add("`mayor` VARCHAR(36) NOT NULL");
		columns.add("`nation` VARCHAR(36) NOT NULL");
		columns.add("`assistants` text DEFAULT NULL");
		columns.add("`townBoard` mediumtext DEFAULT NULL");
		columns.add("`tag` mediumtext DEFAULT NULL");
		columns.add("`protectionStatus` mediumtext DEFAULT NULL");
		columns.add("`bonus` int(11) DEFAULT 0");
		columns.add("`purchased` int(11)  DEFAULT 0");
		columns.add("`taxpercent` bool NOT NULL DEFAULT '0'");
		columns.add("`maxPercentTaxAmount` float DEFAULT NULL");
		columns.add("`taxes` float DEFAULT 0");
		columns.add("`hasUpkeep` bool NOT NULL DEFAULT '0'");
		columns.add("`plotPrice` float DEFAULT NULL");
		columns.add("`plotTax` float DEFAULT NULL");
		columns.add("`commercialPlotPrice` float DEFAULT NULL");
		columns.add("`commercialPlotTax` float NOT NULL");
		columns.add("`embassyPlotPrice` float NOT NULL");
		columns.add("`embassyPlotTax` float NOT NULL");
		columns.add("`open` bool NOT NULL DEFAULT '0'");
		columns.add("`public` bool NOT NULL DEFAULT '0'");
		columns.add("`admindisabledpvp` bool NOT NULL DEFAULT '0'");
		columns.add("`adminenabledpvp` bool NOT NULL DEFAULT '0'");
		columns.add("`homeblock` mediumtext NOT NULL");
		columns.add("`spawn` mediumtext NOT NULL");
		columns.add("`outpostSpawns` mediumtext DEFAULT NULL");
		columns.add("`jailSpawns` mediumtext DEFAULT NULL");
		columns.add("`outlaws` mediumtext DEFAULT NULL");
		columns.add("`uuid` VARCHAR(36) DEFAULT NULL");
		columns.add("`registered` BIGINT DEFAULT NULL");
		columns.add("`spawnCost` float NOT NULL");
		columns.add("`mapColorHexCode` mediumtext DEFAULT NULL");
		columns.add("`metadata` text DEFAULT NULL");
		columns.add("`conqueredDays` mediumint");
		columns.add("`conquered` bool NOT NULL DEFAULT '0'");
		columns.add("`ruined` bool NOT NULL DEFAULT '0'");
		columns.add("`ruinedTime` BIGINT DEFAULT '0'");
		columns.add("`neutral` bool NOT NULL DEFAULT '0'");
		columns.add("`debtBalance` float NOT NULL");
		columns.add("`joinedNationAt` BIGINT NOT NULL");
		columns.add("`primaryJail` VARCHAR(36) DEFAULT NULL");
		columns.add("`movedHomeBlockAt` BIGINT NOT NULL");
		columns.add("`trustedResidents` mediumtext DEFAULT NULL");
		columns.add("`nationZoneOverride` int(11) DEFAULT 0");
		columns.add("`nationZoneEnabled` bool NOT NULL DEFAULT '1'");
		columns.add("`allies` mediumtext NOT NULL");
		columns.add("`enemies` mediumtext NOT NULL");
		columns.add("`hasUnlimitedClaims` bool NOT NULL DEFAULT '0'");
		columns.add("`manualTownLevel` BIGINT DEFAULT '-1'");
		return columns;
	}

	private static String getNATIONS() {
		return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "NATIONS (" 
			+ "`uuid` VARCHAR(36) NOT NULL,"
			+ "PRIMARY KEY (`uuid`)" + ")";
	}

	private static List<String> getNationColumns(){
		List<String> columns = new ArrayList<>();
		columns.add("`name` VARCHAR(32) NOT NULL");
		columns.add("`capital` VARCHAR(36) NOT NULL");
		columns.add("`tag` mediumtext NOT NULL");
		columns.add("`allies` mediumtext NOT NULL");
		columns.add("`enemies` mediumtext NOT NULL");
		columns.add("`taxes` float NOT NULL");
		columns.add("`spawnCost` float NOT NULL");
		columns.add("`neutral` bool NOT NULL DEFAULT '0'");
		columns.add("`uuid` VARCHAR(36) DEFAULT NULL");
		columns.add("`registered` BIGINT DEFAULT NULL");
		columns.add("`nationBoard` mediumtext DEFAULT NULL");
		columns.add("`mapColorHexCode` mediumtext DEFAULT NULL");
		columns.add("`nationSpawn` mediumtext DEFAULT NULL");
		columns.add("`isPublic` bool NOT NULL DEFAULT '1'");
		columns.add("`isOpen` bool NOT NULL DEFAULT '1'");
		columns.add("`metadata` text DEFAULT NULL");
		return columns;
	}

	private static String getWORLDS() {
		return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "WORLDS ("
			+ "`uuid` VARCHAR(36) NOT NULL,"
			+ "PRIMARY KEY (`uuid`)" + ")";
	}

	private static List<String> getWorldColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`name` VARCHAR(32) NOT NULL");
		columns.add("`claimable` bool NOT NULL DEFAULT '0'");
		columns.add("`pvp` bool NOT NULL DEFAULT '0'");
		columns.add("`forcepvp` bool NOT NULL DEFAULT '0'");
		columns.add("`forcetownmobs` bool NOT NULL DEFAULT '0'");
		columns.add("`friendlyFire` bool NOT NULL DEFAULT '0'");
		columns.add("`worldmobs` bool NOT NULL DEFAULT '0'");
		columns.add("`wildernessmobs` bool NOT NULL DEFAULT '0'");
		columns.add("`firespread` bool NOT NULL DEFAULT '0'");
		columns.add("`forcefirespread` bool NOT NULL DEFAULT '0'");
		columns.add("`explosions` bool NOT NULL DEFAULT '0'");
		columns.add("`forceexplosions` bool NOT NULL DEFAULT '0'");
		columns.add("`endermanprotect` bool NOT NULL DEFAULT '0'");
		columns.add("`disablecreaturetrample` bool NOT NULL DEFAULT '0'");
		columns.add("`unclaimedZoneBuild` bool NOT NULL DEFAULT '0'");
		columns.add("`unclaimedZoneDestroy` bool NOT NULL DEFAULT '0'");
		columns.add("`unclaimedZoneSwitch` bool NOT NULL DEFAULT '0'");
		columns.add("`unclaimedZoneItemUse` bool NOT NULL DEFAULT '0'");
		columns.add("`unclaimedZoneName` mediumtext NOT NULL");
		columns.add("`unclaimedZoneIgnoreIds` mediumtext NOT NULL");
		columns.add("`usingPlotManagementDelete` bool NOT NULL DEFAULT '0'");
		columns.add("`plotManagementDeleteIds` mediumtext NOT NULL");
		columns.add("`isDeletingEntitiesOnUnclaim` bool NOT NULL DEFAULT '0'");
		columns.add("`unclaimDeleteEntityTypes` mediumtext NOT NULL");
		columns.add("`usingPlotManagementMayorDelete` bool NOT NULL DEFAULT '0'");
		columns.add("`plotManagementMayorDelete` mediumtext NOT NULL");
		columns.add("`usingPlotManagementRevert` bool NOT NULL DEFAULT '0'");
		columns.add("`plotManagementIgnoreIds` mediumtext NOT NULL");
		columns.add("`usingPlotManagementWildRegen` bool NOT NULL DEFAULT '0'");
		columns.add("`plotManagementWildRegenEntities` mediumtext NOT NULL");
		columns.add("`plotManagementWildRegenBlockWhitelist` mediumtext NOT NULL");
		columns.add("`plotManagementWildRegenSpeed` long NOT NULL");
		columns.add("`usingPlotManagementWildRegenBlocks` bool NOT NULL DEFAULT '0'");
		columns.add("`plotManagementWildRegenBlocks` mediumtext NOT NULL");		
		columns.add("`usingTowny` bool NOT NULL DEFAULT '0'");
		columns.add("`warAllowed` bool NOT NULL DEFAULT '0'");
		columns.add("`metadata` text DEFAULT NULL");
		return columns;
	}

	private static String getTOWNBLOCKS() {
		return "CREATE TABLE IF NOT EXISTS " + tb_prefix + "TOWNBLOCKS (" 
			+ "`world` VARCHAR(36) NOT NULL,"
			+ "`x` mediumint NOT NULL," + "`z` mediumint NOT NULL," + "PRIMARY KEY (`world`,`x`,`z`)" + ")";
	}

	private static List<String> getTownBlockColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`name` mediumtext");
		columns.add("`price` float DEFAULT '-1'");
		columns.add("`town` mediumtext");
		columns.add("`resident` mediumtext");
		columns.add("`type` TINYINT NOT  NULL DEFAULT '0'");
		columns.add("`typeName` mediumtext");
		columns.add("`outpost` bool NOT NULL DEFAULT '0'");
		columns.add("`permissions` mediumtext NOT NULL");
		columns.add("`locked` bool NOT NULL DEFAULT '0'");
		columns.add("`changed` bool NOT NULL DEFAULT '0'");
		columns.add("`metadata` text DEFAULT NULL");
		columns.add("`groupID` VARCHAR(36) DEFAULT NULL");
		columns.add("`claimedAt` BIGINT NOT NULL");
		columns.add("`trustedResidents` mediumtext DEFAULT NULL");
		columns.add("`customPermissionData` mediumtext DEFAULT NULL");
		return columns;
	}

	/**
	 * Create and update database schema.
	 *
	 * @param cntx    a database connection
	 * @param db_name the name of a database
	 */
	public static void initTables(Connection cntx, String db_name) {

		/*
		 * Fetch WORLDS Table schema.
		 */

		try {
			Statement s = cntx.createStatement();
			s.executeUpdate(getWORLDS());
			TownyMessaging.sendDebugMsg("Table WORLDS is ok!");
		} catch (SQLException ee) {
			TownyMessaging.sendErrorMsg("Error Creating table WORLDS : " + ee.getMessage());
		}

		/*
		 * Add columns to world (if not already there)
		 */

		for (String column : getWorldColumns()) {
			try {
				String world_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "WORLDS` " + "ADD COLUMN "
						+ column;
				PreparedStatement ps = cntx.prepareStatement(world_update);
				ps.executeUpdate();
			} catch (SQLException ee) {
				if (ee.getErrorCode() != 1060)
					TownyMessaging.sendErrorMsg("Error updating table WORLDS :" + ee.getMessage());
			}
		}
		TownyMessaging.sendDebugMsg("Table WORLDS is updated!");

		/*
		 * Fetch NATIONS Table schema.
		 */

		try {
			Statement s = cntx.createStatement();
			s.executeUpdate(getNATIONS());
			TownyMessaging.sendDebugMsg("Table NATIONS is ok!");
		} catch (SQLException ee) {
			TownyMessaging.sendErrorMsg("Error Creating table NATIONS : " + ee.getMessage());
		}

		/*
		 * Add columns to nation (if not already there)
		 */

		for (String column : getNationColumns()) {
			try {
				String nation_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "NATIONS` " + "ADD COLUMN "
						+ column;
				PreparedStatement ps = cntx.prepareStatement(nation_update);
				ps.executeUpdate();
			} catch (SQLException ee) {
				if (ee.getErrorCode() != 1060)
					TownyMessaging.sendErrorMsg("Error updating table NATIONS :" + ee.getMessage());
			}
		}
		TownyMessaging.sendDebugMsg("Table NATIONS is updated!");

		/*
		 * Fetch TOWNS Table schema.
		 */

		try {
			Statement s = cntx.createStatement();
			s.executeUpdate(getTOWNS());
			TownyMessaging.sendDebugMsg("Table TOWNS is ok!");
		} catch (SQLException ee) {
			TownyMessaging.sendErrorMsg("Creating table TOWNS :" + ee.getMessage());
		}

		/*
		 * Add columns to town (if not already there)
		 */

		for (String column : getTownColumns()) {
			try {
				String town_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "TOWNS` " + "ADD COLUMN " + column;
				PreparedStatement ps = cntx.prepareStatement(town_update);
				ps.executeUpdate();

			} catch (SQLException ee) {
				if (ee.getErrorCode() != 1060)
					TownyMessaging.sendErrorMsg("Error updating table TOWNS :" + ee.getMessage());
			}
		}
		TownyMessaging.sendDebugMsg("Table TOWNS is updated!");

		/*
		 * Fetch RESIDENTS Table schema.
		 */

		try {
			Statement s = cntx.createStatement();
			s.executeUpdate(getRESIDENTS());
			TownyMessaging.sendDebugMsg("Table RESIDENTS is ok!");
		} catch (SQLException ee) {
			TownyMessaging.sendErrorMsg("Error Creating table RESIDENTS :" + ee.getMessage());
		}

		/*
		 * Add columns to residents (if not already there)
		 */

		for (String column : getResidentColumns()) {
			try {
				String resident_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "RESIDENTS` " + "ADD COLUMN "
						+ column;
				PreparedStatement ps = cntx.prepareStatement(resident_update);
				ps.executeUpdate();
			} catch (SQLException ee) {
				if (ee.getErrorCode() != 1060)
					TownyMessaging.sendErrorMsg("Error updating table RESIDENTS :" + ee.getMessage());
			}
		}
		TownyMessaging.sendDebugMsg("Table RESIDENTS is updated!");

		/*
		 * Fetch TOWNBLOCKS Table schema.
		 */

		try {
			Statement s = cntx.createStatement();
			s.executeUpdate(getTOWNBLOCKS());
			TownyMessaging.sendDebugMsg("Table TOWNBLOCKS is ok!");
		} catch (SQLException ee) {
			TownyMessaging.sendErrorMsg("Error Creating table TOWNBLOCKS : " + ee.getMessage());
		}

		/*
		 * Add columns to townblocks (if not already there)
		 */

		for (String column : getTownBlockColumns()) {
			try {
				String townblocks_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "TOWNBLOCKS` "
						+ "ADD COLUMN " + column;
				PreparedStatement ps = cntx.prepareStatement(townblocks_update);
				ps.executeUpdate();
			} catch (SQLException ee) {
				if (ee.getErrorCode() != 1060)
					TownyMessaging.sendErrorMsg("Error updating table TOWNBLOCKS :" + ee.getMessage());
			}
		}
		TownyMessaging.sendDebugMsg("Table TOWNBLOCKS is updated!");

		/*
		 * Fetch PLOTGROUPS table schema
		 */

		try {
			Statement s = cntx.createStatement();
			s.executeUpdate(getPLOTGROUPS());
			TownyMessaging.sendDebugMsg("Table PLOTGROUPS is ok!");
		} catch (SQLException ee) {
			TownyMessaging.sendErrorMsg("Error Creating table PLOTGROUPS : " + ee.getMessage());
		}

		/*
		 * Add columns to plotgroups (if not already there)
		 */

		for (String column : getPlotGroupColumns()) {
			try {
				String plotGroups_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "PLOTGROUPS` "
						+ "ADD COLUMN " + column;
				PreparedStatement ps = cntx.prepareStatement(plotGroups_update);
				ps.executeUpdate();
			} catch (SQLException ee) {
				if (ee.getErrorCode() != 1060)
					TownyMessaging.sendErrorMsg("Error updating table PLOTGROUPS :" + ee.getMessage());
			}
			TownyMessaging.sendDebugMsg("Table PLOTGROUPS is updated!");
		}

		/*
		 * Fetch JAILS Table schema.
		 */

		try {
			Statement s = cntx.createStatement();
			s.executeUpdate(getJAILS());
			TownyMessaging.sendDebugMsg("Table JAILS is ok!");
		} catch (SQLException ee) {
			TownyMessaging.sendErrorMsg("Creating table JAILS :" + ee.getMessage());
		}

		/*
		 * Add columns to jails (if not already there)
		 */

		for (String column : getJailsColumns()) {
			try {
				String jail_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "JAILS` " + "ADD COLUMN " + column;
				PreparedStatement ps = cntx.prepareStatement(jail_update);
				ps.executeUpdate();
			} catch (SQLException ee) {
				if (ee.getErrorCode() != 1060)
					TownyMessaging.sendErrorMsg("Error updating table JAILS :" + ee.getMessage());
			}
		}
		TownyMessaging.sendDebugMsg("Table JAILS is updated!");

		/*
		 * Fetch HIBERNATEDRESIDENTS Table schema.
		 */

		try {
			Statement s = cntx.createStatement();
			s.executeUpdate(getHIBERNATEDRESIDENTS());
			TownyMessaging.sendDebugMsg("Table JAILS is ok!");
		} catch (SQLException ee) {
			TownyMessaging.sendErrorMsg("Creating table JAILS :" + ee.getMessage());
		}

		/*
		 * Add columns to hibernated residents (if not already there)
		 */

		for (String column : getHibernatedResidentsColumns()) {
			try {
				String hibres_update = "ALTER TABLE `" + db_name + "`.`" + tb_prefix + "HIBERNATEDRESIDENTS` "
						+ "ADD COLUMN " + column;
				PreparedStatement ps = cntx.prepareStatement(hibres_update);
				ps.executeUpdate();
			} catch (SQLException ee) {
				if (ee.getErrorCode() != 1060)
					TownyMessaging.sendErrorMsg("Error updating table HIBERNATEDRESIDENTS :" + ee.getMessage());
			}
		}
		TownyMessaging.sendDebugMsg("Table HIBERNATEDRESIDENTS is updated!");

	}
    
    /**
     * Call after loading to remove any old database elements we no longer need.
     *
     * @param cntx - Connection.
     * @param db_name - Name of database.
     */
    public static void cleanup(Connection cntx, String db_name) {
    	
    	List<ColumnUpdate> cleanups = new ArrayList<ColumnUpdate>();
    	cleanups.add(ColumnUpdate.of("TOWNS", "residents"));
    	cleanups.add(ColumnUpdate.of("NATIONS", "assistants"));
    	cleanups.add(ColumnUpdate.of("NATIONS", "towns"));
    	cleanups.add(ColumnUpdate.of("WORLDS", "towns"));
    	cleanups.add(ColumnUpdate.of("WORLDS", "plotManagementRevertSpeed"));
    	cleanups.add(ColumnUpdate.of("PLOTGROUPS", "claimedAt"));
    	cleanups.add(ColumnUpdate.of("RESIDENTS", "isJailed"));
    	cleanups.add(ColumnUpdate.of("RESIDENTS", "JailSpawn"));
    	cleanups.add(ColumnUpdate.of("RESIDENTS", "JailDays"));
    	cleanups.add(ColumnUpdate.of("RESIDENTS", "JailTown"));
    	cleanups.add(ColumnUpdate.of("TOWNS", "jailSpawns"));
    	cleanups.add(ColumnUpdate.of("WORLDS", "disableplayertrample"));

    	for (ColumnUpdate update : cleanups)
    		dropColumn(cntx, db_name, update.getTable(), update.getColumn());
    }
    
    /**
     * Drops the given column from the given table, if the column is present.
     * 
     * @param cntx database connection.
     * @param db_name database name.
     * @param table table name.
     * @param column column to drop from the given table.
     */
    private static void dropColumn(Connection cntx, String db_name, String table, String column) {
    	String update;
    	
    	try {
    		DatabaseMetaData md = cntx.getMetaData();
        	ResultSet rs = md.getColumns(null, null, table, column);
        	if (!rs.next())
        		return;
        	
    		update = "ALTER TABLE `" + db_name + "`.`" + table + "` DROP COLUMN `" + column + "`";
    		
    		Statement s = cntx.createStatement();
    		s.executeUpdate(update);
    		
    		TownyMessaging.sendDebugMsg("Table " + table + " has dropped the " + column + " column.");
        	
    	} catch (SQLException ee) {
    		if (ee.getErrorCode() != 1060)
    			TownyMessaging.sendErrorMsg("Error updating table " + table + ":" + ee.getMessage());
    	}
    }
    
    private static class ColumnUpdate {
    	private String table;
    	private String column;
    	
    	private ColumnUpdate(String table, String column) {
    		this.table = SQL_Schema.tb_prefix + table;
    		this.column = column;
    	}
    	    	
    	private String getTable() {
    		return this.table;
    	}
    	
    	private String getColumn() {
    		return this.column;
    	}
    	
    	private static ColumnUpdate of(String table, String column) {
    		return new ColumnUpdate(table, column);
    	}
    }
 }
