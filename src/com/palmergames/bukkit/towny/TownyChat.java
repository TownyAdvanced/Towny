package com.palmergames.bukkit.towny;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;

/**
 * Towny Plugin for Bukkit
 * 
 * Website: http://code.google.com/a/eclipselabs.org/p/towny/
 * Source: http://code.google.com/a/eclipselabs.org/p/towny/source/browse/
 * 
 * @author ElgarL
 */

public class TownyChat {

	public static void setDisplayName (Towny plugin, Player player) {
                
		// Setup the chat prefix BEFORE we speak.
        if (TownySettings.isUsingModifyChat()) {
        	
        	Resident resident;
        	Nation nation = null;
        	Town town = null;
        	boolean update = false;
        	
			try {
				resident = plugin.getTownyUniverse().getResident(player.getName());
				
				//Flag if we need to update our chatFormattedName
				if (resident.isChangedName())
					update = true;
				
				if (resident.hasTown()) {
					town = resident.getTown();
					if (town.isChangedName())
						update = true;
				}
				
				if (resident.hasNation()) {
					nation = town.getNation();
					if (nation.isChangedName())
						update = true;
				}
				
				if (update) {
					String colour, formattedName = "";
	                if (resident.isKing())
	                        colour = TownySettings.getKingColour();
	                else if (resident.isMayor())
	                        colour = TownySettings.getMayorColour();
	                else
	                        colour = "";
	                formattedName = TownySettings.getModifyChatFormat();
	                String nationName = "",townName = "";
	                
	                if (nation != null) {
	                	nationName = nation.hasTag() ? "[" + nation.getTag() + "]" : "[" + nation.getName() + "]";
	                } else if ((resident.hasTitle()) || (resident.hasSurname())) {
	                	resident.setTitle(" ");
	                	resident.setSurname(" ");
	                }
	                
	                if (town != null) {
	                	townName = town.hasTag() ? "[" + town.getTag() + "]" : "[" + town.getName() + "]";
	                }
	                
	                formattedName = formattedName.replace("{nation}", nationName);
	                formattedName = formattedName.replace("{town}", townName);
	                formattedName = formattedName.replace("{permprefix}", TownyUniverse.getPermissionSource().getPrefixSuffix(resident, "prefix"));
	                formattedName = formattedName.replace("{townynameprefix}", resident.hasTitle() ? resident.getTitle() : TownyFormatter.getNamePrefix(resident));
	                formattedName = formattedName.replace("{playername}", "%1$s");
	                formattedName = formattedName.replace("{modplayername}", player.getDisplayName().replace(player.getName(), "%1$s"));
	                formattedName = formattedName.replace("{townynamepostfix}", resident.hasSurname() ? resident.getSurname() : TownyFormatter.getNamePostfix(resident));
	                formattedName = formattedName.replace("{permsuffix}", TownyUniverse.getPermissionSource().getPrefixSuffix(resident, "suffix"));
	                
	                formattedName = ChatTools.parseSingleLineString(colour + formattedName + Colors.White).trim();

	                resident.setChatFormattedName(formattedName);
				}
				
					
			} catch (NotRegisteredException e) {
				plugin.log("Not Registered");
			}
        	
        }               
	}
	
	public static void parseTownChatCommand(Towny plugin, Player player, String msg) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Town town = resident.getTown();
			
			//String prefix = TownySettings.getModifyChatFormat().contains("{town}") ? "" : "[" + town.getName() + "] ";
			String line = Colors.Blue + "[TC] " // + prefix
					+ resident.getChatFormattedName().replace("%1$s", player.getName())
					+ Colors.White + ": "
					+ Colors.LightBlue + msg;
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(line));
		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}

	public static void parseNationChatCommand(Towny plugin, Player player, String msg) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Nation nation = resident.getTown().getNation();
			
			//String prefix = TownySettings.getModifyChatFormat().contains("{nation}") ? "" : "[" + nation.getName() + "] ";
			String line = Colors.Gold + "[NC] " // + prefix
					+ resident.getChatFormattedName().replace("%1$s", player.getName())
					+ Colors.White + ": "
					+ Colors.Yellow + msg;
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(line));
		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
	
}