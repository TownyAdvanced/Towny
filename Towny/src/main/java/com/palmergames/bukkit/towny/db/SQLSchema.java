package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.db.TownySQLSource.TownyDBTableType;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
		// Ensure the Versioning table exists, so versions of other tables can be checked.
		initTable(cntx, TownyDBTableType.VERSIONING, null);
		// Load the current versions of each table.
		Map<TownyDBTableType, Integer> versions = loadTableVersions(cntx);

		initTable(cntx, TownyDBTableType.WORLD, versions.get(TownyDBTableType.WORLD));
		updateTable(cntx, TownyDBTableType.WORLD, getWorldColumns(), versions.get(TownyDBTableType.WORLD));

		initTable(cntx, TownyDBTableType.NATION, versions.get(TownyDBTableType.NATION));
		updateTable(cntx, TownyDBTableType.NATION, getNationColumns(), versions.get(TownyDBTableType.NATION));

		initTable(cntx, TownyDBTableType.TOWN, versions.get(TownyDBTableType.TOWN));
		updateTable(cntx, TownyDBTableType.TOWN, getTownColumns(), versions.get(TownyDBTableType.TOWN));

		initTable(cntx, TownyDBTableType.RESIDENT, versions.get(TownyDBTableType.RESIDENT));
		updateTable(cntx, TownyDBTableType.RESIDENT, getResidentColumns(), versions.get(TownyDBTableType.RESIDENT));

		initTable(cntx, TownyDBTableType.TOWNBLOCK, versions.get(TownyDBTableType.TOWNBLOCK));
		updateTable(cntx, TownyDBTableType.TOWNBLOCK, getTownBlockColumns(), versions.get(TownyDBTableType.TOWNBLOCK));

		initTable(cntx, TownyDBTableType.PLOTGROUP, versions.get(TownyDBTableType.PLOTGROUP));
		updateTable(cntx, TownyDBTableType.PLOTGROUP, getPlotGroupColumns(), versions.get(TownyDBTableType.PLOTGROUP));

		initTable(cntx, TownyDBTableType.DISTRICT, versions.get(TownyDBTableType.DISTRICT));
		updateTable(cntx, TownyDBTableType.DISTRICT, getDistrictColumns(), versions.get(TownyDBTableType.DISTRICT));

		initTable(cntx, TownyDBTableType.JAIL, versions.get(TownyDBTableType.JAIL));
		updateTable(cntx, TownyDBTableType.JAIL, getJailsColumns(), versions.get(TownyDBTableType.JAIL));

		initTable(cntx, TownyDBTableType.HIBERNATED_RESIDENT, versions.get(TownyDBTableType.HIBERNATED_RESIDENT));
		updateTable(cntx, TownyDBTableType.HIBERNATED_RESIDENT, getHibernatedResidentsColumns(), versions.get(TownyDBTableType.HIBERNATED_RESIDENT));

		initTable(cntx, TownyDBTableType.COOLDOWN, versions.get(TownyDBTableType.COOLDOWN));
		updateTable(cntx, TownyDBTableType.COOLDOWN, getCooldownColumns(), versions.get(TownyDBTableType.COOLDOWN));
	}

	/**
	 * Check that the tables are created.
	 *
	 * @param cntx a database connection
	 * @param tableType the table to initialize
	 * @param latestVersion the latest version of the table, or null if it does not
	 */
	private static void initTable(Connection cntx, TownyDBTableType tableType, Integer latestVersion) {
		if (latestVersion != null) {
			// Assume table exists if it has a version
			return;
		}
		long startTime = System.currentTimeMillis();

		try (Statement s = cntx.createStatement()) {
			s.executeUpdate(fetchTableSchema(tableType));
			long time = System.currentTimeMillis() - startTime;
			TownyMessaging.sendDebugMsg("Table " + tableType.tableName() + " is ok! Took " + time + "ms");
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
			case VERSIONING -> fetchVersioningStatement(tableType);
			default -> fetchCreateNamedStatement(tableType);
		};
	}

	private static void updateOrInsertTableVersion(Connection cntx, TownyDBTableType tableType, int version) {
		try (PreparedStatement ps = cntx.prepareStatement("REPLACE INTO `" + SQLDB_NAME + "`.`" + TABLE_PREFIX + TownyDBTableType.VERSIONING.tableName() + "` (name, version) VALUES (?, ?)")) {
			ps.setString(1, tableType.name());
			ps.setInt(2, version);
			ps.executeUpdate();
		} catch (SQLException ee) {
			TownyMessaging.sendErrorMsg("Error updating table " + tableType.tableName() + " versioning:" + ee.getMessage());
		}
	}

	private static Map<TownyDBTableType, Integer> loadTableVersions(Connection cntx) {
		TownyMessaging.sendDebugMsg("Loading Table Versions");
		Map<TownyDBTableType, Integer> tableVersions = new HashMap<>();
		try {
			Statement s = cntx.createStatement();
			ResultSet rs = s.executeQuery("SELECT name, version FROM " + TABLE_PREFIX + TownyDBTableType.VERSIONING);

			while (rs.next()) {
				try {
					TownyDBTableType name = TownyDBTableType.valueOf(rs.getString("name"));
					int version = rs.getInt("version");
					tableVersions.put(name, version);
				} catch (IllegalArgumentException e) {
					TownyMessaging.sendErrorMsg("Unknown table version entry: " + rs.getString("name"));
				}
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("SQL Error loading towns: " + e.getMessage());
		}
		return tableVersions;
	}

	/*
	 * Create table statement for the Towny Table Versioning.
	 */
	private static String fetchVersioningStatement(TownyDBTableType tableType) {
		return "CREATE TABLE IF NOT EXISTS " + TABLE_PREFIX + tableType.tableName() + " (`name` VARCHAR(32) NOT NULL,`version` VARCHAR(8) DEFAULT NULL,PRIMARY KEY (`name`))";
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
	private static void updateTable(Connection cntx, TownyDBTableType tableType, List<ImmutablePair<Integer, String>> columns, Integer latestVersion) {
		long startTime = System.currentTimeMillis();

		String update = "ALTER TABLE `" + SQLDB_NAME + "`.`" + TABLE_PREFIX + tableType.tableName() + "` ADD COLUMN ";
		for (ImmutablePair<Integer, String> column : columns.stream().filter(col -> latestVersion == null || col.getLeft() > latestVersion).toList()) {
			try (PreparedStatement ps = cntx.prepareStatement(update + column.getRight())) {
				ps.executeUpdate();
			} catch (SQLException ee) {
				if (ee.getErrorCode() != MYSQL_DUPLICATE_COLUMN_ERR)
					TownyMessaging.sendErrorMsg("Error updating table " + tableType.tableName() + ":" + ee.getMessage());
			}
		}
		if (latestVersion == null || latestVersion < tableType.latestVersion()) {
			updateOrInsertTableVersion(cntx, tableType, tableType.latestVersion());
		}
		long time = System.currentTimeMillis() - startTime;
		TownyMessaging.sendDebugMsg("Table " + tableType.tableName() + " is updated! Took " + time + "ms");
	}

	/*
	 * Columns of each Object type follow:
	 */

	private static List<ImmutablePair<Integer, String>> getJailsColumns() {
		// Version, Column Definition. Version is incremented when a column is added and ensure TownyDBTableType.latestVersion() is updated.
		final List<ImmutablePair<Integer, String>> columns = new ArrayList<>();
		columns.add(new ImmutablePair<>(1, "`townBlock` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`spawns`  mediumtext DEFAULT NULL"));
		return columns;
	}

	private static List<ImmutablePair<Integer, String>> getPlotGroupColumns() {
		// Version, Column Definition. Version is incremented when a column is added and ensure TownyDBTableType.latestVersion() is updated.
		final List<ImmutablePair<Integer, String>> columns = new ArrayList<>();
		columns.add(new ImmutablePair<>(1, "`groupName` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`groupPrice` float DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`town` VARCHAR(32) NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`metadata` text DEFAULT NULL"));
		return columns;
	}

	private static List<ImmutablePair<Integer, String>> getDistrictColumns() {
		// Version, Column Definition. Version is incremented when a column is added and ensure TownyDBTableType.latestVersion() is updated.
		final List<ImmutablePair<Integer, String>> columns = new ArrayList<>();
		columns.add(new ImmutablePair<>(1, "`districtName` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`town` VARCHAR(36) NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`metadata` text DEFAULT NULL"));
		return columns;
	}

	private static List<ImmutablePair<Integer, String>> getResidentColumns() {
		// Version, Column Definition. Version is incremented when a column is added and ensure TownyDBTableType.latestVersion() is updated.
		final List<ImmutablePair<Integer, String>> columns = new ArrayList<>();
		columns.add(new ImmutablePair<>(1, "`town` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`town-ranks` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`nation-ranks` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`lastOnline` BIGINT NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`registered` BIGINT NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`joinedTownAt` BIGINT NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`isNPC` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`jailUUID` VARCHAR(36) DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`jailCell` mediumint"));
		columns.add(new ImmutablePair<>(1, "`jailHours` mediumint"));
		columns.add(new ImmutablePair<>(1, "`jailBail` float DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`title` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`surname` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`protectionStatus` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`friends` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`metadata` text DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`uuid` VARCHAR(36) NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`about` mediumtext DEFAULT NULL"));
		return columns;
	}

	private static List<ImmutablePair<Integer, String>> getHibernatedResidentsColumns() {
		// Version, Column Definition. Version is incremented when a column is added and ensure TownyDBTableType.latestVersion() is updated.
		final List<ImmutablePair<Integer, String>> columns = new ArrayList<>();
		columns.add(new ImmutablePair<>(1, "`registered` BIGINT DEFAULT NULL"));
		return columns;
	}

	private static List<ImmutablePair<Integer, String>> getTownColumns() {
		// Version, Column Definition. Version is incremented when a column is added and ensure TownyDBTableType.latestVersion() is updated.
		final List<ImmutablePair<Integer, String>> columns = new ArrayList<>();
		columns.add(new ImmutablePair<>(1, "`mayor` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`nation` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`townBoard` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`tag` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`founder` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`protectionStatus` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`bonus` int(11) DEFAULT 0"));
		columns.add(new ImmutablePair<>(1, "`purchased` int(11)  DEFAULT 0"));
		columns.add(new ImmutablePair<>(1, "`taxpercent` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`maxPercentTaxAmount` float DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`taxes` float DEFAULT 0"));
		columns.add(new ImmutablePair<>(1, "`hasUpkeep` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`plotPrice` float DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`plotTax` float DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`commercialPlotPrice` float DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`commercialPlotTax` float NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`embassyPlotPrice` float NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`embassyPlotTax` float NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`open` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`public` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`adminEnabledMobs` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`admindisabledpvp` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`adminenabledpvp` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`allowedToWar` bool NOT NULL DEFAULT '1'"));
		columns.add(new ImmutablePair<>(1, "`homeblock` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`spawn` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`outpostSpawns` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`outlaws` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`uuid` VARCHAR(36) DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`registered` BIGINT DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`spawnCost` float NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`mapColorHexCode` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`metadata` text DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`conqueredDays` mediumint"));
		columns.add(new ImmutablePair<>(1, "`conquered` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`ruined` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`ruinedTime` BIGINT DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`neutral` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`debtBalance` float NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`joinedNationAt` BIGINT NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`primaryJail` VARCHAR(36) DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`movedHomeBlockAt` BIGINT NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`trustedResidents` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`trustedTowns` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`nationZoneOverride` int(11) DEFAULT 0"));
		columns.add(new ImmutablePair<>(1, "`nationZoneEnabled` bool NOT NULL DEFAULT '1'"));
		columns.add(new ImmutablePair<>(1, "`allies` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`enemies` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`hasUnlimitedClaims` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`manualTownLevel` BIGINT DEFAULT '-1'"));
		columns.add(new ImmutablePair<>(1, "`forSale` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`forSalePrice` float NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`forSaleTime` BIGINT DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`visibleOnTopLists` bool NOT NULL DEFAULT '1'"));
		return columns;
	}

	private static List<ImmutablePair<Integer, String>> getNationColumns() {
		// Version, Column Definition. Version is incremented when a column is added and ensure TownyDBTableType.latestVersion() is updated.
		final List<ImmutablePair<Integer, String>> columns = new ArrayList<>();
		columns.add(new ImmutablePair<>(1, "`capital` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`tag` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`allies` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`enemies` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`taxes` float NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`taxpercent` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`maxPercentTaxAmount` float DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`spawnCost` float NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`neutral` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`uuid` VARCHAR(36) DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`registered` BIGINT DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`nationBoard` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`mapColorHexCode` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`nationSpawn` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`isPublic` bool NOT NULL DEFAULT '1'"));
		columns.add(new ImmutablePair<>(1, "`isOpen` bool NOT NULL DEFAULT '1'"));
		columns.add(new ImmutablePair<>(1, "`metadata` text DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`conqueredTax` float NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`sanctionedTowns` mediumtext DEFAULT NULL"));
		return columns;
	}

	private static List<ImmutablePair<Integer, String>> getWorldColumns() {
		// Version, Column Definition. Version is incremented when a column is added and ensure TownyDBTableType.latestVersion() is updated.
		final List<ImmutablePair<Integer, String>> columns = new ArrayList<>();
		columns.add(new ImmutablePair<>(1, "`uuid` VARCHAR(36) DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`claimable` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`pvp` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`forcepvp` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`forcetownmobs` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`friendlyFire` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`worldmobs` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`wildernessmobs` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`firespread` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`forcefirespread` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`explosions` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`forceexplosions` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`endermanprotect` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`disablecreaturetrample` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`unclaimedZoneBuild` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`unclaimedZoneDestroy` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`unclaimedZoneSwitch` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`unclaimedZoneItemUse` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`unclaimedZoneName` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`unclaimedZoneIgnoreIds` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`usingPlotManagementDelete` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`plotManagementDeleteIds` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`isDeletingEntitiesOnUnclaim` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`unclaimDeleteEntityTypes` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`usingPlotManagementMayorDelete` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`plotManagementMayorDelete` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`usingPlotManagementRevert` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`plotManagementIgnoreIds` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`revertOnUnclaimWhitelistMaterials` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`usingPlotManagementWildRegen` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`plotManagementWildRegenEntities` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`plotManagementWildRegenBlockWhitelist` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`wildRegenBlocksToNotOverwrite` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`plotManagementWildRegenSpeed` long NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`usingPlotManagementWildRegenBlocks` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`plotManagementWildRegenBlocks` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`usingTowny` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`warAllowed` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`metadata` text DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`jailing` bool NOT NULL DEFAULT '1'"));
		return columns;
	}

	private static List<ImmutablePair<Integer, String>> getTownBlockColumns() {
		// Version, Column Definition. Version is incremented when a column is added and ensure TownyDBTableType.latestVersion() is updated.
		final List<ImmutablePair<Integer, String>> columns = new ArrayList<>();
		columns.add(new ImmutablePair<>(1, "`name` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`price` float DEFAULT '-1'"));
		columns.add(new ImmutablePair<>(1, "`taxed` bool NOT NULL DEFAULT '1'"));
		columns.add(new ImmutablePair<>(1, "`town` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`resident` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`type` TINYINT NOT  NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`typeName` mediumtext"));
		columns.add(new ImmutablePair<>(1, "`outpost` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`permissions` mediumtext NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`locked` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`changed` bool NOT NULL DEFAULT '0'"));
		columns.add(new ImmutablePair<>(1, "`metadata` text DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`groupID` VARCHAR(36) DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`districtID` VARCHAR(36) DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`claimedAt` BIGINT NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`trustedResidents` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`customPermissionData` mediumtext DEFAULT NULL"));
		columns.add(new ImmutablePair<>(1, "`minTownMembershipDays` SMALLINT NOT NULL DEFAULT '-1'"));
		columns.add(new ImmutablePair<>(1, "`maxTownMembershipDays` SMALLINT NOT NULL DEFAULT '-1'"));
		return columns;
	}

	private static List<ImmutablePair<Integer, String>> getCooldownColumns() {
		// Version, Column Definition. Version is incremented when a column is added and ensure TownyDBTableType.latestVersion() is updated.
		final List<ImmutablePair<Integer, String>> columns = new ArrayList<>();
		columns.add(new ImmutablePair<>(1, "`key` varchar(200) NOT NULL"));
		columns.add(new ImmutablePair<>(1, "`expiry` BIGINT NOT NULL"));
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
			// No need to check if the column exists, just try to drop it.
			// If it doesn't exist, we get error 1091, which we ignore.

			update = "ALTER TABLE `" + SQLDB_NAME + "`.`" + table + "` DROP COLUMN `" + column + "`";

			s.executeUpdate(update);

			TownyMessaging.sendDebugMsg("Table " + table + " has dropped the " + column + " column.");

		} catch (SQLException ee) {
			if (ee.getErrorCode() != MYSQL_DUPLICATE_COLUMN_ERR && ee.getErrorCode() != 1091)
				TownyMessaging.sendErrorMsg("Error updating table " + table + ":" + ee.getMessage());
		}
	}

	private record ColumnUpdate(String table, String column) {
		private static ColumnUpdate update(String table, String column) {
			return new ColumnUpdate(SQLSchema.TABLE_PREFIX + table, column);
		}
	}
}
