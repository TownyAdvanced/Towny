package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;
import com.palmergames.bukkit.towny.object.metadata.ByteDataField;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.DecimalDataField;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;
import com.palmergames.bukkit.towny.object.metadata.LongDataField;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;

/**
 * 
 * @author LlmDl
 *
 */
public class MetaDataUtil {
	
	/**
	 * Does the TownyObject have the StringDataField meta?
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock. 
	 * @param sdf StringDataField to check for on the TownyObject.
	 * @return true if the TownyObject has the StringDataField meta.
	 */
	public static boolean hasMeta(TownyObject townyObject, StringDataField sdf) {
		return townyObject.hasMeta(sdf.getKey());
	}

	/**
	 * Does the TownyObject have the BooleanDataField meta?
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock. 
	 * @param bdf BooleanDataField to check for on the TownyObject.
	 * @return true if the TownyObject has the BooleanDataField meta.
	 */
	public static boolean hasMeta(TownyObject townyObject, BooleanDataField bdf) {
		return townyObject.hasMeta(bdf.getKey());
	}
	
	/**
	 * Does the TownyObject have the LongDataField meta?
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock. 
	 * @param ldf LongDataField to check for on the TownyObject.
	 * @return true if the TownyObject has the LongDataField meta.
	 */
	public static boolean hasMeta(TownyObject townyObject, LongDataField ldf) {
		return townyObject.hasMeta(ldf.getKey());
	}
	
	/**
	 * Does the TownyObject have the IntegerDataField meta?
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock. 
	 * @param idf IntegerDataField to check for on the TownyObject.
	 * @return true if the TownyObject has the IntegerDataField meta.
	 */
	public static boolean hasMeta(TownyObject townyObject, IntegerDataField idf) {
		return townyObject.hasMeta(idf.getKey());
	}
	
	/**
	 * Does the TownyObject have the DecimalDataField meta?
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock. 
	 * @param ddf DecimalDataField to check for on the TownyObject.
	 * @return true if the TownyObject has the DecimalDataField meta.
	 */
	public static boolean hasMeta(TownyObject townyObject, DecimalDataField ddf) {
		return townyObject.hasMeta(ddf.getKey());
	}
	
	/**
	 * Does the TownyObject have the ByteDataField meta?
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock. 
	 * @param bdf ByteDataField to check for on the TownyObject.
	 * @return true if the TownyObject has the ByteDataField meta.
	 */
	public static boolean hasMeta(TownyObject townyObject, ByteDataField bdf) {
		return townyObject.hasMeta(bdf.getKey());
	}

	/**
	 * Get a string from a TownyObject's metadata.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param sdf StringDataField to get from the TownyObject.
	 * @return String from the metadata or an empty string.
	 */
	public static String getString(TownyObject townyObject, StringDataField sdf) {
		CustomDataField<?> cdf = townyObject.getMetadata(sdf.getKey());
		if (cdf instanceof StringDataField) {
			return ((StringDataField) cdf).getValue();
		}
		return "";
	}

	/**
	 * Get a boolean from a TownyObject's metadata.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param bdf BooleanDataField to get from the TownyObject.
	 * @return boolean from the metadata or false.
	 */
	public static boolean getBoolean(TownyObject townyObject, BooleanDataField bdf) {
		CustomDataField<?> cdf = townyObject.getMetadata(bdf.getKey());
		if (cdf instanceof BooleanDataField)
			return ((BooleanDataField) cdf).getValue();
		return false;
	}

	/**
	 * Get a long from a TownyObject's metadata.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param ldf LongDataField to get from the TownyObject.
	 * @return long from the metadata or 0l.
	 */
	public static long getLong(TownyObject townyObject, LongDataField ldf) {
		CustomDataField<?> cdf = townyObject.getMetadata(ldf.getKey());
		if (cdf instanceof LongDataField)
			return ((LongDataField) cdf).getValue();
		return 0l;
	}
	
	/**
	 * Get a int from a TownyObject's metadata.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param idf IntegerDataField to get from the TownyObject.
	 * @return int from the metadata or 0.
	 */
	public static int getInt(TownyObject townyObject, IntegerDataField idf) {
		CustomDataField<?> cdf = townyObject.getMetadata(idf.getKey());
		if (cdf instanceof IntegerDataField) 
			return ((IntegerDataField) cdf).getValue();
		return 0;				
	}
	
	/**
	 * Get a double from a TownyObject's metadata.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param ddf DecimalDataField to get from the TownyObject.
	 * @return double from the metadata or 0.0.
	 */
	public static double getDouble(TownyObject townyObject, DecimalDataField ddf) {
		CustomDataField<?> cdf = townyObject.getMetadata(ddf.getKey());
		if (cdf instanceof DecimalDataField)
			return ((DecimalDataField) cdf).getValue();
		return 0.0;
	}
	
	/**
	 * Get a byte from a TownyObject's metadata.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param bdf ByteDataField to get from the TownyObject.
	 * @return byte from the metadata or 0.
	 */
	public static byte getByte(TownyObject townyObject, ByteDataField bdf) {
		CustomDataField<?> cdf = townyObject.getMetadata(bdf.getKey());
		if (cdf instanceof ByteDataField)
			return ((ByteDataField) cdf).getValue();
		return 0;
	}
	
	/**
	 * Adds a new StringDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param sdf StringDataField to apply to the TownyObject.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewMeta(TownyObject townyObject, StringDataField sdf, boolean save) {
		townyObject.addMetaData(sdf, save);
	}
	
	/**
	 * Adds a new BooleanDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param bdf BooleanDataField to apply to the TownyObject.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewMeta(TownyObject townyObject, BooleanDataField bdf, boolean save) {
		townyObject.addMetaData(bdf, save);
	}
	
	/**
	 * Adds a new LongDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param ldf LongDataField to apply to the TownyObject.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewMeta(TownyObject townyObject, LongDataField ldf, boolean save) {
		townyObject.addMetaData(ldf, save);
	}
	
	/**
	 * Adds a new IntegerDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param idf IntegerDataField to apply to the TownyObject.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewMeta(TownyObject townyObject, IntegerDataField idf, boolean save) {
		townyObject.addMetaData(idf, save);
	}
	
	/**
	 * Adds a new DecimalDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param ddf DecimalDataField to apply to the TownyObject.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewMeta(TownyObject townyObject, DecimalDataField ddf, boolean save) {
		townyObject.addMetaData(ddf, save);
	}
	
	/**
	 * Adds a new ByteDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param bdf ByteDataField to apply to the TownyObject.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewMeta(TownyObject townyObject, ByteDataField bdf, boolean save) {
		townyObject.addMetaData(bdf, save);
	}
	
	/**
	 * Creates and adds a new StringDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param key String name of the new StringDataField key. 
	 * @param value String value of the StringDataField key.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewStringMeta(TownyObject townyObject, String key, String value, boolean save) {
		addNewMeta(townyObject, new StringDataField(key, value), save);
	}

	/**
	 * Creates and adds a new BooleanDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param key String name of the new BooleanDataField key. 
	 * @param value boolean value of the BooleanDataField key.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewBooleanMeta(TownyObject townyObject, String key, boolean value, boolean save) {
		addNewMeta(townyObject, new BooleanDataField(key, value), save);
	}
	
	/**
	 * Creates and adds a new LongDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param key String name of the new LongDataField key. 
	 * @param value long value of the LongDataField key.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewLongMeta(TownyObject townyObject, String key, long value, boolean save) {
		addNewMeta(townyObject, new LongDataField(key, value), save);
	}
	
	/**
	 * Creates and adds a new IntegerDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param key String name of the new IntegerDataField key. 
	 * @param value long value of the IntegerDataField key.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewIntegerMeta(TownyObject townyObject, String key, int value, boolean save) {
		addNewMeta(townyObject, new IntegerDataField(key, value), save);
	}
	
	/**
	 * Creates and adds a new DecimalDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param key String name of the new DecimalDataField key. 
	 * @param value double value of the DecimalDataField key.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewDoubleMeta(TownyObject townyObject, String key, double value, boolean save) {
		addNewMeta(townyObject, new DecimalDataField(key, value), save);
	}
	
	/**
	 * Creates and adds a new ByteDataField MetaData to a TownyObject, overriding any existing MetaData with the same key.
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param key String name of the new ByteDataField key. 
	 * @param value byte value of the ByteDataField key.
	 * @param save set true to save the object after applying the MetaData.
	 */
	public static void addNewByteMeta(TownyObject townyObject, String key, byte value, boolean save) {
		addNewMeta(townyObject, new ByteDataField(key, value), save);
	}
	
	/**
	 * Sets a StringDataField metadata on a TownyObject.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param sdf StringDataField to apply to the TownyObject.
	 * @param string String to apply to the StringDataField.
	 * @param save True to save the TownyObject after setting the metadata. 
	 */
	public static void setString(TownyObject townyObject, StringDataField sdf, String string, boolean save) {
		CustomDataField<?> cdf = townyObject.getMetadata(sdf.getKey());
		if (cdf instanceof StringDataField) {
			StringDataField value = (StringDataField) cdf;
			value.setValue(string);
			if (save)
				townyObject.save();
		}
	}

	/**
	 * Sets a BooleanDataField metadata on a TownyObject.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param bdf BooleanDataField to apply to the TownyObject.
	 * @param bool boolean to apply to the BooleanDataField.
	 * @param save True to save the TownyObject after setting the metadata. 
	 */
	public static void setBoolean(TownyObject townyObject, BooleanDataField bdf, boolean bool, boolean save) {
		CustomDataField<?> cdf = townyObject.getMetadata(bdf.getKey());
		if (cdf instanceof BooleanDataField) {
			BooleanDataField value = (BooleanDataField) cdf;
			value.setValue(bool);
			if (save)
				townyObject.save();
		}
	}

	/**
	 * Sets a LongDataField metadata on a TownyObject.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param ldf LongDataField to apply to the TownyObject.
	 * @param num long to apply to the LongDataField.
	 * @param save True to save the TownyObject after setting the metadata. 
	 */
	public static void setLong(TownyObject townyObject, LongDataField ldf, long num, boolean save) {
		CustomDataField<?> cdf = townyObject.getMetadata(ldf.getKey());
		if (cdf instanceof LongDataField) {
			LongDataField value = (LongDataField) cdf;
			value.setValue(num);
			if (save)
				townyObject.save();
		}
	}

	/**
	 * Sets a IntegerDataField metadata on a TownyObject.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param idf IntegerDataField to apply to the TownyObject.
	 * @param num int to apply to the IntegerDataField.
	 * @param save True to save the TownyObject after setting the metadata. 
	 */
	public static void setInt(TownyObject townyObject, IntegerDataField idf, int num, boolean save) {
		CustomDataField<?> cdf = townyObject.getMetadata(idf.getKey());
		if (cdf instanceof IntegerDataField) {
			IntegerDataField value = (IntegerDataField) cdf;
			value.setValue(num);
			if (save)
				townyObject.save();
		}
	}
	
	/**
	 * Sets a DecimalDataField metadata on a TownyObject.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param ddf DecimalDataField to apply to the TownyObject.
	 * @param num double to apply to the DecimalDataField.
	 * @param save True to save the TownyObject after setting the metadata. 
	 */
	public static void setDouble(TownyObject townyObject, DecimalDataField ddf, double num, boolean save) {
		CustomDataField<?> cdf = townyObject.getMetadata(ddf.getKey());
		if (cdf instanceof DecimalDataField) {
			DecimalDataField value = (DecimalDataField) cdf;
			value.setValue(num);
			if (save)
				townyObject.save();
		}
	}
	
	/**
	 * Sets a ByteDataField metadata on a TownyObject.
	 * 
	 * @param townyObject TownyObject, ie: Resident, Town, Nation, TownBlock.
	 * @param bdf ByteDataField to apply to the TownyObject.
	 * @param num value to apply to the ByteDataField.
	 * @param save True to save the TownyObject after setting the metadata. 
	 */
	public static void setByte(TownyObject townyObject, ByteDataField bdf, byte num, boolean save) {
		CustomDataField<?> cdf = townyObject.getMetadata(bdf.getKey());
		if (cdf instanceof ByteDataField) {
			ByteDataField value = (ByteDataField) cdf;
			value.setValue(num);
			if (save)
				townyObject.save();
		}
	}
}