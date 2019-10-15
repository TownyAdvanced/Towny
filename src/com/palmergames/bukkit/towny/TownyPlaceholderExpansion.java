package com.palmergames.bukkit.towny;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * This class will be registered through the register-method in the 
 * plugins onEnable-method.
 */
public class TownyPlaceholderExpansion extends PlaceholderExpansion {

    private Towny plugin;

    /**
     * Since we register the expansion inside our own plugin, we
     * can simply use this method here to get an instance of our
     * plugin.
     *
     * @param plugin
     *        The instance of our plugin.
     */
    public TownyPlaceholderExpansion(Towny plugin){
        this.plugin = plugin;
    }

    /**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
    @Override
    public boolean persist(){
        return true;
    }

    /**
     * Because this is a internal class, this check is not needed
     * and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister(){
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     * <br>For convienience do we return the author from the plugin.yml
     * 
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor(){
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest 
     * method to obtain a value if a placeholder starts with our 
     * identifier.
     * <br>This must be unique and can not contain % or _
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier(){
        return "townyadvanced";
    }

    /**
     * This is the version of the expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     *
     * For convienience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    /**
     * This is the method called when a placeholder with our identifier 
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param  player
     *         A {@link org.bukkit.Player Player}.
     * @param  identifier
     *         A String containing the identifier/value.
     *
     * @return possibly-null String of the requested identifier.
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier){

        if(player == null){
            return "";
        }
        Resident resident;
		try {
			resident = TownyAPI.getInstance().getDataSource().getResident(player.getName());
		} catch (NotRegisteredException e) {
			return null;
		}
		

        // %townyadvanced_town%
        if(identifier.equals("town")){
        	String town = "";
        	try {
				town = String.format(TownySettings.getPAPIFormattingTown(), resident.getTown().getName());
			} catch (NotRegisteredException ignored) {
			}        	
            return town;
        }

        // %townyadvanced_town_formatted%
        if(identifier.equals("town_formatted")){
        	String town = "";
        	try {
				town = String.format(TownySettings.getPAPIFormattingTown(), resident.getTown().getFormattedName());
			} catch (NotRegisteredException ignored) {
			}        	
            return town;
        }

        // %townyadvanced_nation%
        if(identifier.equals("nation")){
        	String nation = "";
        	try {
				nation = String.format(TownySettings.getPAPIFormattingNation(), resident.getTown().getNation().getName());				
			} catch (NotRegisteredException ignored) {
			}        	
            return nation;
        }
        
        // %townyadvanced_nation_formatted%
        if(identifier.equals("nation_formatted")){
        	String nation = "";
        	try {
				nation = String.format(TownySettings.getPAPIFormattingNation(), resident.getTown().getNation().getFormattedName());
			} catch (NotRegisteredException ignored) {
			}        	
            return nation;
        }
        
        // %townyadvanced_town_balance%
        if(identifier.equals("town_balance")){
        	String balance = "";
        	try {
				balance = resident.getTown().getHoldingFormattedBalance();
			} catch (NotRegisteredException ignored) {
			}        	
            return balance;
        }

        // %townyadvanced_nation_balance%
        if(identifier.equals("nation_balance")){
        	String balance = "";
        	try {
				balance = resident.getTown().getNation().getHoldingFormattedBalance();
			} catch (NotRegisteredException ignored) {
			}        	
            return balance;
        }
        
        // %townyadvanced_town_tag%
        if(identifier.equals("town_tag")){
        	String tag = "";
        	try {
       			tag = String.format(TownySettings.getPAPIFormattingTown(), resident.getTown().getTag());
			} catch (NotRegisteredException ignored) {
			}        	
            return tag;
        }

        // %townyadvanced_town_tag_override%
        if(identifier.equals("town_tag_override")){
        	String tag = "";
        	try {
        		if (resident.getTown().hasTag())
        			tag = String.format(TownySettings.getPAPIFormattingTown(), resident.getTown().getTag());
        		else 
        			tag = String.format(TownySettings.getPAPIFormattingTown(), resident.getTown().getName());
        	} catch (NotRegisteredException ignored) {
			}        	
            return tag;
        }
        
        // %townyadvanced_nation_tag%
        if(identifier.equals("nation_tag")){
        	String tag = "";
        	try {
				tag = String.format(TownySettings.getPAPIFormattingNation(), resident.getTown().getNation().getTag());
			} catch (NotRegisteredException ignored) {
			}        	
            return tag;
        }
        
        // %townyadvanced_nation_tag_override%
        if(identifier.equals("nation_tag_override")){
        	String tag = "";
        	try {
        		if (resident.getTown().getNation().hasTag())
        			tag = String.format(TownySettings.getPAPIFormattingNation(), resident.getTown().getNation().getTag());
        		else 
        			tag = String.format(TownySettings.getPAPIFormattingNation(), resident.getTown().getNation().getName());
        	} catch (NotRegisteredException ignored) {
			}        	
            return tag;
        }
        
        // %townyadvanced_towny_tag%
        if(identifier.equals("towny_tag")){
        	String tag = "";
        	try {
        		String town = "";
        		String nation = "";
        		if (resident.hasTown()) {
        			if (resident.getTown().hasTag())
        				town = resident.getTown().getTag();
        			if (resident.getTown().hasNation())
        				if (resident.getTown().getNation().hasTag())
        					nation = resident.getTown().getNation().getTag();
        		}
        		if (!nation.isEmpty())
        			tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
        		else if (!town.isEmpty())
        			tag = String.format(TownySettings.getPAPIFormattingTown(), town);

			} catch (NotRegisteredException ignored) {
			}        	
            return tag;
        }
        
        // %townyadvanced_towny_formatted%
        if(identifier.equals("towny_formatted")){
        	String tag = "";
        	try {
        		String town = "";
        		String nation = "";
        		if (resident.hasTown()) {        		
		    		town = resident.getTown().getFormattedName();
		    		if (resident.getTown().hasNation())
		    			nation = resident.getTown().getNation().getFormattedName();
        		}
        		if (!nation.isEmpty())
        			tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
        		else if (!town.isEmpty())
        			tag = String.format(TownySettings.getPAPIFormattingTown(), town);

			} catch (NotRegisteredException ignored) {
			}        	
            return tag;
        }
        
        // %townyadvanced_towny_tag_override%
        if(identifier.equals("towny_tag_override")){
        	String tag = "";
        	try {
        		String town = "";
        		String nation = "";
        		if (resident.hasTown()) {
        			if (resident.getTown().hasTag())
	       				town = resident.getTown().getTag();
	        		else 
	        			town = resident.getTown().getName();
        			if (resident.getTown().hasNation()) {
		        		if (resident.getTown().getNation().hasTag())
		        			nation = resident.getTown().getNation().getTag();
		        		else 
		        			nation = resident.getTown().getNation().getName();
        			}
        		}
        		if (!nation.isEmpty())
        			tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
        		else if (!town.isEmpty())
        			tag = String.format(TownySettings.getPAPIFormattingTown(), town);
			} catch (NotRegisteredException ignored) {
			}        	
            return tag;
        }
        
        // %townyadvanced_title%
        if(identifier.equals("title")){
        	String title = "";
        	if (resident.hasTitle())
				title = resident.getTitle();        	
            return title;
        }

        // %townyadvanced_surname%
        if(identifier.equals("surname")){
        	String title = "";
        	if (resident.hasSurname())
				title = resident.getSurname();        	
            return title;
        }
        
        // %townyadvanced_towny_name_prefix%
        if(identifier.equals("towny_name_prefix")){
        	String title = "";
        	if (resident.isMayor())
				title = TownySettings.getMayorPrefix(resident);
			if (resident.isKing())
				title = TownySettings.getKingPrefix(resident);        	
            return title;
        }
        
        // %townyadvanced_towny_name_postfix%
        if(identifier.equals("towny_name_postfix")){
        	String title = "";
        	if (resident.isMayor())
				title = TownySettings.getMayorPostfix(resident);
			if (resident.isKing())
				title = TownySettings.getKingPostfix(resident);        	
            return title;
        }
        
        // %townyadvanced_towny_prefix%
        if(identifier.equals("towny_prefix")){
        	String title = "";
        	if (resident.hasTitle())
				title = resident.getTitle();
			else {
				if (resident.isMayor())
					title = TownySettings.getMayorPrefix(resident);
				if (resident.isKing())
					title = TownySettings.getKingPrefix(resident);
			}        	
            return title;
        }
        
        // %townyadvanced_towny_postfix%
        if(identifier.equals("towny_postfix")){
        	String title = "";
        	if (resident.hasSurname())
				title = resident.getSurname();
			else {
				if (resident.isMayor())
					title = TownySettings.getMayorPostfix(resident);
				if (resident.isKing())
					title = TownySettings.getKingPostfix(resident);
			}        	
            return title;
        }
        
        // %townyadvanced_towny_colour%
        if(identifier.equals("towny_colour")){
        	String colour = "";
        	if (!resident.hasTown())
        		colour = TownySettings.getPAPIFormattingNomad();
			else {
				colour = TownySettings.getPAPIFormattingResident();
				if (resident.isMayor())
					colour = TownySettings.getPAPIFormattingMayor();
				if (resident.isKing())
					colour = TownySettings.getPAPIFormattingKing();
			}        	
            return colour;
        }

        
        return null;
    }
    
}