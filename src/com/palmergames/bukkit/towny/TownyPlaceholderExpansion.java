package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.object.EconomyAccount;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.util.StringMgmt;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.Objects;
import java.util.Optional;

/**
 * This class will be registered through the register-method in the plugins
 * onEnable-method.
 */
public class TownyPlaceholderExpansion extends PlaceholderExpansion {

	private Towny plugin;

	/**
	 * Since we register the expansion inside our own plugin, we can simply use this
	 * method here to get an instance of our plugin.
	 *
	 * @param plugin The instance of our plugin.
	 */
	public TownyPlaceholderExpansion(Towny plugin) {
		this.plugin = plugin;
	}

	/**
	 * Because this is an internal class, you must override this method to let
	 * PlaceholderAPI know to not unregister your expansion class when
	 * PlaceholderAPI is reloaded
	 *
	 * @return true to persist through reloads
	 */
	@Override
	public boolean persist() {
		return true;
	}

	/**
	 * Because this is a internal class, this check is not needed and we can simply
	 * return {@code true}
	 *
	 * @return Always true since it's an internal class.
	 */
	@Override
	public boolean canRegister() {
		return true;
	}

	/**
	 * The name of the person who created this expansion should go here. <br>
	 * For convienience do we return the author from the plugin.yml
	 * 
	 * @return The name of the author as a String.
	 */
	@Override
	public String getAuthor() {
		return plugin.getDescription().getAuthors().toString();
	}

	/**
	 * The placeholder identifier should go here. <br>
	 * This is what tells PlaceholderAPI to call our onRequest method to obtain a
	 * value if a placeholder starts with our identifier. <br>
	 * This must be unique and can not contain % or _
	 *
	 * @return The identifier in {@code %<identifier>_<value>%} as String.
	 */
	@Override
	public String getIdentifier() {
		return "townyadvanced";
	}

	/**
	 * This is the version of the expansion. <br>
	 * You don't have to use numbers, since it is set as a String.
	 *
	 * For convienience do we return the version from the plugin.yml
	 *
	 * @return The version as a String.
	 */
	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}

	/**
	 * This is the method called when a placeholder with our identifier is found and
	 * needs a value. <br>
	 * We specify the value identifier in this method. <br>
	 * Since version 2.9.1 can you use OfflinePlayers in your requests.
	 *
	 * @param player     A {@link org.bukkit.entity.Player Player}.
	 * @param identifier A String containing the identifier/value.
	 *
	 * @return possibly-null String of the requested identifier.
	 */
	@Override
	public String onPlaceholderRequest(Player player, String identifier) {

		if (player == null) {
			return "";
		}
		Resident resident;
		resident = TownyUniverse.getInstance().getDatabaseHandler().getResident(player.getUniqueId());
		Optional<Resident> optionalResident = Optional.ofNullable(resident);
		Optional<Town> optionalTown = optionalResident.map(Resident::getTown);
		Optional<Nation> optionalNation = optionalTown.map(Town::getNation);
		Optional<EconomyAccount> optionalTownAccount = optionalTown.map(Town::getAccount);
		Optional<EconomyAccount> optionalNationAccount = optionalNation.map(Nation::getAccount);
		
		if (resident == null) {
			return null;
		}
		
		String town;
		String nation;
		String balance;
		String tag = "";
		String title = "";
		String amount = "";
		String name = "";
		String rank = "";
		double cost = 0.0;

		switch (identifier) {
		case "town": // %townyadvanced_town%
            town = String.format(TownySettings.getPAPIFormattingTown(), optionalTown.map(TownyObject::getName).orElse(""));
            return town;
		case "town_formatted": // %townyadvanced_town_formatted%
            town = String.format(TownySettings.getPAPIFormattingTown(), optionalTown.map(Town::getFormattedName).orElse(""));
            return town;
		case "nation": // %townyadvanced_nation%
            nation = String.format(TownySettings.getPAPIFormattingNation(), optionalNation.map(TownyObject::getName).orElse(""));
            return nation;
		case "nation_formatted": // %townyadvanced_nation_formatted%
            nation = String.format(TownySettings.getPAPIFormattingNation(),optionalNation.map(Nation::getFormattedName).orElse(""));
            return nation;
		case "town_balance": // %townyadvanced_town_balance%
            balance = optionalTownAccount.map(EconomyAccount::getHoldingFormattedBalance).orElse("");
            return balance;
		case "nation_balance": // %townyadvanced_nation_balance%
            balance = optionalNationAccount.map(EconomyAccount::getHoldingFormattedBalance).orElse("");
            return balance;
		case "town_tag": // %townyadvanced_town_tag%
            tag = String.format(TownySettings.getPAPIFormattingTown(), optionalTown.map(Town::getTag).orElse(""));
            return tag;
		case "town_tag_override": // %townyadvanced_town_tag_override%
			return String.format(TownySettings.getPAPIFormattingTown(), optionalTown
				.map(Town::getTag)
				.orElse(optionalTown.map(TownyObject::getName)
					.orElse("")));
		case "nation_tag": // %townyadvanced_nation_tag%
            tag = String.format(TownySettings.getPAPIFormattingNation(), optionalNation.map(Nation::getTag).orElse(""));
            return tag;
		case "nation_tag_override": // %townyadvanced_nation_tag_override%
			return String.format(TownySettings.getPAPIFormattingNation(),
				optionalNation.map(Nation::getTag)
				.orElse(optionalNation.map(TownyObject::getName).orElse("")));
		case "towny_tag": // %townyadvanced_towny_tag%
			
			town = optionalTown.map(Town::getTag).orElse("");
			nation = optionalNation.map(Nation::getTag).orElse("");
          
            if (!nation.isEmpty())
                tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
            else if (!town.isEmpty())
                tag = String.format(TownySettings.getPAPIFormattingTown(), town);

            return tag;
		case "towny_formatted": // %townyadvanced_towny_formatted%
			town = optionalTown.map(Town::getFormattedName).orElse("");
			nation = optionalNation.map(Nation::getFormattedName).orElse("");
           
            if (!nation.isEmpty())
                tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
            else if (!town.isEmpty())
                tag = String.format(TownySettings.getPAPIFormattingTown(), town);

            return tag;
		case "towny_tag_formatted": // %townyadvanced_towny_tag_formatted%
			town = optionalTown
				.map(Town::getTag)
				.orElse(optionalTown
					.map(Town::getFormattedName)
					.orElse(""));
			
			nation = optionalNation
				.map(Nation::getTag)
				.orElse(optionalNation
					.map(TownyObject::getName)
					.orElse(""));
			
            if (!nation.isEmpty())
                tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
            else if (!town.isEmpty())
                tag = String.format(TownySettings.getPAPIFormattingTown(), town);
            return tag;
		case "towny_tag_override": // %townyadvanced_towny_tag_override%
			
			town = optionalTown.map(Town::getTag)
				.orElse(optionalTown
				.map(TownyObject::getName)
				.orElse(""));
			
			nation = optionalNation.map(Nation::getTag)
				.orElse(optionalNation
				.map(TownyObject::getName)
				.orElse(""));
			
            if (!nation.isEmpty())
                tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
            else if (!town.isEmpty())
                tag = String.format(TownySettings.getPAPIFormattingTown(), town);
            return tag;
		case "title": // %townyadvanced_title%
			return optionalResident.map(Resident::getTitle).orElse("");
		case "surname": // %townyadvanced_surname%
			return optionalResident.map(Resident::getSurname).orElse("");
		case "towny_name_prefix": // %townyadvanced_towny_name_prefix%
			if (resident.isMayor())
				title = TownySettings.getMayorPrefix(resident);
			if (resident.isKing())
				title = TownySettings.getKingPrefix(resident);
			return title;
		case "towny_name_postfix": // %townyadvanced_towny_name_postfix%
			if (resident.isMayor())
				title = TownySettings.getMayorPostfix(resident);
			if (resident.isKing())
				title = TownySettings.getKingPostfix(resident);
			return title;
		case "towny_prefix": // %townyadvanced_towny_prefix%
			if (resident.hasTitle())
				title = resident.getTitle() + " ";
			else {
				if (resident.isMayor())
					title = TownySettings.getMayorPrefix(resident);
				if (resident.isKing())
					title = TownySettings.getKingPrefix(resident);
			}
			return title;
		case "towny_postfix": // %townyadvanced_towny_postfix%
			if (resident.hasSurname())
				title = " " + resident.getSurname();
			else {
				if (resident.isMayor())
					title = TownySettings.getMayorPostfix(resident);
				if (resident.isKing())
					title = TownySettings.getKingPostfix(resident);
			}
			return title;
		case "towny_colour": // %townyadvanced_towny_colour%
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
		case "town_residents_amount": // %townyadvanced_town_residents_amount%
			if (resident.hasTown()) {
                amount = String.valueOf(resident.getTown().getNumResidents());
            }
			return amount;
		case "town_residents_online": // %townyadvanced_town_residents_online%
			if (resident.hasTown()) {
                amount = String.valueOf(TownyAPI.getInstance().getOnlinePlayers(resident.getTown()).size());
            }
			return amount;
		case "town_townblocks_used": // %townyadvanced_town_townblocks_used%
			if (resident.hasTown()) {
                amount = String.valueOf(resident.getTown().getTownBlocks().size());
            }
			return amount;
		case "town_townblocks_bought": // %townyadvanced_town_townblocks_bought%
			if (resident.hasTown()) {
                amount = String.valueOf(resident.getTown().getPurchasedBlocks());
            }
			return amount;
		case "town_townblocks_bonus": // %townyadvanced_town_townblocks_bonus%
			if (resident.hasTown()) {
                amount = String.valueOf(resident.getTown().getBonusBlocks());
            }
			return amount;
		case "town_townblocks_maximum": // %townyadvanced_town_townblocks_maximum%
			if (resident.hasTown()) {
                amount = String.valueOf(TownySettings.getMaxTownBlocks(resident.getTown()));
            }
			return amount;
		case "town_townblocks_natural_maximum": // %townyadvanced_town_townblocks_natural_maximum%
			if (resident.hasTown()) {
                amount = String.valueOf(TownySettings.getMaxTownBlocks(resident.getTown()) - resident.getTown().getBonusBlocks() - resident.getTown().getPurchasedBlocks());
            }
			return amount;
		case "town_mayor": // %townyadvanced_town_mayor%
			if (resident.hasTown()) {
                name = resident.getTown().getMayor().getName();
            }
			return name;
		case "nation_king": // %townyadvanced_nation_king%
			return optionalNation
				.map(Nation::getKing)
				.map(TownyObject::getName)
				.orElse("");
		case "resident_friends_amount": // %townyadvanced_resident_friends_amount%
			amount = String.valueOf(resident.getFriends().size());
			return amount;
		case "nation_residents_amount": // %townyadvanced_nation_residents_amount%
			return optionalNation
				.map(Nation::getNumResidents)
				.map(Objects::toString)
				.orElse("");
		case "nation_residents_online": // %townyadvanced_nation_residents_online%
			return optionalNation.map(value -> String.valueOf(TownyAPI.getInstance().getOnlinePlayers(value).size())).orElse(amount); 
		case "nation_capital": // %townyadvanced_nation_capital%
			return optionalNation
				.map(Nation::getCapital)
				.map(TownyObject::getName)
				.orElse("");
		case "daily_town_upkeep": // %townyadvanced_daily_town_upkeep%
			if (resident.hasTown()) {
                cost = TownySettings.getTownUpkeepCost(resident.getTown());
            }
			return String.valueOf(cost);
		case "daily_nation_upkeep": // %townyadvanced_daily_nation_upkeep%
			if (resident.hasTown()) {
                if (resident.getTown().hasNation())
                    cost = TownySettings.getNationUpkeepCost(resident.getTown().getNation());
            }
			return String.valueOf(cost);
		case "has_town": // %townyadvanced_has_town%
			return String.valueOf(resident.hasTown());
		case "has_nation": // %townyadvanced_has_nation%
			return String.valueOf(resident.hasNation());
		case "nation_tag_town_formatted": // %townyadvanced_nation_tag_town_formatted%
			
			town = optionalTown
				.map(Town::getFormattedName)
				.orElse("");
			
			nation = optionalNation
				.map(Nation::getTag)
				.orElse("");
			
            if (!nation.isEmpty())
                tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
            else if (!town.isEmpty())
                tag = String.format(TownySettings.getPAPIFormattingTown(), town);
            return tag;
		case "town_ranks": // %townyadvanced_town_ranks%
			if (resident.isMayor())
				rank = TownySettings.getLangString("mayor_sing");
			else if (!resident.getTownRanks().isEmpty())
				rank = StringMgmt.capitalize(StringMgmt.join(resident.getTownRanks(), ", "));
			return rank;
			
		case "nation_ranks": // %townyadvanced_nation_ranks%
			if (resident.isKing())
				rank = TownySettings.getLangString("king_sing");
			else if (!resident.getNationRanks().isEmpty())
				rank = StringMgmt.capitalize(StringMgmt.join(resident.getNationRanks(), ", "));
			return rank;
		default:
			return null;
		}
	}
}