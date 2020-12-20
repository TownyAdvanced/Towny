package com.palmergames.bukkit.towny.war.siegewar.metadata;

import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;
import com.palmergames.bukkit.towny.object.metadata.DecimalDataField;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;
import com.palmergames.bukkit.towny.object.metadata.LongDataField;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import com.palmergames.bukkit.towny.war.siegewar.metadata.MetaDataUtil;

/**
 * 
 * 
 * @author LlmDl
 *
 */
public class SiegeMetaDataController {

	@SuppressWarnings("unused")
	private Towny plugin;
	private static SiegeMetaDataController siegeMetaDataController;
	private static BooleanDataField hasSiege = new BooleanDataField("siegewar_hasSiege", false);
	private static StringDataField siegeName = new StringDataField("siegewar_name", "");
	private static StringDataField siegeNationUUID = new StringDataField("siegewar_nationName", "");
	private static StringDataField siegeTownUUID = new StringDataField("siegewar_townName", "");
	private static StringDataField siegeFlagLocation = new StringDataField("siegewar_flagLocation", "");
	private static StringDataField siegeStatus = new StringDataField("siegewar_status", "");
	private static IntegerDataField siegePoints = new IntegerDataField("siegewar_points", 0);
	private static DecimalDataField siegeWarChestAmount = new DecimalDataField("siegewar_warChestAmount", 0.0);
	private static BooleanDataField townPlundered = new BooleanDataField("siegewar_townPlundered", false);
	private static BooleanDataField townInvaded = new BooleanDataField("siegewar_townInvaded", false);
	private static LongDataField startTime = new LongDataField("siegewar_startTime", 0l);
	private static LongDataField endTime = new LongDataField("siegewar_endTime", 0l);
	private static LongDataField actualEndTime = new LongDataField("siegewar_actualEndTime", 0l);
	
	
	public SiegeMetaDataController(Towny plugin) {
		this.plugin = plugin;
	}
	
	public static SiegeMetaDataController getSiegeMeta() {
		return siegeMetaDataController;
	}
	
	public static boolean hasSiege(Town town) {
		BooleanDataField bdf = (BooleanDataField) hasSiege.clone();
		if (town.hasMeta(bdf.getKey())) {
			return MetaDataUtil.getBoolean(town, bdf);
		}
		return false;
	}
	
	public static void setSiege(Town town, boolean bool) {
		BooleanDataField bdf = (BooleanDataField) hasSiege.clone();
		if (town.hasMeta(bdf.getKey()))
			MetaDataUtil.setBoolean(town, bdf, bool);
		else
			town.addMetaData(new BooleanDataField("siegewar_hasSiege", bool));
	}

	@Nullable
	public static String getSiegeName(Town town) {
		StringDataField sdf = (StringDataField) siegeName.clone();
		if (town.hasMeta(sdf.getKey()))
			return MetaDataUtil.getString(town, sdf);
		return null;
	}
	
	public static void setSiegeName(Town town, String name) {
		StringDataField sdf = (StringDataField) siegeName.clone();
		if (town.hasMeta(sdf.getKey()))
			MetaDataUtil.setString(town, sdf, name);
		else
			town.addMetaData(new StringDataField("siegewar_name", name));
	}
	
	@Nullable
	public static String getNationUUID(Town town) {
		StringDataField sdf = (StringDataField) siegeNationUUID.clone();
		if (town.hasMeta(sdf.getKey()))
			return MetaDataUtil.getString(town, sdf);
		return null;
	}
	
	public static void setNationUUID(Town town, String name) {
		StringDataField sdf = (StringDataField) siegeNationUUID.clone();
		if (town.hasMeta(sdf.getKey()))
			MetaDataUtil.setString(town, sdf, name);
		else
			town.addMetaData(new StringDataField("siegewar_nationName", name));
	}
	
	@Nullable
	public static String getTownName(Town town) {
		StringDataField sdf = (StringDataField) siegeTownUUID.clone();
		if (town.hasMeta(sdf.getKey()))
			return MetaDataUtil.getString(town, sdf);
		return null;
	}
	
	public static void setTownUUID(Town town, String name) {
		StringDataField sdf = (StringDataField) siegeTownUUID.clone();
		if (town.hasMeta(sdf.getKey()))
			MetaDataUtil.setString(town, sdf, name);
		else
			town.addMetaData(new StringDataField("siegewar_townName", name));
	}

	@Nullable
	public static String getFlagLocation(Town town) {
		StringDataField sdf = (StringDataField) siegeFlagLocation.clone();
		if (town.hasMeta(sdf.getKey()))
			return MetaDataUtil.getString(town, sdf);
		return null;
	}
	
	public static void setFlagLocation(Town town, String name) {
		StringDataField sdf = (StringDataField) siegeFlagLocation.clone();
		if (town.hasMeta(sdf.getKey()))
			MetaDataUtil.setString(town, sdf, name);
		else
			town.addMetaData(new StringDataField("siegewar_flagLocation", name));
	}
	
	@Nullable
	public static String getStatus(Town town) {
		StringDataField sdf = (StringDataField) siegeStatus.clone();
		if (town.hasMeta(sdf.getKey()))
			return MetaDataUtil.getString(town, sdf);
		return null;
	}
	
	public static void setStatus(Town town, String name) {
		StringDataField sdf = (StringDataField) siegeStatus.clone();
		if (town.hasMeta(sdf.getKey()))
			MetaDataUtil.setString(town, sdf, name);
		else
			town.addMetaData(new StringDataField("siegewar_status", name));
	}
	
	public static int getPoints(Town town) {
		IntegerDataField idf = (IntegerDataField) siegePoints.clone();
		if (town.hasMeta(idf.getKey()))
			return MetaDataUtil.getInt(town, idf);
		return 0;
	}
	
	public static void setPoints(Town town, int num) {
		IntegerDataField idf = (IntegerDataField) siegePoints.clone();
		if (town.hasMeta(idf.getKey()))
			MetaDataUtil.setInt(town, idf, num);
		else
			town.addMetaData(new IntegerDataField("siegewar_points", num));
	}
	
	public static double getWarChestAmount(Town town) {
		DecimalDataField ddf = (DecimalDataField) siegeWarChestAmount.clone();
		if (town.hasMeta(ddf.getKey()))
			return MetaDataUtil.getDouble(town, ddf);
		return 0.0;
	}
	
	public static void setWarChestAmount(Town town, double num) {
		DecimalDataField ddf = (DecimalDataField) siegeWarChestAmount.clone();
		if (town.hasMeta(ddf.getKey()))
			MetaDataUtil.setDouble(town, ddf, num);
		else
			town.addMetaData(new DecimalDataField("siegewar_warChestAmount", num));
	}
	
	public static boolean townPlundered(Town town) {
		BooleanDataField bdf = (BooleanDataField) townPlundered.clone();
		if (town.hasMeta(bdf.getKey())) {
			return MetaDataUtil.getBoolean(town, bdf);
		}
		return false;
	}
	
	public static void setTownPlundered(Town town, boolean bool) {
		BooleanDataField bdf = (BooleanDataField) townPlundered.clone();
		if (town.hasMeta(bdf.getKey()))
			MetaDataUtil.setBoolean(town, bdf, bool);
		else
			town.addMetaData(new BooleanDataField("siegewar_townPlundered", bool));
	}
	
	public static boolean townInvaded(Town town) {
		BooleanDataField bdf = (BooleanDataField) townInvaded.clone();
		if (town.hasMeta(bdf.getKey())) {
			return MetaDataUtil.getBoolean(town, bdf);
		}
		return false;
	}
	
	public static void setTownInvaded(Town town, boolean bool) {
		BooleanDataField bdf = (BooleanDataField) townInvaded.clone();
		if (town.hasMeta(bdf.getKey()))
			MetaDataUtil.setBoolean(town, bdf, bool);
		else
			town.addMetaData(new BooleanDataField("siegewar_townInvaded", bool));
	}
	
	public static long getStartTime(Town town) {
		LongDataField ldf = (LongDataField) startTime.clone();
		if (town.hasMeta(ldf.getKey()))
			return MetaDataUtil.getLong(town, ldf);
		return 0l;
	}

	public static void setStartTime(Town town, long num) {
		LongDataField ldf = (LongDataField) startTime.clone();
		if (town.hasMeta(ldf.getKey()))
			MetaDataUtil.setLong(town, ldf, num);
		else
			town.addMetaData(new LongDataField("siegewar_startTime", num));
	}
	
	public static long getEndTime(Town town) {
		LongDataField ldf = (LongDataField) endTime.clone();
		if (town.hasMeta(ldf.getKey()))
			return MetaDataUtil.getLong(town, ldf);
		return 0l;
	}

	public static void setEndTime(Town town, long num) {
		LongDataField ldf = (LongDataField) endTime.clone();
		if (town.hasMeta(ldf.getKey()))
			MetaDataUtil.setLong(town, ldf, num);
		else
			town.addMetaData(new LongDataField("siegewar_endTime", num));
	}
	
	public static long getActualEndTime(Town town) {
		LongDataField ldf = (LongDataField) actualEndTime.clone();
		if (town.hasMeta(ldf.getKey()))
			return MetaDataUtil.getLong(town, ldf);
		return 0l;
	}

	public static void setActualEndTime(Town town, long num) {
		LongDataField ldf = (LongDataField) actualEndTime.clone();
		if (town.hasMeta(ldf.getKey()))
			MetaDataUtil.setLong(town, ldf, num);
		else
			town.addMetaData(new LongDataField("siegewar_actualEndTime", num));
	}
}
