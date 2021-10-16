package com.palmergames.bukkit.towny.war.eventwar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;
import com.palmergames.util.StringMgmt;

public class WarBooks {

	private static String newline = "\n";
	private static String newParagraph = "\n\n";
	private static final SimpleDateFormat warDateFormat = new SimpleDateFormat("MMM d YY '@' HH:mm");

	/**
	 * Creates the first book given to players in the war.
	 * 
	 * @param war War instance.
	 * @return String containing the raw text of what will become a book.
	 */
	public static String warStartBook(War war) {
		WarType warType = war.getWarType();
		/*
		 * Flashy Header.
		 */
		String text = "oOo War Declared! oOo" + newline;
		text += "-" + warDateFormat.format(System.currentTimeMillis()) + "-" + newline;
		text += "-------------------" + newline;
		
		/*
		 * Add who is involved.
		 */
		switch(warType) {
			case WORLDWAR:
				
				text += "War has broken out across all enemied nations!" + newline;
				text += newline;
				text += "The following nations have joined the battle: " + newline;
				for (Nation nation : war.getWarParticipants().getNations())
					text+= "* " + nation.getName() + newline;
				text += newline;
				text += "May the victors bring glory to their nation!";			
				break;
				
			case NATIONWAR:
				
				text += "War has broken out between two nations:" + newline;
				for (Nation nation : war.getWarParticipants().getNations())
					text+= "* " + nation.getName() + newline;
				text += newline;
				text += "May the victor bring glory to their nation!";
				break;
				
			case CIVILWAR:
				
				text += String.format("Civil war has broken out in the nation of %s!",war.getWarParticipants().getNations().get(0).getName()) + newline ;
				text += newline;
				text += "On the side of the nation's capital " + war.getWarParticipants().getNations().get(0).getName() + ":" + newline;
				for (TownyObject town : war.getWarParticipants().getGovSide())
					text+= "* " + ((Town)town).getName() + newline;
				text += newline;
				text += "And taking up arms in rebelion against the capital:" + newline;
				for (TownyObject town : war.getWarParticipants().getRebSide())
					text+= "* " + ((Town)town).getName() + newline;
				text += newline;
				text += "May the victor bring peace to their nation!";
				break;
				
			case TOWNWAR:
				
				text += "War has broken out between two towns:";
				for (Town town : war.getWarParticipants().getTowns())
					text+= newline + "* " + town.getName();
				text += newline;
				text += "May the victor bring glory to their town!";
				break;
				
			case RIOT:
				
				text += String.format("A riot has broken out in the town of %s!", war.getWarParticipants().getTowns().get(0).getName()) + newline;
				text += newline;
				text += "Aligned with the mayor and the city:" + newline;
				for (TownyObject res : war.getWarParticipants().getGovSide())
					text+= "* " + ((Resident)res).getName() + newline;
				text += newline;
				text += "Amongst the unruly mob:" + newline;
				for (TownyObject res : war.getWarParticipants().getRebSide())
					text+= "* " + ((Resident)res).getName() + newline;

				text += newline;
				text += "The last man standing will be the leader, but what will remain?!";
				break;
		}
		
		/*
		 * Add scoring types and winnings at stake.
		 */
		text += newline;
		text += "-------------------" + newParagraph;
		text += "War Rules:" + newline;
		if (warType.hasTownBlockHP) {
			text += "Town blocks will have an HP stat. " + newline;
			text += "Regular Townblocks have an HP of " + TownySettings.getWarzoneTownBlockHealth() + ". ";
			text += "Homeblocks have an HP of " + TownySettings.getWarzoneHomeBlockHealth() + ". ";
			text += "Townblocks lose HP when enemies stand anywhere inside of the plot above Y level " + TownySettings.getMinWarHeight() + ". ";
			if (TownySettings.getPlotsHealableInWar())
				text += "Townblocks that have not dropped all the way to 0 hp are healable by town members and their allies. ";
			if (TownySettings.getOnlyAttackEdgesInWar())
				text += "Only edge plots will be attackable at first, so protect your borders and at all costs. ";
			text += "Do not let the enemy drop your homeblock to 0 hp! ";
			if (warType.hasTownBlocksSwitchTowns)
				text += "Townblocks which drop to 0 hp will be taken over by the attacker permanently! ";
			else
				text += "Townblocks which drop to 0 hp will not change ownership after the war. ";
		}

		if (warType.hasTownConquering) {
			switch(warType) {
			case TOWNWAR:
				text += "The town which wins the war will take over all of the land and residents of the losing town. ";
				break;
			case CIVILWAR:
				text += "The town which wins the war will take over control of the nation as the new capital city. ";
				break;
			case NATIONWAR:
			case WORLDWAR:
				text += "The towns which are defeated by the opposing nation will change sides and join the victorious nation. ";
				if (TownySettings.getWarEventConquerTime() > 0)
					text += "These towns will be conquered for " + TownySettings.getWarEventConquerTime() + " days. ";
				break;
			case RIOT:
				text += "The player who survives with the highest score will become the new town mayor. ";
				break;
			default:
				break;
			
			}
		}
	
		text += newParagraph;
		if (!warType.equals(WarType.RIOT) && warType.hasMayorDeath)
			text += newline + "If your mayor runs out of lives your nation or town will be removed from the war! ";

		if (warType.residentLives > 0)
			text += newline + "Normal residents will start with " + warType.residentLives + (warType.residentLives == 1 ? " life.":" lives.") + " If you run out of lives and die again you will be removed from the war. ";
		else
			text += newline + "Residents have unlimited lives. ";

		if (warType.mayorLives > 0)
			text += newline + "Mayors start with " + warType.mayorLives + (warType.mayorLives == 1 ? " life.":" lives.") + " If you run out of lives and die again you will be removed from the war. ";
		else
			text += newline + "Mayors have unlimited lives. ";		

		text += newParagraph;
		text += "WarSpoils up for grabs at the end of this war: " + TownyEconomyHandler.getFormattedBalance(war.warSpoilsAtStart);
		
		return text;
	}


	/**
	 * Creates a book to be given to players mid-war.
	 * 
	 * @param war War instance.
	 * @return String containing the raw text of what will become a book.
	 */
	public static String warUpdateBook(War war) {
		WarType warType = war.getWarType();
		/*
		 * Flashy Header.
		 */
		String text = "oOo Extra Extra! oOo" + newline;
		text += "-" + warDateFormat.format(System.currentTimeMillis()) + "-" + newline;
		text += "-------------------" + newline;
		
		text += "The " + war.getWarName() + " continues on." + newParagraph;
		
		
		/*
		 * Add who is involved.
		 */
		switch(warType) {
		case WORLDWAR:
			
			text += "The fighting rages on in these nations: " + newline;
			for (Nation nation : war.getWarParticipants().getNations())
				text+= "* " + nation.getName() + newline;
			text += newline;
			break;
			
		case NATIONWAR:
			
			for (Nation nation : war.getWarParticipants().getNations()) {
				text+= nation.getFormattedName() + " has the following towns still in play:" + newline; 
				for (Town town : nation.getTowns()) {
					if (war.getWarParticipants().getTowns().contains(town))
						text += "* " + town.getName() + newline; 
				}
				text += newline;
			}
			break;
			
		case CIVILWAR:

			text += "The following towns battle for the fate of their nation:" + newline;
			text += "On the side of the nation's capital " + war.getWarParticipants().getNations().get(0).getName() + ":" + newline;
			for (TownyObject town : war.getWarParticipants().getGovSide())
				text+= "* " + ((Town)town).getName() + newline;
			text += newline;
			text += "And continuing in their rebelion against the capital:" + newline;
			for (TownyObject town : war.getWarParticipants().getRebSide())
				text+= "* " + ((Town)town).getName() + newline;
			text += newline;
			break;
			
		case TOWNWAR:
			for (Town town : war.getWarParticipants().getTowns()) {
				text+= town.getName() + " fights on with the following soldiers:" + newline;
				List<String> fighters = new ArrayList<>();
				for (Resident resident : town.getResidents())					
					if (war.getWarParticipants().getResidents().contains(resident))
						fighters.add(resident.getName());
				text += StringMgmt.join(fighters, ", ") + ".";
				text += newParagraph;
			}
			break;
			
		case RIOT:
			text+= "The following residents remain in the fray:" + newline;
			text += "Aligned with the mayor and the city:" + newline;
			for (TownyObject res : war.getWarParticipants().getGovSide())
				text+= "* " + ((Resident)res).getName() + newline;
			text += newline;
			text += "And continuing to sew unrest:" + newline;
			for (TownyObject res : war.getWarParticipants().getRebSide())
				text+= "* " + ((Resident)res).getName() + newline;
			text += newline;
			break;
		}
		
		text += "Current Scores:" + newParagraph;
		List<String> scores = war.getScoreManager().getScores(-1, false);
		for (String line : scores)
			text += line + newline;
		
		return text;
	}
}
