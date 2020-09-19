package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;

public class NationTaxCollector implements TaxCollector {
	
	double taxes;
	
	@Override
	public double getTaxes() {
		return taxes;
	}

	@Override
	public void setTaxes(double taxes) {
		this.taxes = Math.min(taxes, TownySettings.getMaxNationTax());
	}
}
