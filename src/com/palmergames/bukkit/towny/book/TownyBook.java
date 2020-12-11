package com.palmergames.bukkit.towny.book;

import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;

public class TownyBook {

	final static String NEWLINE= "\n";
	final static String NEWPARAGRAPH = "\n\n  ";
	
	
	/*
	 * The idea here being a new method for each book or book-section, returning a List<String> which can be fed into the Book factory.
	 */
	public List<String> getTownClaimNewDefaults() {
		List<String> out = new ArrayList<>();
		
		out.add(NEWPARAGRAPH);
		out.add("On creation a town will get the following settings by default:" + NEWLINE);
		out.add("Public status: " + TownySettings.getTownDefaultPublic() + NEWLINE);              // Some sections will end up being true/false bullet point style.
		out.add("Open status: " + TownySettings.getTownDefaultOpen() + NEWLINE);
		out.add("Default town board: " + TownySettings.getTownDefaultBoard() + NEWLINE);
		out.add("Default tax settings: " + NEWLINE);
		/*
		 * Enter various tax settings
		 */
		out.add("Daily tax on residents: " + TownySettings.getTownDefaultTax() + NEWLINE);
		out.add("This tax is a percentage: " + TownySettings.getTownDefaultTaxPercentage() + NEWLINE);
		out.add("Plot taxes: " + NEWLINE);
		out.add("Normal plots: " + TownySettings.getTownDefaultPlotTax() + NEWLINE);
		out.add("Shop plots: " + TownySettings.getTownDefaultShopTax() + NEWLINE);
		out.add("Embassy plots: " + TownySettings.getTownDefaultEmbassyTax() + NEWLINE);
		
		out.add(NEWPARAGRAPH);
		out.add("Bonus plot info: " + NEWLINE);
		if (!TownySettings.isBonusBlocksPerTownLevel())
			out.add("Max bonus blocks purchasable: " + TownySettings.getInt(ConfigNodes.TOWN_MAX_PURCHASED_BLOCKS) + NEWLINE);
		else
			out.add("Purchased bonus plots depend on town level.");

		out.add("The maximum number of Towns allowed to exist on the server is " + TownySettings.getTownLimit() + ". ");  // While other sections can be written out in fairly good english.
		out.add("The minimum distance tests " + (TownySettings.isMinDistanceIgnoringTownsInSameNation() ? "will be " : "won't be ") + "ignored for towns in the same nation.");
		out.add("Minimum distance between any two towns' plots is " + TownySettings.getMinDistanceFromTownPlotblocks() + 
				" and the minimum distance between a town's plot and another town's homeblock is " + TownySettings.getMinDistanceFromTownHomeblocks() + "." + NEWLINE);

		
		return out;
	}
}
