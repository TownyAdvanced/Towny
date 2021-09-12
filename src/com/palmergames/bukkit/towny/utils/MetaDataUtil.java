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

	public static String getString(TownyObject townyObject, StringDataField sdf) {
		CustomDataField<?> cdf = townyObject.getMetadata(sdf.getKey());
		if (cdf instanceof StringDataField) {
			return ((StringDataField) cdf).getValue();
		}
		return "";
	}

	public static boolean getBoolean(TownyObject townyObject, BooleanDataField bdf) {
		CustomDataField<?> cdf = townyObject.getMetadata(bdf.getKey());
		if (cdf instanceof BooleanDataField)
			return ((BooleanDataField) cdf).getValue();
		return false;
	}

	public static long getLong(TownyObject townyObject, LongDataField ldf) {
		CustomDataField<?> cdf = townyObject.getMetadata(ldf.getKey());
		if (cdf instanceof LongDataField)
			return ((LongDataField) cdf).getValue();
		return 0l;
	}
	
	public static int getInt(TownyObject townyObject, IntegerDataField idf) {
		CustomDataField<?> cdf = townyObject.getMetadata(idf.getKey());
		if (cdf instanceof IntegerDataField) 
			return ((IntegerDataField) cdf).getValue();
		return 0;				
	}
	
	public static double getDouble(TownyObject townyObject, DecimalDataField ddf) {
		CustomDataField<?> cdf = townyObject.getMetadata(ddf.getKey());
		if (cdf instanceof DecimalDataField)
			return ((DecimalDataField) cdf).getValue();
		return 0.0;
	}

	public static void setString(TownyObject townyObject, StringDataField sdf, String string) {
		CustomDataField<?> cdf = townyObject.getMetadata(sdf.getKey());
		if (cdf instanceof StringDataField) {
			StringDataField value = (StringDataField) cdf;
			value.setValue(string);
			townyObject.save();
		}
	}

	public static void setBoolean(TownyObject townyObject, BooleanDataField bdf, boolean bool) {
		CustomDataField<?> cdf = townyObject.getMetadata(bdf.getKey());
		if (cdf instanceof BooleanDataField) {
			BooleanDataField value = (BooleanDataField) cdf;
			value.setValue(bool);
			townyObject.save();
		}
	}

	public static void setLong(TownyObject townyObject, LongDataField ldf, long num) {
		CustomDataField<?> cdf = townyObject.getMetadata(ldf.getKey());
		if (cdf instanceof LongDataField) {
			LongDataField value = (LongDataField) cdf;
			value.setValue(num);
			townyObject.save();
		}
	}

	public static void setInt(TownyObject townyObject, IntegerDataField idf, int num) {
		CustomDataField<?> cdf = townyObject.getMetadata(idf.getKey());
		if (cdf instanceof IntegerDataField) {
			IntegerDataField value = (IntegerDataField) cdf;
			value.setValue(num);
			townyObject.save();
		}
	}
	
	public static void setDouble(TownyObject townyObject, DecimalDataField ddf, double num) {
		CustomDataField<?> cdf = townyObject.getMetadata(ddf.getKey());
		if (cdf instanceof DecimalDataField) {
			DecimalDataField value = (DecimalDataField) cdf;
			value.setValue(num);
			townyObject.save();
		}
	}
}