package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;

public class TownTaxCollector implements TaxCollector {
	
	private double taxes = TownySettings.getTownDefaultTax();
	private boolean isTaxPercentage = TownySettings.getTownDefaultTaxPercentage();;
	private double commercialPlotTax = TownySettings.getTownDefaultShopTax();
	private double plotTax = TownySettings.getTownDefaultPlotTax();;
	private double embassyPlotTax = TownySettings.getTownDefaultEmbassyTax();;
	private double maxPercentTaxAmount = TownySettings.getMaxTownTaxPercentAmount();
	
	@Override
	public double getTaxes() {
		return taxes;
	}

	@Override
	public void setTaxes(double taxes) {
		this.taxes = Math.min(taxes, isTaxPercentage() ? TownySettings.getMaxTownTaxPercent() : TownySettings.getMaxTownTax());

		// Fix invalid taxes
		if (this.taxes < 0)
			this.taxes = TownySettings.getTownDefaultTax();
	}

	public void setPlotTax(double plotTax) {
		this.plotTax = Math.min(plotTax, TownySettings.getMaxPlotTax());
	}

	public double getPlotTax() {
		return plotTax;
	}

	public void setCommercialPlotTax(double commercialTax) {
		this.commercialPlotTax = Math.min(commercialTax, TownySettings.getMaxPlotTax());
	}

	public double getCommercialPlotTax() {
		return commercialPlotTax;
	}

	public void setEmbassyPlotTax(double embassyPlotTax) {
		this.embassyPlotTax = Math.min(embassyPlotTax, TownySettings.getMaxPlotTax());
	}

	public double getEmbassyPlotTax() {
		return embassyPlotTax;
	}
	
	public boolean isTaxPercentage() {
		return isTaxPercentage;
	}

	public double getMaxPercentTaxAmount() {
		return maxPercentTaxAmount;
	}

	public void setMaxPercentTaxAmount(double maxPercentTaxAmount) {
		// Max tax amount cannot go over amount defined in config.
		this.maxPercentTaxAmount = Math.min(maxPercentTaxAmount, TownySettings.getMaxTownTaxPercentAmount());
	}

	public void setTaxPercentage(boolean isPercentage) {

		this.isTaxPercentage = isPercentage;
		if (this.getTaxes() > 100) {
			this.setTaxes(0);
		}
	}
}
