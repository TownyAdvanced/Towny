package com.palmergames.bukkit.towny.war.eventwar;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.utils.MetaDataUtil;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;

public class WarMetaDataController {

	private static StringDataField warUUID = new StringDataField("eventwar_warUUID", "");
	private static IntegerDataField residentLives = new IntegerDataField("eventwar_residentLives", 0, "War Lives Remaining");

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
			MetaDataUtil.setString(obj, sdf, uuid.toString());
		else
			obj.addMetaData(new StringDataField("eventwar_warUUID", uuid.toString()), true);
	}
	
	public static void removeWarUUID(TownyObject obj) {
		StringDataField sdf = (StringDataField) warUUID.clone();
		if (obj.hasMeta(sdf.getKey()))
			if (obj.removeMetaData(sdf, true))
				System.out.println("Successfully removed warUUID from " + obj.getName());
	}
	
	public static int getResidentLives(Resident res) {
		IntegerDataField idf = (IntegerDataField) residentLives.clone();
		if (res.hasMeta(idf.getKey()))
			return MetaDataUtil.getInt(res, idf);
		return 0;
	}
	
	public static void setResidentLives(Resident res, int lives) {
		IntegerDataField idf = (IntegerDataField) residentLives.clone();
		if (res.hasMeta(idf.getKey()))
			MetaDataUtil.setInt(res, idf, lives);
		else
			res.addMetaData(new IntegerDataField("eventwar_residentLives", lives, "War Lives Remaining"), true);
	}
	
	public static void decrementResidentLives(Resident res) {
		IntegerDataField idf = (IntegerDataField) residentLives.clone();
		if (res.hasMeta(idf.getKey()))
			MetaDataUtil.setInt(res, idf, getResidentLives(res) - 1);
		res.save();
	}
	
	public static void removeResidentLivesMeta(Resident res) {
		IntegerDataField idf = (IntegerDataField) residentLives.clone();
		if (res.hasMeta(idf.getKey()))
			res.removeMetaData(idf, true);
	}
}
