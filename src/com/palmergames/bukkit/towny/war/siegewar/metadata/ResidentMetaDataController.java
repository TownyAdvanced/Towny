package com.palmergames.bukkit.towny.war.siegewar.metadata;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.IntegerDataField;

public class ResidentMetaDataController {

	@SuppressWarnings("unused")
	private Towny plugin;
	private static IntegerDataField refundAmount = new IntegerDataField("siegewar_nationrefund", 0, "Nation Refund");
	
	public ResidentMetaDataController(Towny plugin) {
		this.plugin = plugin;
	}
	
	public static int getNationRefundAmount(Resident resident) {
		int nationRefundAmount = 0;
		IntegerDataField idf = (IntegerDataField) refundAmount.clone();
		if (resident.hasMeta(idf.getKey())) {
			CustomDataField<?> cdf = resident.getMetadata(idf.getKey());
			if (cdf instanceof IntegerDataField) {
				IntegerDataField amount = (IntegerDataField) cdf; 
				nationRefundAmount = amount.getValue();
			}
		}
		return nationRefundAmount;
	}

	public static void setNationRefundAmount(Resident resident, int nationRefundAmount) {
		IntegerDataField idf = (IntegerDataField) refundAmount.clone();
		if (resident.hasMeta(idf.getKey())) {
			resident.removeMetaData(idf);
		} else {
			resident.addMetaData(new IntegerDataField("siegewar_nationrefund", nationRefundAmount, "Nation Refund"));			
		}
	}
}
