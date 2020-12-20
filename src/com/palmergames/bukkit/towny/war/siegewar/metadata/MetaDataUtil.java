package com.palmergames.bukkit.towny.war.siegewar.metadata;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.DecimalDataField;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;
import com.palmergames.bukkit.towny.object.metadata.LongDataField;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;

class MetaDataUtil {

	private static void saveTown(Town town) {
		TownyUniverse.getInstance().getDataSource().saveTown(town);
	}

	static String getString(Town town, StringDataField sdf) {
		CustomDataField<?> cdf = town.getMetadata(sdf.getKey());
		if (cdf instanceof StringDataField) {
			return ((StringDataField) cdf).getValue();
		}
		return "";
	}

	static boolean getBoolean(Town town, BooleanDataField bdf) {
		CustomDataField<?> cdf = town.getMetadata(bdf.getKey());
		if (cdf instanceof BooleanDataField)
			return ((BooleanDataField) cdf).getValue();
		return false;
	}

	static long getLong(Town town, LongDataField ldf) {
		CustomDataField<?> cdf = town.getMetadata(ldf.getKey());
		if (cdf instanceof LongDataField)
			return ((LongDataField) cdf).getValue();
		return 0l;
	}
	
	static int getInt(Town town, IntegerDataField idf) {
		CustomDataField<?> cdf = town.getMetadata(idf.getKey());
		if (cdf instanceof IntegerDataField) 
			return ((IntegerDataField) cdf).getValue();
		return 0;				
	}
	
	static double getDouble(Town town, DecimalDataField ddf) {
		CustomDataField<?> cdf = town.getMetadata(ddf.getKey());
		if (cdf instanceof DecimalDataField)
			return ((DecimalDataField) cdf).getValue();
		return 0.0;
	}

	static void setString(Town town, StringDataField sdf, String string) {
		CustomDataField<?> cdf = town.getMetadata(sdf.getKey());
		if (cdf instanceof StringDataField) {
			StringDataField value = (StringDataField) cdf;
			value.setValue(string);
			saveTown(town);
		}
	}

	static void setBoolean(Town town, BooleanDataField bdf, boolean bool) {
		CustomDataField<?> cdf = town.getMetadata(bdf.getKey());
		if (cdf instanceof BooleanDataField) {
			BooleanDataField value = (BooleanDataField) cdf;
			value.setValue(bool);
			saveTown(town);
		}
	}

	static void setLong(Town town, LongDataField ldf, long num) {
		CustomDataField<?> cdf = town.getMetadata(ldf.getKey());
		if (cdf instanceof LongDataField) {
			LongDataField value = (LongDataField) cdf;
			value.setValue(num);
			saveTown(town);
		}
	}
	
	static void setInt(Town town, IntegerDataField idf, int num) {
		CustomDataField<?> cdf = town.getMetadata(idf.getKey());
		if (cdf instanceof IntegerDataField) {
			IntegerDataField value = (IntegerDataField) cdf;
			value.setValue(num);
			saveTown(town);
		}
	}
	
	static void setDouble(Town town, DecimalDataField ddf, double num) {
		CustomDataField<?> cdf = town.getMetadata(ddf.getKey());
		if (cdf instanceof DecimalDataField) {
			DecimalDataField value = (DecimalDataField) cdf;
			value.setValue(num);
			saveTown(town);
		}
	}
}
