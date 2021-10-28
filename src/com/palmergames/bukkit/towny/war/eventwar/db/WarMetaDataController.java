package com.palmergames.bukkit.towny.war.eventwar.db;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.utils.MetaDataUtil;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;
import com.palmergames.bukkit.towny.object.metadata.LongDataField;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;

public class WarMetaDataController {

	private static StringDataField warUUID = new StringDataField("eventwar_warUUID", "");
	private static IntegerDataField residentLives = new IntegerDataField("eventwar_residentLives", 0, "War Lives Remaining");
	private static IntegerDataField score = new IntegerDataField("eventwar_score", 0);
	private static IntegerDataField tokens = new IntegerDataField("eventwar_tokens", 0, "War Tokens");
	private static StringDataField warSide = new StringDataField("eventwar_warSide", "");
	private static LongDataField lastWarEndTime = new LongDataField("eventwar_lastWarEndTime", 0l);

	/*
	 * War UUIDs 
	 */

	@Nullable
	public static String getWarUUID(TownyObject obj) {
		StringDataField sdf = (StringDataField) warUUID.clone();
		if (obj.hasMeta(sdf.getKey()))
			return MetaDataUtil.getString(obj, sdf);
		return null;
	}
	
	public static void setWarUUID(TownyObject obj, UUID uuid) {
		StringDataField sdf = (StringDataField) warUUID.clone();
		if (obj.hasMeta(sdf.getKey()))
			MetaDataUtil.setString(obj, sdf, uuid.toString(), true);
		else
			obj.addMetaData(new StringDataField("eventwar_warUUID", uuid.toString()), true);
	}
	
	public static void removeWarUUID(TownyObject obj) {
		StringDataField sdf = (StringDataField) warUUID.clone();
		if (obj.hasMeta(sdf.getKey()))
			obj.removeMetaData(sdf, true);
	}
	
	/*
	 * Resident Lives 
	 */
	
	public static int getResidentLives(Resident res) {
		IntegerDataField idf = (IntegerDataField) residentLives.clone();
		if (res.hasMeta(idf.getKey()))
			return MetaDataUtil.getInt(res, idf);
		return 0;
	}
	
	public static boolean hasResidentLives(Resident res) {
		IntegerDataField idf = (IntegerDataField) residentLives.clone();
		return res.hasMeta(idf.getKey());
	}
	
	public static void setResidentLives(Resident res, int lives) {
		IntegerDataField idf = (IntegerDataField) residentLives.clone();
		if (res.hasMeta(idf.getKey()))
			MetaDataUtil.setInt(res, idf, lives, true);
		else
			res.addMetaData(new IntegerDataField("eventwar_residentLives", lives, "War Lives Remaining"), true);
	}
	
	public static void decrementResidentLives(Resident res) {
		IntegerDataField idf = (IntegerDataField) residentLives.clone();
		if (res.hasMeta(idf.getKey()))
			MetaDataUtil.setInt(res, idf, getResidentLives(res) - 1, true);
		res.save();
	}
	
	public static void removeResidentLivesMeta(Resident res) {
		IntegerDataField idf = (IntegerDataField) residentLives.clone();
		if (res.hasMeta(idf.getKey()))
			res.removeMetaData(idf, true);
	}
	
	/*
	 * War Sides (For Riot and Civil Wars) 
	 */
	
	@Nullable
	public static String getWarSide(TownyObject obj) {
		StringDataField sdf = (StringDataField) warSide.clone();
		if (obj.hasMeta(sdf.getKey()))
			return MetaDataUtil.getString(obj, sdf);
		return null;
	}
	
	public static void setWarSide(TownyObject obj, String side) {
		StringDataField sdf = (StringDataField) warSide.clone();
		if (obj.hasMeta(sdf.getKey()))
			MetaDataUtil.setString(obj, sdf, side, true);
		else
			obj.addMetaData(new StringDataField("eventwar_warSide", side), true);
	}
	
	public static void removeWarSide(TownyObject obj) {
		StringDataField sdf = (StringDataField) warSide.clone();
		if (obj.hasMeta(sdf.getKey()))
			obj.removeMetaData(sdf, true);
	}

	/*
	 * Scores 
	 */
	
	public static int getScore(TownyObject obj) {
		IntegerDataField idf = (IntegerDataField) score.clone();
		if (obj.hasMeta(idf.getKey()))
			return MetaDataUtil.getInt(obj, idf);
		return 0;
	}
	
	public static boolean hasScore(TownyObject obj) {
		IntegerDataField idf = (IntegerDataField) score.clone();
		return obj.hasMeta(idf.getKey());
	}
	
	public static void setScore(TownyObject obj, int _score) {
		IntegerDataField idf = (IntegerDataField) score.clone();
		if (obj.hasMeta(idf.getKey()))
			MetaDataUtil.setInt(obj, idf, _score, true);
		else
			obj.addMetaData(new IntegerDataField("eventwar_score", _score), true);
	}
	
	public static void removeScore(TownyObject obj) {
		IntegerDataField idf = (IntegerDataField) score.clone();
		if (obj.hasMeta(idf.getKey()))
			obj.removeMetaData(idf, true);
	}
	
	/*
	 * Last War Times 
	 */
	
	public static long getLastWarTime(TownyObject obj) {
		LongDataField ldf = (LongDataField) lastWarEndTime.clone();
		if (obj.hasMeta(ldf.getKey()))
			return MetaDataUtil.getLong(obj, ldf);
		else
			return 0l;
	}
	
	public static boolean hasLastWarTime(TownyObject obj) {
		LongDataField ldf = (LongDataField) lastWarEndTime.clone();
		return obj.hasMeta(ldf.getKey());
	}
	
	public static void setLastWarTime(TownyObject obj, long time) {
		LongDataField ldf = (LongDataField) lastWarEndTime.clone();
		if (obj.hasMeta(ldf.getKey()))
			MetaDataUtil.setLong(obj, ldf, time, true);
		else
			obj.addMetaData(new LongDataField("eventwar_lastWarEndTime"), true);
	}

	public static void removeEndTime(TownyObject obj) {
		LongDataField ldf = (LongDataField) lastWarEndTime.clone();
		if (obj.hasMeta(ldf.getKey()))
			obj.removeMetaData(ldf, true);
	}
	
	/*
	 * War Tokens  
	 */
	
	public static int getWarTokens(TownyObject obj) {
		IntegerDataField idf = (IntegerDataField) tokens.clone();
		if (obj.hasMeta(idf.getKey()))
			return MetaDataUtil.getInt(obj, idf);
		return 0;
	}
	
	public static void setTokens(TownyObject obj, int num) {
		IntegerDataField idf = (IntegerDataField) tokens.clone();
		if (obj.hasMeta(idf.getKey()))
			MetaDataUtil.setInt(obj, idf, num, true);
		else
			obj.addMetaData(new IntegerDataField("eventwar_tokens", num, "War Tokens"), true);
	}
	
	public static void removeTokens(TownyObject obj) {
		IntegerDataField idf = (IntegerDataField) tokens.clone();
		if (obj.hasMeta(idf.getKey()))
			obj.removeMetaData(idf, true);
	}
	
	public static void incrementTokens(TownyObject obj) {
		IntegerDataField idf = (IntegerDataField) tokens.clone();
		if (obj.hasMeta(idf.getKey()))
			MetaDataUtil.setInt(obj, idf, getWarTokens(obj) + 1, true);
		else
			obj.addMetaData(new IntegerDataField("eventwar_tokens", 1, "War Tokens"), true);
	}

}
