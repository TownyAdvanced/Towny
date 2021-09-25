package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;
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
}