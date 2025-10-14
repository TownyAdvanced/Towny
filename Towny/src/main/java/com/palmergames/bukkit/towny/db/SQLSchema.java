package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.db.TownySQLSource.TownyDBTableType;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
		final Map<String, Set<String>> existingTableColumns = loadExistingTables(cntx);

		initTable(cntx, TownyDBTableType.WORLD, existingTableColumns);
		updateTable(cntx, TownyDBTableType.WORLD, getWorldColumns(), existingTableColumns);

		initTable(cntx, TownyDBTableType.NATION, existingTableColumns);
		updateTable(cntx, TownyDBTableType.NATION, getNationColumns(), existingTableColumns);

		initTable(cntx, TownyDBTableType.TOWN, existingTableColumns);
		updateTable(cntx, TownyDBTableType.TOWN, getTownColumns(), existingTableColumns);

		initTable(cntx, TownyDBTableType.RESIDENT, existingTableColumns);
		updateTable(cntx, TownyDBTableType.RESIDENT, getResidentColumns(), existingTableColumns);

		initTable(cntx, TownyDBTableType.TOWNBLOCK, existingTableColumns);
		updateTable(cntx, TownyDBTableType.TOWNBLOCK, getTownBlockColumns(), existingTableColumns);

		initTable(cntx, TownyDBTableType.PLOTGROUP, existingTableColumns);
		updateTable(cntx, TownyDBTableType.PLOTGROUP, getPlotGroupColumns(), existingTableColumns);

		initTable(cntx, TownyDBTableType.DISTRICT, existingTableColumns);
		updateTable(cntx, TownyDBTableType.DISTRICT, getDistrictColumns(), existingTableColumns);

		initTable(cntx, TownyDBTableType.JAIL, existingTableColumns);
		updateTable(cntx, TownyDBTableType.JAIL, getJailsColumns(), existingTableColumns);

		initTable(cntx, TownyDBTableType.HIBERNATED_RESIDENT, existingTableColumns);
		updateTable(cntx, TownyDBTableType.HIBERNATED_RESIDENT, getHibernatedResidentsColumns(), existingTableColumns);
		
		initTable(cntx, TownyDBTableType.COOLDOWN, existingTableColumns);
		updateTable(cntx, TownyDBTableType.COOLDOWN, getCooldownColumns(), existingTableColumns);
	}

	/*
	 * Check that the tables are created.
	 */
	private static void initTable(Connection cntx, TownyDBTableType tableType, Map<String, Set<String>> existingTableColumns) {
		if (existingTableColumns.containsKey((TABLE_PREFIX + tableType.tableName()).toLowerCase(Locale.ROOT))) {
			TownyMessaging.sendDebugMsg("Table " + tableType.tableName() + " already exists!");
			return;
		}

		try (Statement s = cntx.createStatement()) {
			s.executeUpdate(fetchTableSchema(tableType));
			TownyMessaging.sendDebugMsg("Table " + tableType.tableName() + " is ok!");
		} catch (SQLException ee) {
			TownyMessaging.sendErrorMsg("Error Creating table " + tableType.tableName() + " : " + ee.getMessage());
		}
	}

	/**
	 * Queries all existing columns and their tables.
	 */
	private static Map<String, Set<String>> loadExistingTables(final Connection connection) {
		final Map<String, Set<String>> existingTableColumns = new HashMap<>();

		try (ResultSet rs = connection.getMetaData().getColumns(SQLDB_NAME, null, null, null)) {
			while (rs.next()) {
				final String tableName = rs.getString("TABLE_NAME").toLowerCase(Locale.ROOT);
				final String columnName = rs.getString("COLUMN_NAME"); // not necessary to toLowerCase this

				existingTableColumns.computeIfAbsent(tableName, k -> new HashSet<>()).add(columnName);
			}
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Error retrieving existing tables and columns: " + e.getMessage());
		}

		return existingTableColumns;
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
	private static void updateTable(Connection cntx, TownyDBTableType tableType, List<ColumnData> columns, Map<String, Set<String>> existingTableColumns) {
		String update = "ALTER TABLE `" + SQLDB_NAME + "`.`" + TABLE_PREFIX + tableType.tableName() + "` ADD COLUMN ";
		final Set<String> existingColumns = existingTableColumns.getOrDefault((TABLE_PREFIX + tableType.tableName()).toLowerCase(Locale.ROOT), Set.of());

		int addedColumns = 0;
		for (final ColumnData column : columns) {
			if (existingColumns.contains(column.name)) {
				continue;
			}

			try (PreparedStatement ps = cntx.prepareStatement(update + column)) {
				ps.executeUpdate();
				addedColumns++;
			} catch (SQLException ee) {
				if (ee.getErrorCode() != MYSQL_DUPLICATE_COLUMN_ERR)
					TownyMessaging.sendErrorMsg("Error updating table " + tableType.tableName() + ":" + ee.getMessage());
			}
		}
		TownyMessaging.sendDebugMsg("Table " + tableType.tableName() + " is updated! Created " + addedColumns + " missing column" + (addedColumns == 1 ? "" : "s") + ".");
	}

	/*
	 * Columns of each Object type follow:
	 */
	
	private static List<ColumnData> getJailsColumns() {
		List<ColumnData> columns = new ArrayList<>();
		columns.add(new ColumnData("townBlock", "mediumtext NOT NULL"));
		columns.add(new ColumnData("spawns", "mediumtext DEFAULT NULL"));
		return columns;
	}

	private static List<ColumnData> getPlotGroupColumns() {
		List<ColumnData> columns = new ArrayList<>();
		columns.add(new ColumnData("groupName", "mediumtext NOT NULL"));
		columns.add(new ColumnData("groupPrice", "float DEFAULT NULL"));
		columns.add(new ColumnData("town", "VARCHAR(32) NOT NULL"));
		columns.add(new ColumnData("metadata", "text DEFAULT NULL"));
		return columns;
	}

	private static List<ColumnData> getDistrictColumns() {
		List<ColumnData> columns = new ArrayList<>();
		columns.add(new ColumnData("districtName", "mediumtext NOT NULL"));
		columns.add(new ColumnData("town", "VARCHAR(36) NOT NULL"));
		columns.add(new ColumnData("metadata", "text DEFAULT NULL"));
		return columns;
	}

	private static List<ColumnData> getResidentColumns(){
		List<ColumnData> columns = new ArrayList<>();
		columns.add(new ColumnData("town", "mediumtext"));
		columns.add(new ColumnData("town-ranks", "mediumtext"));
		columns.add(new ColumnData("nation-ranks", "mediumtext"));
		columns.add(new ColumnData("lastOnline", "BIGINT NOT NULL"));
		columns.add(new ColumnData("registered", "BIGINT NOT NULL"));
		columns.add(new ColumnData("joinedTownAt", "BIGINT NOT NULL"));
		columns.add(new ColumnData("isNPC", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("jailUUID", "VARCHAR(36) DEFAULT NULL"));
		columns.add(new ColumnData("jailCell", "mediumint"));
		columns.add(new ColumnData("jailHours", "mediumint"));
		columns.add(new ColumnData("jailBail", "float DEFAULT NULL"));
		columns.add(new ColumnData("title", "mediumtext"));
		columns.add(new ColumnData("surname", "mediumtext"));
		columns.add(new ColumnData("protectionStatus", "mediumtext"));
		columns.add(new ColumnData("friends", "mediumtext"));
		columns.add(new ColumnData("metadata", "text DEFAULT NULL"));
		columns.add(new ColumnData("uuid", "VARCHAR(36) NOT NULL"));
		columns.add(new ColumnData("about", "mediumtext DEFAULT NULL"));
		return columns;
	}

	private static List<ColumnData> getHibernatedResidentsColumns() {
		List<ColumnData> columns = new ArrayList<>();
		columns.add(new ColumnData("registered", "BIGINT DEFAULT NULL"));
		return columns;
	}

	private static List<ColumnData> getTownColumns() {
		List<ColumnData> columns = new ArrayList<>();
		columns.add(new ColumnData("mayor", "mediumtext"));
		columns.add(new ColumnData("nation", "mediumtext"));
		columns.add(new ColumnData("townBoard", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("tag", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("founder", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("protectionStatus", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("bonus", "int(11) DEFAULT 0"));
		columns.add(new ColumnData("purchased", "int(11)  DEFAULT 0"));
		columns.add(new ColumnData("taxpercent", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("maxPercentTaxAmount", "float DEFAULT NULL"));
		columns.add(new ColumnData("taxes", "float DEFAULT 0"));
		columns.add(new ColumnData("hasUpkeep", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("plotPrice", "float DEFAULT NULL"));
		columns.add(new ColumnData("plotTax", "float DEFAULT NULL"));
		columns.add(new ColumnData("commercialPlotPrice", "float DEFAULT NULL"));
		columns.add(new ColumnData("commercialPlotTax", "float NOT NULL"));
		columns.add(new ColumnData("embassyPlotPrice", "float NOT NULL"));
		columns.add(new ColumnData("embassyPlotTax", "float NOT NULL"));
		columns.add(new ColumnData("open", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("public", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("adminEnabledMobs", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("admindisabledpvp", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("adminenabledpvp", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("allowedToWar", "bool NOT NULL DEFAULT '1'"));
		columns.add(new ColumnData("homeblock", "mediumtext NOT NULL"));
		columns.add(new ColumnData("spawn", "mediumtext NOT NULL"));
		columns.add(new ColumnData("outpostSpawns", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("jailSpawns", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("outlaws", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("uuid", "VARCHAR(36) DEFAULT NULL"));
		columns.add(new ColumnData("registered", "BIGINT DEFAULT NULL"));
		columns.add(new ColumnData("spawnCost", "float NOT NULL"));
		columns.add(new ColumnData("mapColorHexCode", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("metadata", "text DEFAULT NULL"));
		columns.add(new ColumnData("conqueredDays", "mediumint"));
		columns.add(new ColumnData("conquered", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("ruined", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("ruinedTime", "BIGINT DEFAULT '0'"));
		columns.add(new ColumnData("neutral", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("debtBalance", "float NOT NULL"));
		columns.add(new ColumnData("joinedNationAt", "BIGINT NOT NULL"));
		columns.add(new ColumnData("primaryJail", "VARCHAR(36) DEFAULT NULL"));
		columns.add(new ColumnData("movedHomeBlockAt", "BIGINT NOT NULL"));
		columns.add(new ColumnData("trustedResidents", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("trustedTowns", "mediumtext NOT NULL"));
		columns.add(new ColumnData("nationZoneOverride", "int(11) DEFAULT 0"));
		columns.add(new ColumnData("nationZoneEnabled", "bool NOT NULL DEFAULT '1'"));
		columns.add(new ColumnData("allies", "mediumtext NOT NULL"));
		columns.add(new ColumnData("enemies", "mediumtext NOT NULL"));
		columns.add(new ColumnData("hasUnlimitedClaims", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("manualTownLevel", "BIGINT DEFAULT '-1'"));
		columns.add(new ColumnData("forSale", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("forSalePrice", "float NOT NULL"));
		columns.add(new ColumnData("forSaleTime", "BIGINT DEFAULT '0'"));
		columns.add(new ColumnData("visibleOnTopLists", "bool NOT NULL DEFAULT '1'"));
		
		return columns;
	}

	private static List<ColumnData> getNationColumns(){
		List<ColumnData> columns = new ArrayList<>();
		columns.add(new ColumnData("capital", "mediumtext NOT NULL"));
		columns.add(new ColumnData("tag", "mediumtext NOT NULL"));
		columns.add(new ColumnData("allies", "mediumtext NOT NULL"));
		columns.add(new ColumnData("enemies", "mediumtext NOT NULL"));
		columns.add(new ColumnData("taxes", "float NOT NULL"));
		columns.add(new ColumnData("taxpercent", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("maxPercentTaxAmount", "float DEFAULT NULL"));
		columns.add(new ColumnData("spawnCost", "float NOT NULL"));
		columns.add(new ColumnData("neutral", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("uuid", "VARCHAR(36) DEFAULT NULL"));
		columns.add(new ColumnData("registered", "BIGINT DEFAULT NULL"));
		columns.add(new ColumnData("nationBoard", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("mapColorHexCode", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("nationSpawn", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("isPublic", "bool NOT NULL DEFAULT '1'"));
		columns.add(new ColumnData("isOpen", "bool NOT NULL DEFAULT '1'"));
		columns.add(new ColumnData("metadata", "text DEFAULT NULL"));
		columns.add(new ColumnData("conqueredTax", "float NOT NULL"));
		columns.add(new ColumnData("sanctionedTowns", "mediumtext DEFAULT NULL"));
		return columns;
	}

	private static List<ColumnData> getWorldColumns() {
		List<ColumnData> columns = new ArrayList<>();
		columns.add(new ColumnData("uuid", "VARCHAR(36) DEFAULT NULL"));
		columns.add(new ColumnData("claimable", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("pvp", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("forcepvp", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("forcetownmobs", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("friendlyFire", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("worldmobs", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("wildernessmobs", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("firespread", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("forcefirespread", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("explosions", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("forceexplosions", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("endermanprotect", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("disablecreaturetrample", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("unclaimedZoneBuild", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("unclaimedZoneDestroy", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("unclaimedZoneSwitch", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("unclaimedZoneItemUse", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("unclaimedZoneName", "mediumtext NOT NULL"));
		columns.add(new ColumnData("unclaimedZoneIgnoreIds", "mediumtext NOT NULL"));
		columns.add(new ColumnData("usingPlotManagementDelete", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("plotManagementDeleteIds", "mediumtext NOT NULL"));
		columns.add(new ColumnData("isDeletingEntitiesOnUnclaim", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("unclaimDeleteEntityTypes", "mediumtext NOT NULL"));
		columns.add(new ColumnData("usingPlotManagementMayorDelete", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("plotManagementMayorDelete", "mediumtext NOT NULL"));
		columns.add(new ColumnData("usingPlotManagementRevert", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("plotManagementIgnoreIds", "mediumtext NOT NULL"));
		columns.add(new ColumnData("revertOnUnclaimWhitelistMaterials", "mediumtext NOT NULL"));
		columns.add(new ColumnData("usingPlotManagementWildRegen", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("plotManagementWildRegenEntities", "mediumtext NOT NULL"));
		columns.add(new ColumnData("plotManagementWildRegenBlockWhitelist", "mediumtext NOT NULL"));
		columns.add(new ColumnData("wildRegenBlocksToNotOverwrite", "mediumtext NOT NULL"));
		columns.add(new ColumnData("plotManagementWildRegenSpeed", "long NOT NULL"));
		columns.add(new ColumnData("usingPlotManagementWildRegenBlocks", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("plotManagementWildRegenBlocks", "mediumtext NOT NULL"));		
		columns.add(new ColumnData("usingTowny", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("warAllowed", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("metadata", "text DEFAULT NULL"));
		columns.add(new ColumnData("jailing", "bool NOT NULL DEFAULT '1'"));
		return columns;
	}

	private static List<ColumnData> getTownBlockColumns() {
		List<ColumnData> columns = new ArrayList<>();
		columns.add(new ColumnData("name", "mediumtext"));
		columns.add(new ColumnData("price", "float DEFAULT '-1'"));
		columns.add(new ColumnData("taxed", "bool NOT NULL DEFAULT '1'"));
		columns.add(new ColumnData("town", "mediumtext"));
		columns.add(new ColumnData("resident", "mediumtext"));
		columns.add(new ColumnData("type", "TINYINT NOT  NULL DEFAULT '0'"));
		columns.add(new ColumnData("typeName", "mediumtext"));
		columns.add(new ColumnData("outpost", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("permissions", "mediumtext NOT NULL"));
		columns.add(new ColumnData("locked", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("changed", "bool NOT NULL DEFAULT '0'"));
		columns.add(new ColumnData("metadata", "text DEFAULT NULL"));
		columns.add(new ColumnData("groupID", "VARCHAR(36) DEFAULT NULL"));
		columns.add(new ColumnData("districtID", "VARCHAR(36) DEFAULT NULL"));
		columns.add(new ColumnData("claimedAt", "BIGINT NOT NULL"));
		columns.add(new ColumnData("trustedResidents", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("customPermissionData", "mediumtext DEFAULT NULL"));
		columns.add(new ColumnData("minTownMembershipDays", "SMALLINT NOT NULL DEFAULT '-1'"));
		columns.add(new ColumnData("maxTownMembershipDays", "SMALLINT NOT NULL DEFAULT '-1'"));
		return columns;
	}
	
	private static List<ColumnData> getCooldownColumns() {
		List<ColumnData> columns = new ArrayList<>();
		columns.add(new ColumnData("key", "varchar(200) NOT NULL"));
		columns.add(new ColumnData("expiry", "BIGINT NOT NULL"));
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

	private record ColumnData(String name, String dataType) {
		@Override
		public @NotNull String toString() {
			return "`" + this.name + "` " + this.dataType;
		}
	}
}
