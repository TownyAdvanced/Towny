package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.db.TownySQLSource.TownyDBTableType;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author ElgarL
 */
public class SQLSchema {

	private static final String SQLDB_NAME = TownySettings.getSQLDBName();
	private static final String TABLE_PREFIX = TownySettings.getSQLTablePrefix().toUpperCase(Locale.ROOT);
	private static final int MYSQL_DUPLICATE_COLUMN_ERR = 1060;

	/**
	 * Create and update database schema.
	 *
	 * @param cntx    a database connection
	 */
	public static void initTables(Connection cntx) {

		initTable(cntx, TownyDBTableType.WORLD);
		updateTable(cntx, TownyDBTableType.WORLD, getWorldColumns());

		initTable(cntx, TownyDBTableType.NATION);
		updateTable(cntx, TownyDBTableType.NATION, getNationColumns());

		initTable(cntx, TownyDBTableType.TOWN);
		updateTable(cntx, TownyDBTableType.TOWN, getTownColumns());

		initTable(cntx, TownyDBTableType.RESIDENT);
		updateTable(cntx, TownyDBTableType.RESIDENT, getResidentColumns());

		initTable(cntx, TownyDBTableType.TOWNBLOCK);
		updateTable(cntx, TownyDBTableType.TOWNBLOCK, getTownBlockColumns());

		initTable(cntx, TownyDBTableType.PLOTGROUP);
		updateTable(cntx, TownyDBTableType.PLOTGROUP, getPlotGroupColumns());

		initTable(cntx, TownyDBTableType.DISTRICT);
		updateTable(cntx, TownyDBTableType.DISTRICT, getDistrictColumns());

		initTable(cntx, TownyDBTableType.JAIL);
		updateTable(cntx, TownyDBTableType.JAIL, getJailsColumns());

		initTable(cntx, TownyDBTableType.HIBERNATED_RESIDENT);
		updateTable(cntx, TownyDBTableType.HIBERNATED_RESIDENT, getHibernatedResidentsColumns());
		
		initTable(cntx, TownyDBTableType.COOLDOWN);
		updateTable(cntx, TownyDBTableType.COOLDOWN, getCooldownColumns());
	}

	/*
	 * Check that the tables are created.
	 */
	private static void initTable(Connection cntx, TownyDBTableType tableType) {
		try (Statement s = cntx.createStatement()) {
			s.executeUpdate(fetchTableSchema(tableType));
			TownyMessaging.sendDebugMsg("Table " + tableType.tableName() + " is ok!");
		} catch (SQLException ee) {
			TownyMessaging.sendErrorMsg("Error Creating table " + tableType.tableName() + " : " + ee.getMessage());
		}
	}

	/*
	 * Get create table slug for the given TownyDBTableType. TownBlocks are keyed differently.
	 */
	private static String fetchTableSchema(TownyDBTableType tableType) {
		return switch(tableType) {
			case TOWNBLOCK -> fetchCreateTownBlocksStatement();
			case JAIL -> fetchCreateUUIDStatement(tableType);
			case PLOTGROUP -> fetchCreatePlotGroupStatement(tableType);
			case DISTRICT -> fetchCreateUUIDStatement(tableType);
			case COOLDOWN -> fetchCreateCooldownsStatement(tableType);
			case WORLD -> fetchCreateWorldStatemnt(tableType);
			case HIBERNATED_RESIDENT -> fetchCreateUUIDStatement(tableType);
			default -> fetchCreateNamedStatement(tableType);
		};
	}

	/*
	 * Generic create table statement for the Name keyed TownyDBTableTypes
	 */
	private static String fetchCreateNamedStatement(TownyDBTableType tableType) {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_PREFIX + tableType.tableName() + " (`name` VARCHAR(32) NOT NULL,PRIMARY KEY (`name`))";
	}

	/*
	 * Create table statement for the PlotGroups that originally got keyed with groupID
	 */
	private static String fetchCreatePlotGroupStatement(TownyDBTableType tableType) {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_PREFIX + tableType.tableName() + " (`groupID` VARCHAR(36) NOT NULL,PRIMARY KEY (`groupID`))";
	}

	/*
	 * Generic create table statement for the UUID keyed TownyDBTableTypes
	 */
	private static String fetchCreateUUIDStatement(TownyDBTableType tableType) {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_PREFIX + tableType.tableName() + " (`uuid` VARCHAR(36) NOT NULL,PRIMARY KEY (`uuid`))";
	}

	/*
	 * Special create table statement for the TownBlock TownyDBTableType
	 */
	private static String fetchCreateTownBlocksStatement() {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_PREFIX + "TOWNBLOCKS (`world` VARCHAR(36) NOT NULL,`x` mediumint NOT NULL,`z` mediumint NOT NULL,PRIMARY KEY (`world`,`x`,`z`))";
	}
	
	private static String fetchCreateCooldownsStatement(TownyDBTableType tableType) {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_PREFIX + tableType.tableName() + " (`key` varchar(200) not null, primary key (`key`))";
	}

	/*
	 * Create table statement for the TownyWorld, with a larger varchar.
	 */
	private static String fetchCreateWorldStatemnt(TownyDBTableType tableType) {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_PREFIX + tableType.tableName() + " (`name` VARCHAR(64) NOT NULL,PRIMARY KEY (`name`))";
	}

	/*
	 * Update a table in the database to make sure that each column is present. 
	 */
	private static void updateTable(Connection cntx, TownyDBTableType tableType, List<String> columns) {
		String update = "ALTER TABLE `" + SQLDB_NAME + "`.`" + TABLE_PREFIX + tableType.tableName() + "` ADD COLUMN ";
		for (String column : columns) {
			try (PreparedStatement ps = cntx.prepareStatement(update + column)) {
				ps.executeUpdate();
			} catch (SQLException ee) {
				if (ee.getErrorCode() != MYSQL_DUPLICATE_COLUMN_ERR)
					TownyMessaging.sendErrorMsg("Error updating table " + tableType.tableName() + ":" + ee.getMessage());
			}
		}
		TownyMessaging.sendDebugMsg("Table " + tableType.tableName() + " is updated!");
	}

	/*
	 * Columns of each Object type follow:
	 */
	
	private static List<String> getJailsColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`townBlock` mediumtext NOT NULL");
		columns.add("`spawns`  mediumtext DEFAULT NULL");
		return columns;
	}

	private static List<String> getPlotGroupColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`groupName` mediumtext NOT NULL");
		columns.add("`groupPrice` float DEFAULT NULL");
		columns.add("`town` VARCHAR(32) NOT NULL");
		columns.add("`metadata` text DEFAULT NULL");
		return columns;
	}

	private static List<String> getDistrictColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`districtName` mediumtext NOT NULL");
		columns.add("`town` VARCHAR(36) NOT NULL");
		columns.add("`metadata` text DEFAULT NULL");
		return columns;
	}

	private static List<String> getResidentColumns(){
		List<String> columns = new ArrayList<>();
		columns.add("`town` mediumtext");
		columns.add("`town-ranks` mediumtext");
		columns.add("`nation-ranks` mediumtext");
		columns.add("`lastOnline` BIGINT NOT NULL");
		columns.add("`registered` BIGINT NOT NULL");
		columns.add("`joinedTownAt` BIGINT NOT NULL");
		columns.add("`isNPC` bool NOT NULL DEFAULT '0'");
		columns.add("`jailUUID` VARCHAR(36) DEFAULT NULL");
		columns.add("`jailCell` mediumint");
		columns.add("`jailHours` mediumint");
		columns.add("`jailBail` float DEFAULT NULL");
		columns.add("`title` mediumtext");
		columns.add("`surname` mediumtext");
		columns.add("`protectionStatus` mediumtext");
		columns.add("`friends` mediumtext");
		columns.add("`metadata` text DEFAULT NULL");
		columns.add("`uuid` VARCHAR(36) NOT NULL");
		columns.add("`about` mediumtext DEFAULT NULL");
		return columns;
	}

	private static List<String> getHibernatedResidentsColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`registered` BIGINT DEFAULT NULL");
		return columns;
	}

	private static List<String> getTownColumns() {
	List<String> columns = new ArrayList<>();
		columns.add("`mayor` mediumtext");
		columns.add("`nation` mediumtext");
		columns.add("`townBoard` mediumtext DEFAULT NULL");
		columns.add("`tag` mediumtext DEFAULT NULL");
		columns.add("`founder` mediumtext DEFAULT NULL");
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
		columns.add("`adminEnabledMobs` bool NOT NULL DEFAULT '0'");
		columns.add("`admindisabledpvp` bool NOT NULL DEFAULT '0'");
		columns.add("`adminenabledpvp` bool NOT NULL DEFAULT '0'");
		columns.add("`allowedToWar` bool NOT NULL DEFAULT '1'");
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
		columns.add("`trustedTowns` mediumtext NOT NULL");
		columns.add("`nationZoneOverride` int(11) DEFAULT 0");
		columns.add("`nationZoneEnabled` bool NOT NULL DEFAULT '1'");
		columns.add("`allies` mediumtext NOT NULL");
		columns.add("`enemies` mediumtext NOT NULL");
		columns.add("`hasUnlimitedClaims` bool NOT NULL DEFAULT '0'");
		columns.add("`manualTownLevel` BIGINT DEFAULT '-1'");
		columns.add("`forSale` bool NOT NULL DEFAULT '0'");
		columns.add("`forSalePrice` float NOT NULL");
		columns.add("`forSaleTime` BIGINT DEFAULT '0'");
		columns.add("`visibleOnTopLists` bool NOT NULL DEFAULT '1'");
		
		return columns;
	}

	private static List<String> getNationColumns(){
		List<String> columns = new ArrayList<>();
		columns.add("`capital` mediumtext NOT NULL");
		columns.add("`tag` mediumtext NOT NULL");
		columns.add("`allies` mediumtext NOT NULL");
		columns.add("`enemies` mediumtext NOT NULL");
		columns.add("`taxes` float NOT NULL");
		columns.add("`taxpercent` bool NOT NULL DEFAULT '0'");
		columns.add("`maxPercentTaxAmount` float DEFAULT NULL");
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
		columns.add("`conqueredTax` float NOT NULL");
		columns.add("`sanctionedTowns` mediumtext DEFAULT NULL");
		return columns;
	}

	private static List<String> getWorldColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`uuid` VARCHAR(36) DEFAULT NULL");
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
		columns.add("`revertOnUnclaimWhitelistMaterials` mediumtext NOT NULL");
		columns.add("`usingPlotManagementWildRegen` bool NOT NULL DEFAULT '0'");
		columns.add("`plotManagementWildRegenEntities` mediumtext NOT NULL");
		columns.add("`plotManagementWildRegenBlockWhitelist` mediumtext NOT NULL");
		columns.add("`wildRegenBlocksToNotOverwrite` mediumtext NOT NULL");
		columns.add("`plotManagementWildRegenSpeed` long NOT NULL");
		columns.add("`usingPlotManagementWildRegenBlocks` bool NOT NULL DEFAULT '0'");
		columns.add("`plotManagementWildRegenBlocks` mediumtext NOT NULL");		
		columns.add("`usingTowny` bool NOT NULL DEFAULT '0'");
		columns.add("`warAllowed` bool NOT NULL DEFAULT '0'");
		columns.add("`metadata` text DEFAULT NULL");
		columns.add("`jailing` bool NOT NULL DEFAULT '1'");
		return columns;
	}

	private static List<String> getTownBlockColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`name` mediumtext");
		columns.add("`price` float DEFAULT '-1'");
		columns.add("`taxed` bool NOT NULL DEFAULT '1'");
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
		columns.add("`districtID` VARCHAR(36) DEFAULT NULL");
		columns.add("`claimedAt` BIGINT NOT NULL");
		columns.add("`trustedResidents` mediumtext DEFAULT NULL");
		columns.add("`customPermissionData` mediumtext DEFAULT NULL");
		columns.add("`minTownMembershipDays` SMALLINT NOT NULL DEFAULT '-1'");
		columns.add("`maxTownMembershipDays` SMALLINT NOT NULL DEFAULT '-1'");
		return columns;
	}
	
	private static List<String> getCooldownColumns() {
		List<String> columns = new ArrayList<>();
		columns.add("`key` varchar(200) NOT NULL");
		columns.add("`expiry` BIGINT NOT NULL");
		return columns;
	}

	/**
	 * Call after loading to remove any old database elements we no longer need.
	 *
	 * @param connection A connection to the database.
	 */
	public static void cleanup(Connection connection) {

		List<ColumnUpdate> cleanups = new ArrayList<>();
		cleanups.add(ColumnUpdate.update("TOWNS", "residents"));
		cleanups.add(ColumnUpdate.update("NATIONS", "assistants"));
		cleanups.add(ColumnUpdate.update("NATIONS", "towns"));
		cleanups.add(ColumnUpdate.update("WORLDS", "towns"));
		cleanups.add(ColumnUpdate.update("WORLDS", "plotManagementRevertSpeed"));
		cleanups.add(ColumnUpdate.update("PLOTGROUPS", "claimedAt"));
		cleanups.add(ColumnUpdate.update("RESIDENTS", "isJailed"));
		cleanups.add(ColumnUpdate.update("RESIDENTS", "JailSpawn"));
		cleanups.add(ColumnUpdate.update("RESIDENTS", "JailDays"));
		cleanups.add(ColumnUpdate.update("RESIDENTS", "JailTown"));
		cleanups.add(ColumnUpdate.update("TOWNS", "jailSpawns"));
		cleanups.add(ColumnUpdate.update("WORLDS", "disableplayertrample"));
		cleanups.add(ColumnUpdate.update("TOWNS", "assistants"));

		for (ColumnUpdate update : cleanups)
			dropColumn(connection, update.table(), update.column());
	}

	/**
	 * Drops the given column from the given table, if the column is present.
	 * 
	 * @param cntx    database connection.
	 * @param table   table name.
	 * @param column  column to drop from the given table.
	 */
	private static void dropColumn(Connection cntx, String table, String column) {
		String update;

		try (Statement s = cntx.createStatement()) {
			DatabaseMetaData md = cntx.getMetaData();
			ResultSet rs = md.getColumns(null, null, table, column);
			if (!rs.next())
				return;

			update = "ALTER TABLE `" + SQLDB_NAME + "`.`" + table + "` DROP COLUMN `" + column + "`";

			s.executeUpdate(update);

			TownyMessaging.sendDebugMsg("Table " + table + " has dropped the " + column + " column.");

		} catch (SQLException ee) {
			if (ee.getErrorCode() != MYSQL_DUPLICATE_COLUMN_ERR)
				TownyMessaging.sendErrorMsg("Error updating table " + table + ":" + ee.getMessage());
		}
	}

	private record ColumnUpdate(String table, String column) {
		private static ColumnUpdate update(String table, String column) {
			return new ColumnUpdate(SQLSchema.TABLE_PREFIX + table, column);
		}
	}
}
