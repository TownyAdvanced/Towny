package com.palmergames.bukkit.towny.war.siegewar.metadata;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;
import com.palmergames.bukkit.towny.object.metadata.LongDataField;

public class TownMetaDataController {

	@SuppressWarnings("unused")
	private Towny plugin;
	private static IntegerDataField peacefulnessChangeConfirmationCounterDays = new IntegerDataField("siegewar_peacefuldays", 0, "Days To Peacefulness Status Change");
	private static BooleanDataField desiredPeacefulness = new BooleanDataField("siegewar_desiredPeaceSetting", false);
	private static LongDataField revoltImmunityEndTime = new LongDataField("siegewar_revoltImmunityEndTime", 0l);
	private static LongDataField siegeImmunityEndTime = new LongDataField("siegewar_siegeImmunityEndTime", 0l);
	
	public TownMetaDataController(Towny plugin) {
		this.plugin = plugin;
	}
	
	public static int getPeacefulnessChangeConfirmationCounterDays(Town town) {
		int days = 0;
		IntegerDataField idf = (IntegerDataField) peacefulnessChangeConfirmationCounterDays.clone();
		if (town.hasMeta(idf.getKey())) {
			CustomDataField<?> cdf = town.getMetadata(idf.getKey());
			if (cdf instanceof IntegerDataField) {
				IntegerDataField amount = (IntegerDataField) cdf; 
				days = amount.getValue();
			}
		}
		return days;
	}

	public static void setPeacefulnessChangeDays(Town town, int days) {
		IntegerDataField idf = (IntegerDataField) peacefulnessChangeConfirmationCounterDays.clone();
		if (town.hasMeta(idf.getKey())) {
			if (days == 0) {
				town.removeMetaData(idf);
				return;
			}
			CustomDataField<?> cdf = town.getMetadata(idf.getKey());
			if (cdf instanceof IntegerDataField) {
				IntegerDataField amount = (IntegerDataField) cdf;
				amount.setValue(days);
				TownyUniverse.getInstance().getDataSource().saveTown(town);
			}
		} else {
			town.addMetaData(new IntegerDataField("siegewar_peacefuldays", days, "Days To Peacefulness Status Change"));			
		}
	}
	
	public static boolean getDesiredPeacefulnessSetting(Town town) {
		BooleanDataField bdf = (BooleanDataField) desiredPeacefulness.clone();
		if (town.hasMeta(bdf.getKey())) {
			CustomDataField<?> cdf = town.getMetadata(bdf.getKey());
			if (cdf instanceof BooleanDataField) {
				BooleanDataField bool = (BooleanDataField) bdf;
				return bool.getValue();
			}
		}
		return false;
	}
	
	public static void setDesiredPeacefullnessSetting(Town town, boolean bool) {
		BooleanDataField bdf = (BooleanDataField) desiredPeacefulness.clone();
		if (town.hasMeta(bdf.getKey())) {
			CustomDataField<?> cdf = town.getMetadata(bdf.getKey());
			if (cdf instanceof BooleanDataField) {
				BooleanDataField value = (BooleanDataField) bdf;
				value.setValue(bool);
				TownyUniverse.getInstance().getDataSource().saveTown(town);
			}
		} else {
			town.addMetaData(new BooleanDataField("siegewar_desiredPeaceSetting", bool));
		}
	}
	
	public static long getRevoltImmunityEndTime(Town town) {
		LongDataField ldf = (LongDataField) revoltImmunityEndTime.clone();
		if (town.hasMeta(ldf.getKey())) {
			CustomDataField<?> cdf = town.getMetadata(ldf.getKey());
			if (cdf instanceof LongDataField) {
				LongDataField value = (LongDataField) ldf;
				return value.getValue();
			}
		}
		return 0l;
	}
	
	public static void setRevoltImmunityEndTime(Town town, long time) {
		LongDataField ldf = (LongDataField) revoltImmunityEndTime.clone();
		if (time == 0) {
			town.removeMetaData(ldf);
			return;
		}
		if (town.hasMeta(ldf.getKey())) {
			CustomDataField<?> cdf = town.getMetadata(ldf.getKey());
			if (cdf instanceof LongDataField) {
				LongDataField value = (LongDataField) ldf;
				value.setValue(time);
				TownyUniverse.getInstance().getDataSource().saveTown(town);
			}
		} else {
			town.addMetaData(new LongDataField("siegewar_revoltImmunityEndTime", time));
		}
	}
	
	public static long getSiegeImmunityEndTime(Town town) {
		LongDataField ldf = (LongDataField) siegeImmunityEndTime.clone();
		if (town.hasMeta(ldf.getKey())) {
			CustomDataField<?> cdf = town.getMetadata(ldf.getKey());
			if (cdf instanceof LongDataField) {
				LongDataField value = (LongDataField) ldf;
				return value.getValue();
			}
		}
		return 0l;
	}
	
	public static void setSiegeImmunityEndTime(Town town, long time) {
		LongDataField ldf = (LongDataField) siegeImmunityEndTime.clone();
		if (time == 0) {
			town.removeMetaData(ldf);
			return;
		}
		if (town.hasMeta(ldf.getKey())) {
			CustomDataField<?> cdf = town.getMetadata(ldf.getKey());
			if (cdf instanceof LongDataField) {
				LongDataField value = (LongDataField) ldf;
				value.setValue(time);
				TownyUniverse.getInstance().getDataSource().saveTown(town);
			}
		} else {
			town.addMetaData(new LongDataField("siegewar_siegeImmunityEndTime", time));
		}
	}
}
