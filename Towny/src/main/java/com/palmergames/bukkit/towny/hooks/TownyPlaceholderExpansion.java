package com.palmergames.bukkit.towny.hooks;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.comparators.ComparatorCaches;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.utils.CombatUtil;

import com.palmergames.util.Pair;
import com.palmergames.util.TimeMgmt;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.util.StringMgmt;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * This class will be registered through the register-method in the plugins
 * onEnable-method.
 */
public class TownyPlaceholderExpansion extends PlaceholderExpansion implements Relational {

	final DecimalFormat dFormat= new DecimalFormat("#.##");
	final String nomad = TownySettings.getPAPIFormattingNomad() + Translation.of("nomad_sing");
	final String res = TownySettings.getPAPIFormattingResident() + Translation.of("res_sing");
	final String mayor = TownySettings.getPAPIFormattingMayor() + Translation.of("mayor_sing");
	final String king = TownySettings.getPAPIFormattingKing() + Translation.of("king_sing");
	
	private final Towny plugin;

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
	
	@Override
	public String onPlaceholderRequest(Player player, Player player2, String identifier) {
		return ChatColor.translateAlternateColorCodes('&', getRelationalPlaceholder(player, player2, identifier));
	}

	private String getRelationalPlaceholder(Player player, Player player2, String identifier) {
		if (!identifier.equalsIgnoreCase("color"))
			return TownySettings.getPAPIRelationNone();

		Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
		Resident res2 = TownyUniverse.getInstance().getResident(player2.getUniqueId());
		if (res == null || res2 == null)
			return TownySettings.getPAPIRelationNone();
		
		if (!res2.hasTown()) 
			return TownySettings.getPAPIRelationNoTown();
		else if (CombatUtil.isSameTown(res, res2))
			return TownySettings.getPAPIRelationSameTown();
		else if (CombatUtil.isSameNation(res, res2))
			return res2.getTownOrNull().isConquered() || res.getTownOrNull().isConquered() ? TownySettings.getPAPIRelationConqueredTown() : TownySettings.getPAPIRelationSameNation();
		else if (CombatUtil.isAlly(res, res2))
			return TownySettings.getPAPIRelationAlly();
		else if (CombatUtil.isEnemy(res, res2))
			return TownySettings.getPAPIRelationEnemy();
		else
			return TownySettings.getPAPIRelationNone();
	}

	/**
	 * This is the method called when a placeholder with our identifier is found and
	 * needs a value. <br>
	 * We specify the value identifier in this method. <br>
	 * Since version 2.9.1 can you use OfflinePlayers in your requests.
	 *
	 * @param player     A OfflinePlayer.
	 * @param identifier A String containing the identifier/value.
	 *
	 * @return possibly-null String of the requested identifier.
	 */
	@Override
	public String onRequest(OfflinePlayer player, String identifier) {
		return ChatColor.translateAlternateColorCodes('&', getOfflinePlayerPlaceholder(player, identifier));
	}

	private String getOfflinePlayerPlaceholder(OfflinePlayer player, String identifier) {
		if (player == null && !identifier.startsWith("top_")) {
			return "";
		}
		
		if (identifier.startsWith("top_"))
			return getLeaderBoardPlaceholder(identifier);
		
		/*
		 * This is a location-based placeholder request, use the onPlaceholderRequest to fulfill it.
		 * %townyadvanced_player_status% is a special case and should probably be renamed to %townyadvanced_resident_status%.
		 */
		if (player.isOnline() && (identifier.startsWith("player_") && !identifier.equals("player_status")))
			return onPlaceholderRequest((Player) player, identifier);
		
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		
		if (resident == null)
			return "";

		String town = "";
		String nation = "";
		String balance = "";
		String tag = "";
		String title = "";
		String amount = "";
		String name = "";
		String rank = "";
		String hex = "";
		Double cost = 0.0;
		boolean percentage = false;

		switch (identifier) {
		case "town": // %townyadvanced_town%
			if (resident.hasTown())
				town = String.format(TownySettings.getPAPIFormattingTown(), resident.getTownOrNull().getName());
			return StringMgmt.remUnderscore(town);
		case "town_unformatted": // %townyadvanced_town_unformatted%
			if (resident.hasTown())
				town = resident.getTownOrNull().getName();
			return town;
		case "town_formatted": // %townyadvanced_town_formatted%
			if (resident.hasTown())
				town = String.format(TownySettings.getPAPIFormattingTown(), resident.getTownOrNull().getFormattedName());
			return StringMgmt.remUnderscore(town);
		case "town_formatted_with_town_minimessage_colour": // %townyadvanced_town_formatted_with_town_minimessage_colour%
			if (resident.hasTown()) {
				Town residentTown = resident.getTownOrNull();
				String townHexValue = residentTown.getMapColorHexCode();
				if (townHexValue != null)
					town = String.format(TownySettings.getPAPIFormattingTown(), "<#"+townHexValue+">" + residentTown.getFormattedName());
			}
			return StringMgmt.remUnderscore(town);
		case "nation": // %townyadvanced_nation%
			if (resident.hasNation())
				nation = String.format(TownySettings.getPAPIFormattingNation(), resident.getNationOrNull().getName());
			return StringMgmt.remUnderscore(nation);
		case "nation_unformatted": // %townyadvanced_nation_unformatted%
			if (resident.hasNation())
				nation = resident.getNationOrNull().getName();
			return StringMgmt.remUnderscore(nation);
		case "nation_formatted": // %townyadvanced_nation_formatted%
			if (resident.hasNation())
				nation = String.format(TownySettings.getPAPIFormattingNation(), resident.getNationOrNull().getFormattedName());
			return StringMgmt.remUnderscore(nation);
		case "nation_formatted_with_nation_minimessage_colour": // %townyadvanced_nation_formatted_with_nation_minimessage_colour%
			if (resident.hasNation()) {
				Nation residentNation = resident.getNationOrNull();
				String nationHexValue = residentNation.getMapColorHexCode();
				if (nationHexValue != null)
					nation = String.format(TownySettings.getPAPIFormattingNation(), "<#"+nationHexValue+">" + residentNation.getFormattedName());
			}
			return StringMgmt.remUnderscore(nation);
		case "town_balance": // %townyadvanced_town_balance%
			if (resident.hasTown() && TownyEconomyHandler.isActive())
				balance = getMoney(resident.getTownOrNull().getAccount().getCachedBalance());
			return balance;
        case "town_balance_unformatted": // %townyadvanced_town_balance_unformatted%
			if (resident.hasTown() && TownyEconomyHandler.isActive())
				balance = String.valueOf(resident.getTownOrNull().getAccount().getCachedBalance());
            return balance;
		case "nation_balance": // %townyadvanced_nation_balance%
			if (resident.hasNation() && TownyEconomyHandler.isActive())
				balance = getMoney(resident.getTownOrNull().getNationOrNull().getAccount().getCachedBalance());
			return balance;
        case "nation_balance_unformatted": // %townyadvanced_nation_balance_unformatted%
			if (resident.hasNation() && TownyEconomyHandler.isActive())
				balance = String.valueOf(resident.getTownOrNull().getNationOrNull().getAccount().getCachedBalance());
            return balance;
		case "town_tag": // %townyadvanced_town_tag%
			if (resident.hasTown())
				tag = String.format(TownySettings.getPAPIFormattingTown(), resident.getTownOrNull().getTag());
			return tag;
		case "town_tag_override": // %townyadvanced_town_tag_override%
			if (resident.hasTown()) {
				if (resident.getTownOrNull().hasTag())
					tag = String.format(TownySettings.getPAPIFormattingTown(), resident.getTownOrNull().getTag());
				else
					tag = StringMgmt.remUnderscore(String.format(TownySettings.getPAPIFormattingTown(), resident.getTownOrNull().getName()));
			}
			return tag;
		case "town_tag_unformatted": // %townyadvanced_town_tag_unformatted%
			if (resident.hasTown())
				tag = resident.getTownOrNull().getTag();
			return tag;
		case "town_tag_override_unformatted": // %townyadvanced_town_tag_override_unformatted%
			if (resident.hasTown()) {
				if (resident.getTownOrNull().hasTag())
					tag = resident.getTownOrNull().getTag();
				else
					tag = StringMgmt.remUnderscore(resident.getTownOrNull().getName());
			}
			return tag;
		case "nation_tag": // %townyadvanced_nation_tag%
			if (resident.hasNation())
				tag = String.format(TownySettings.getPAPIFormattingNation(), resident.getNationOrNull().getTag());
			return tag;
		case "nation_tag_override": // %townyadvanced_nation_tag_override%
			if (resident.hasNation()) {
				if (resident.getNationOrNull().hasTag())
					tag = String.format(TownySettings.getPAPIFormattingNation(),
							resident.getNationOrNull().getTag());
				else
					tag = StringMgmt.remUnderscore(String.format(TownySettings.getPAPIFormattingNation(),
							resident.getNationOrNull().getName()));
			}
			return tag;
		case "nation_tag_unformatted": // %townyadvanced_nation_tag_unformatted%
			if (resident.hasNation())
				tag = resident.getNationOrNull().getTag();
			return tag;
		case "nation_tag_override_unformatted": // %townyadvanced_nation_tag_override_unformatted%
			if (resident.hasNation()) {
				if (resident.getNationOrNull().hasTag())
					tag = resident.getNationOrNull().getTag();
				else
					tag = StringMgmt.remUnderscore(resident.getNationOrNull().getName());
			}
			return tag;
		case "towny_tag": // %townyadvanced_towny_tag%
			if (resident.hasTown()) {
				if (resident.getTownOrNull().hasTag())
					town = resident.getTownOrNull().getTag();
				if (resident.hasNation() && resident.getNationOrNull().hasTag())
					nation = resident.getNationOrNull().getTag();
			}
			if (!nation.isEmpty())
				tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
			else if (!town.isEmpty())
				tag = String.format(TownySettings.getPAPIFormattingTown(), town);
			return tag;
		case "towny_formatted": // %townyadvanced_towny_formatted%
			if (resident.hasTown()) {
				town = resident.getTownOrNull().getFormattedName();
				if (resident.hasNation())
					nation = resident.getNationOrNull().getFormattedName();
			}
			if (!nation.isEmpty())
				tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
			else if (!town.isEmpty())
				tag = String.format(TownySettings.getPAPIFormattingTown(), town);
			return tag;
		case "towny_tag_formatted": // %townyadvanced_towny_tag_formatted%
			if (resident.hasTown()) {
				if (resident.getTownOrNull().hasTag())
					town = resident.getTownOrNull().getTag();
				else
					town = resident.getTownOrNull().getFormattedName();
				if (resident.hasNation()) {
					if (resident.getNationOrNull().hasTag())
						nation = resident.getNationOrNull().getTag();
					else
						nation = resident.getNationOrNull().getFormattedName();
				}
			}
			if (!nation.isEmpty())
				tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
			else if (!town.isEmpty())
				tag = String.format(TownySettings.getPAPIFormattingTown(), town);
			return tag;
		case "towny_tag_override": // %townyadvanced_towny_tag_override%
			if (resident.hasTown()) {
				if (resident.getTownOrNull().hasTag())
					town = resident.getTownOrNull().getTag();
				else
					town = StringMgmt.remUnderscore(resident.getTownOrNull().getName());
				if (resident.hasNation()) {
					if (resident.getNationOrNull().hasTag())
						nation = resident.getNationOrNull().getTag();
					else
						StringMgmt.remUnderscore(nation = resident.getNationOrNull().getName());
				}
			}
			if (!nation.isEmpty())
				tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
			else if (!town.isEmpty())
				tag = String.format(TownySettings.getPAPIFormattingTown(), town);
			return tag;
		case "towny_tag_override_with_minimessage_colour": // %townyadvanced_towny_tag_override_with_minimessage_colour%
			if (resident.hasTown()) {
				if (resident.getTownOrNull().hasTag())
					town = resident.getTownOrNull().getTag();
				else
					town = StringMgmt.remUnderscore(resident.getTownOrNull().getName());
				String townHexColour = resident.getTownOrNull().getMapColorHexCode();
				if (townHexColour != null)
					town = "<#"+townHexColour+">" + town;

				if (resident.hasNation()) {
					if (resident.getNationOrNull().hasTag())
						nation = resident.getNationOrNull().getTag();
					else
						StringMgmt.remUnderscore(nation = resident.getNationOrNull().getName());
					String nationHexColour = resident.getNationOrNull().getMapColorHexCode();
					if (nationHexColour != null)
						nation = "<#"+nationHexColour+">" + nation;
				}
			}
			if (!nation.isEmpty())
				tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
			else if (!town.isEmpty())
				tag = String.format(TownySettings.getPAPIFormattingTown(), town);
			return tag;
		case "title": // %townyadvanced_title%
			if (resident.hasTitle())
				title = resident.getTitle();
			return title;
		case "surname": // %townyadvanced_surname%
			if (resident.hasSurname())
				title = resident.getSurname();
			return title;
		case "resident_primary_rank": // %townyadvanced_resident_primary_rank%
			return resident.getPrimaryRankPrefix();
		case "resident_primary_rank_spaced": // %townyadvanced_resident_primary_rank_spaced%
			rank = resident.getPrimaryRankPrefix();
			return rank.isEmpty() ? "" : rank + " ";
		case "towny_name_prefix": // %townyadvanced_towny_name_prefix%
			if (resident.isMayor())
				title = TownySettings.getMayorPrefix(resident);
			if (resident.isKing() && !TownySettings.getKingPrefix(resident).isEmpty())
				title = TownySettings.getKingPrefix(resident);
			return title;
		case "towny_name_postfix": // %townyadvanced_towny_name_postfix%
			if (resident.isMayor())
				title = TownySettings.getMayorPostfix(resident);
			if (resident.isKing() && !TownySettings.getKingPostfix(resident).isEmpty())
				title = TownySettings.getKingPostfix(resident);
			return title;
		case "towny_prefix": // %townyadvanced_towny_prefix%
			if (resident.hasTitle())
				title = resident.getTitle() + " ";
			else {
				if (resident.isMayor())
					title = TownySettings.getMayorPrefix(resident);
				if (resident.isKing() && !TownySettings.getKingPrefix(resident).isEmpty())
					title = TownySettings.getKingPrefix(resident);
			}
			return title;
		case "towny_postfix": // %townyadvanced_towny_postfix%
			if (resident.hasSurname())
				title = " " + resident.getSurname();
			else {
				if (resident.isMayor())
					title = TownySettings.getMayorPostfix(resident);
				if (resident.isKing() && !TownySettings.getKingPostfix(resident).isEmpty())
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
				amount = String.valueOf(resident.getTownOrNull().getNumResidents());
			}
			return amount;
		case "town_residents_online": // %townyadvanced_town_residents_online%
			if (resident.hasTown()) {
				amount = String.valueOf(TownyAPI.getInstance().getOnlinePlayers(resident.getTownOrNull()).size());
			}
			return amount;
		case "town_townblocks_used": // %townyadvanced_town_townblocks_used%
			if (resident.hasTown()) {
				amount = String.valueOf(resident.getTownOrNull().getTownBlocks().size());
			}
			return amount;
		case "town_townblocks_bought": // %townyadvanced_town_townblocks_bought%
			if (resident.hasTown()) {
				amount = String.valueOf(resident.getTownOrNull().getPurchasedBlocks());
			}
			return amount;
		case "town_townblocks_bonus": // %townyadvanced_town_townblocks_bonus%
			if (resident.hasTown()) {
				amount = String.valueOf(resident.getTownOrNull().getBonusBlocks());
			}
			return amount;
		case "town_townblocks_maximum": // %townyadvanced_town_townblocks_maximum%
			if (resident.hasTown()) {
				amount = resident.getTownOrNull().getMaxTownBlocksAsAString();
			}
			return amount;
		case "town_townblocks_natural_maximum": // %townyadvanced_town_townblocks_natural_maximum%
			if (resident.hasTown()) {
				Town restown = resident.getTownOrNull();
				amount = restown.hasUnlimitedClaims()
					? restown.getMaxTownBlocksAsAString()
					: String.valueOf(restown.getMaxTownBlocks() - restown.getBonusBlocks() - restown.getPurchasedBlocks());
			}
			return amount;
		case "town_mayor": // %townyadvanced_town_mayor%
			if (resident.hasTown()) {
				name = resident.getTownOrNull().getMayor().getName();
			}
			return name;
		case "nation_king": // %townyadvanced_nation_king%
			if (resident.hasNation()) {
				name = resident.getNationOrNull().getKing().getName();
			}
			return name;
		case "resident_friends_amount": // %townyadvanced_resident_friends_amount%
			amount = String.valueOf(resident.getFriends().size());
			return amount;
		case "nation_residents_amount": // %townyadvanced_nation_residents_amount%
			if (resident.hasNation()) {
				amount = String.valueOf(resident.getNationOrNull().getNumResidents());
			}
			return amount;
		case "nation_residents_online": // %townyadvanced_nation_residents_online%
			if (resident.hasNation()) {
				amount = String.valueOf(TownyAPI.getInstance().getOnlinePlayers(resident.getNationOrNull()).size());
			}
			return amount;
		case "nation_capital": // %townyadvanced_nation_capital%
			if (resident.hasNation()) {
				name = StringMgmt.remUnderscore(resident.getNationOrNull().getCapital().getName());
			}
			return name;
		case "daily_resident_tax": // %townyadvanced_daily_resident_tax%
			return getMoney(resident.getTaxOwing(true));
		case "daily_resident_tax_unformatted": // %townyadvanced_daily_resident_tax_unformatted%
			return String.valueOf(resident.getTaxOwing(true));
		case "daily_town_upkeep": // %townyadvanced_daily_town_upkeep%
			if (resident.hasTown()) {
				cost = TownySettings.getTownUpkeepCost(resident.getTownOrNull());
			}
			return getMoney(cost);
		case "daily_town_upkeep_unformatted": // %townyadvanced_daily_town_upkeep_unformatted%
			if (resident.hasTown()) {
				cost = TownySettings.getTownUpkeepCost(resident.getTownOrNull());
			}
			return String.valueOf(cost);
		case "daily_town_per_plot_upkeep": // %townyadvanced_daily_town_per_plot_upkeep%
			return getMoney(TownySettings.getTownUpkeep());
		case "daily_town_overclaimed_per_plot_upkeep_penalty": // %townyadvanced_daily_town_overclaimed_per_plot_upkeep_penalty%
			return getMoney(TownySettings.getUpkeepPenalty());
		case "daily_town_upkeep_reduction_from_town_level": // %townyadvanced_daily_town_upkeep_reduction_from_town_level%
			cost = resident.hasTown() 
				? resident.getTownOrNull().getTownLevel().upkeepModifier()
				: 1.0;
			return cost == 1.0 ? "0" : String.valueOf(dFormat.format((1.0 - cost) * 100));
		case "daily_town_upkeep_reduction_from_nation_level": // %townyadvanced_daily_town_upkeep_reduction_from_nation_level%
			cost = resident.hasNation() 
				? resident.getNationOrNull().getNationLevel().nationTownUpkeepModifier()
				: 1.0;
			return cost == 1.0 ? "0" : String.valueOf(dFormat.format((1.0 - cost) * 100));
		case "daily_nation_upkeep": // %townyadvanced_daily_nation_upkeep%
			if (resident.hasNation()) {
				cost = TownySettings.getNationUpkeepCost(resident.getNationOrNull());
			}
			return getMoney(cost);
		case "daily_nation_upkeep_unformatted": // %townyadvanced_daily_nation_upkeep_unformatted%
			if (resident.hasNation()) {
				cost = TownySettings.getNationUpkeepCost(resident.getNationOrNull());
			}
			return String.valueOf(cost);
		case "daily_nation_per_town_upkeep": // %townyadvanced_daily_nation_per_town_upkeep%
			return String.valueOf(TownySettings.getNationUpkeep());
		case "daily_nation_upkeep_reduction_from_nation_level": // %townyadvanced_daily_nation_upkeep_reduction_from_nation_level%
			cost = resident.hasNation() 
				? resident.getNationOrNull().getNationLevel().upkeepModifier()
				: 1.0;
			return cost == 1.0 ? "0" : String.valueOf(dFormat.format((1.0 - cost) * 100));
		case "daily_town_tax": // %townyadvanced_daily_town_tax%
			if (resident.hasTown()) {
				cost = resident.getTownOrNull().getTaxes();
				percentage = resident.getTownOrNull().isTaxPercentage();
			}
			return String.valueOf(cost) + (percentage ? "%" : "");
		case "daily_nation_tax": // %townyadvanced_daily_nation_tax%
			if (resident.hasNation()) {
				cost = resident.getNationOrNull().getTaxes();
				percentage = resident.getNationOrNull().isTaxPercentage();
			}
			return String.valueOf(cost) + (percentage ? "%" : "");
		case "town_creation_cost": // %townyadvanced_town_creation_cost%
			return getMoney(TownySettings.getNewTownPrice());
		case "nation_creation_cost": // %townyadvanced_nation_creation_cost%
			return getMoney(TownySettings.getNewNationPrice());
		case "town_merge_cost": // %townyadvanced_town_merge_cost%
			return getMoney(TownySettings.getBaseCostForTownMerge());
		case "town_merge_per_plot_percentage": // %townyadvanced_town_merge_per_plot_percentage%
			return String.valueOf(TownySettings.getPercentageCostPerPlot());
		case "town_reclaim_cost": // %townyadvanced_town_reclaim_cost%
			return getMoney(TownySettings.getEcoPriceReclaimTown());
		case "town_reclaim_max_duration_hours": // %townyadvanced_town_reclaim_max_duration_hours%
			return String.valueOf(TownySettings.getTownRuinsMaxDurationHours());
		case "town_reclaim_min_duration_hours": // %townyadvanced_town_reclaim_max_duration_hours%
			return String.valueOf(TownySettings.getTownRuinsMinDurationHours());
		case "townblock_buy_bonus_price": // %townyadvanced_townblock_buy_bonus_price%
			return getMoney(TownySettings.getPurchasedBonusBlocksCost());
		case "townblock_claim_price": // %townyadvanced_townblock_claim_price%
			return getMoney(TownySettings.getClaimPrice());
		case "townblock_unclaim_price": // %townyadvanced_townblock_unclaim_price%
			return getMoney(TownySettings.getClaimRefundPrice());
		case "outpost_claim_price": // %townyadvanced_outpost_claim_price%
			return getMoney(TownySettings.getOutpostCost());
		case "townblock_next_claim_price": // %townyadvanced_townblock_next_claim_price%
			if (resident.hasTown())
				cost = resident.getTownOrNull().getTownBlockCost();
			else
				cost = TownySettings.getClaimPrice();
			return getMoney(cost);

		case "has_town": // %townyadvanced_has_town%
			return String.valueOf(resident.hasTown());
		case "has_nation": // %townyadvanced_has_nation%
			return String.valueOf(resident.hasNation());
		case "nation_tag_town_formatted": // %townyadvanced_nation_tag_town_formatted%
			if (resident.hasTown()) {
				town = resident.getTownOrNull().getFormattedName();
				if (resident.hasNation() && resident.getNationOrNull().hasTag())
					nation = resident.getNationOrNull().getTag();
			}
			if (!nation.isEmpty())
				tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
			else if (!town.isEmpty())
				tag = String.format(TownySettings.getPAPIFormattingTown(), town);
			return tag;
		case "nation_tag_town_name": // %townyadvanced_nation_tag_town_name%
			if (resident.hasTown()) {
				town = resident.getTownOrNull().getName();
				if (resident.hasNation() && resident.getNationOrNull().hasTag())
					nation = resident.getNationOrNull().getTag();
			}
			if (!nation.isEmpty())
				tag = TownySettings.getPAPIFormattingBoth().replace("%t", town).replace("%n", nation);
			else if (!town.isEmpty())
				tag = String.format(TownySettings.getPAPIFormattingTown(), town);
			return tag;
		case "town_map_color_hex": // %townyadvanced_town_map_color_hex%
			if (resident.hasTown()){
				hex = resident.getTownOrNull().getMapColorHexCode();
				if (!hex.isEmpty())
					hex = "#"+hex;
			}
			return hex;				
		case "nation_map_color_hex": // %townyadvanced_nation_map_color_hex%
			if (resident.hasNation()){
				hex = resident.getNationOrNull().getMapColorHexCode();
				if (!hex.isEmpty())
					hex = "#"+hex;
			}
			return hex;
		case "town_map_color_minimessage_hex": // %townyadvanced_town_map_color_minimessage_hex%
			if (resident.hasTown()){
				hex = resident.getTownOrNull().getMapColorHexCode();
				if (!hex.isEmpty())
					hex = "<#"+hex+">";
			}
			return hex;				
		case "nation_map_color_minimessage_hex": // %townyadvanced_nation_map_color_minimessage_hex%
			if (resident.hasNation()){
				hex = resident.getNationOrNull().getMapColorHexCode();
				if (!hex.isEmpty())
					hex = "<#"+hex+">";
			}
			return hex;	
		case "town_ranks": // %townyadvanced_town_ranks%
			if (resident.isMayor())
				rank = Translation.of("mayor_sing");
			else if (!resident.getTownRanks().isEmpty())
				rank = StringMgmt.capitalize(StringMgmt.join(resident.getTownRanks(), ", "));
			return rank;
			
		case "nation_ranks": // %townyadvanced_nation_ranks%
			if (resident.isKing())
				rank = Translation.of("king_sing");
			else if (!resident.getNationRanks().isEmpty())
				rank = StringMgmt.capitalize(StringMgmt.join(resident.getNationRanks(), ", "));
			return rank;
		case "player_status": // %townyadvanced_player_status%
			if (!resident.hasTown())
				tag = nomad;
			else {
				if (resident.isKing())
					tag = king;
				else if (resident.isMayor())
					tag = mayor;
				else
					tag = res;
			}
			return tag;
		case "town_prefix": // %townyadvanced_town_prefix%
			return resident.hasTown() ? TownySettings.getTownPrefix(resident.getTownOrNull()) : "";
		case "town_postfix": // %townyadvanced_town_postfix%
			return resident.hasTown() ? TownySettings.getTownPostfix(resident.getTownOrNull()) : "";
		case "nation_prefix": // %townyadvanced_nation_prefix%
			return resident.hasNation() ? TownySettings.getNationPrefix(resident.getNationOrNull()) : "";
		case "nation_postfix": // %townyadvanced_nation_postfix%
			return resident.hasNation() ? TownySettings.getNationPostfix(resident.getNationOrNull()) : "";
		case "player_jailed": // %townyadvanced_player_jailed%
			return String.valueOf(resident.isJailed());
		case "is_nation_peaceful": // %townyadvanced_is_nation_peaceful%	
			return resident.hasNation() ? (resident.getNationOrNull().isNeutral() ? Translation.of("status_town_title_peaceful"): "") : "";
		case "is_town_peaceful": // %townyadvanced_is_town_peaceful%	
			return resident.hasTown() ? (resident.getTownOrNull().isNeutral() ? Translation.of("status_town_title_peaceful"): "") : "";
		case "is_town_public": // %townyadvanced_is_town_public%
			return resident.hasTown() ? (resident.getTownOrNull().isPublic() ? Translation.of("status_public") : "") : "";
		case "is_town_open": // %townyadvanced_is_town_open%
			return resident.hasTown() ? (resident.getTownOrNull().isOpen() ? Translation.of("status_title_open") : "") : "";
		case "town_board": // %townyadvanced_town_board%
			return resident.hasTown() ? resident.getTownOrNull().getBoard() : "";
		case "nation_board": // %townyadvanced_nation_board%
			return resident.hasTown() ? (resident.hasNation() ? resident.getNationOrNull().getBoard() : "") : "";
		case "time_until_new_day_formatted": {// %townyadvanced_time_until_new_day_formatted%
			Locale locale = Translation.getLocaleOffline(player);
			return Translatable.of("msg_time_until_a_new_day").append(TimeMgmt.formatCountdownTime(TimeMgmt.townyTime(true), locale)).translate(locale);
		}
		case "time_until_new_day_hours_formatted": // %townyadvanced_time_until_new_day_hours_formatted%
			return TimeMgmt.formatCountdownTimeHours(TimeMgmt.townyTime(true), player.getPlayer()); 
		case "time_until_new_day_minutes_formatted": // %townyadvanced_time_until_new_day_minutes_formatted%
			return TimeMgmt.formatCountdownTimeMinutes(TimeMgmt.townyTime(true), player.getPlayer());
		case "time_until_new_day_seconds_formatted": // %townyadvanced_time_until_new_day_seconds_formatted%
			return TimeMgmt.formatCountdownTimeSeconds(TimeMgmt.townyTime(true), player.getPlayer());
		case "time_until_new_day_hours_raw": // %townyadvanced_time_until_new_day_hours_raw%
			return TimeMgmt.countdownTimeHoursRaw(TimeMgmt.townyTime(true)); 
		case "time_until_new_day_minutes_raw": // %townyadvanced_time_until_new_day_minutes_raw%
			return TimeMgmt.countdownTimeMinutesRaw(TimeMgmt.townyTime(true));
		case "time_until_new_day_seconds_raw": // %townyadvanced_time_until_new_day_seconds_raw%
			return TimeMgmt.countdownTimeSecondsRaw(TimeMgmt.townyTime(true));
		case "number_of_towns_in_server": // %townyadvanced_number_of_towns_in_server%
			return String.valueOf(TownyUniverse.getInstance().getTowns().size());
		case "number_of_neutral_towns_in_server": // %townyadvanced_number_of_neutral_towns_in_server%
			return String.valueOf(TownyUniverse.getInstance().getTowns().stream().filter(Town::isNeutral).count());
		case "nation_or_town_name":	// %townyadvanced_nation_or_town_name%
			return !resident.hasTown() 
				? ""
				: resident.hasNation()
					? String.format(TownySettings.getPAPIFormattingNation(), StringMgmt.remUnderscore(resident.getNationOrNull().getName()))
					: String.format(TownySettings.getPAPIFormattingTown(), StringMgmt.remUnderscore(resident.getTownOrNull().getName()));

		case "resident_join_date_unformatted": // %townyadvanced_resident_join_date_unformatted%
			return String.valueOf(resident.getRegistered());
		case "resident_join_date_formatted":// %townyadvanced_resident_join_date_formatted%
			return TownyFormatter.getFormattedResidentRegistration(resident);

		default:
			return "";
		}
	}

	/*
	 * Method which strips off the beginning and end of the identifier in order to
	 * get the desired leaderboard type and the required array index number.
	 */
	private String getLeaderBoardPlaceholder(String identifier) {
		identifier = identifier.replace("top_", "");
		int underscore = identifier.lastIndexOf("_");
		int num;
		try {
			num = Math.max(0, Integer.parseInt(identifier.substring(underscore + 1)) - 1);
		} catch (Exception e) {
			return "";
		}
		identifier = identifier.substring(0, underscore);
		return getLeaderBoardPlaceholder(identifier, num);
	}

	private String getLeaderBoardPlaceholder(String identifier, int num) {
		Town town = getTownForLeaderBoardPlaceholder(identifier, num);
		if (town == null) {
			return "";
		}

		String value = switch(identifier) {
		case "town_balance" -> getMoney(town.getAccount().getCachedBalance()); // %townyadvanced_top_town_balance_n%
		case "town_residents" -> String.valueOf(town.getNumResidents());       // %townyadvanced_top_town_residents_n%
		case "town_land" -> String.valueOf(town.getNumTownBlocks());           // %townyadvanced_top_town_land_n%
		default -> "";
		};

		return String.format(TownySettings.getPAPILeaderboardFormat(), StringMgmt.remUnderscore(town.getName()), value);
	}

	@Nullable
	private Town getTownForLeaderBoardPlaceholder(String identifier, int num) {
		ComparatorType type = switch (identifier) {
		case "town_balance" -> ComparatorType.BALANCE;     // %townyadvanced_top_town_balance_n%
		case "town_residents" -> ComparatorType.RESIDENTS; // %townyadvanced_top_town_residents_n%
		case "town_land" -> ComparatorType.TOWNBLOCKS;     // %townyadvanced_top_town_land_n%
		default -> null;
		};
		if (type == null) {
			return null;
		}

		final List<Pair<UUID, Component>> cache = ComparatorCaches.getTownListCache(type);
		return cache.size() <= num ? null : TownyAPI.getInstance().getTown(cache.get(num).key());
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier) {
		return ChatColor.translateAlternateColorCodes('&', getPlayerPlaceholder(player, identifier));
	}

	private String getPlayerPlaceholder(Player player, String identifier) {
		if (player == null) {
			return "";
		}
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());

		if (resident == null)
			return null;

		TownBlock townblock = TownyAPI.getInstance().getTownBlock(player);

		switch (identifier) {

			case "player_plot_type": // %townyadvanced_player_plot_type%
				return townblock != null ? StringMgmt.capitalize(townblock.getType().toString()) : "";
			case "player_plot_owner": // %townyadvanced_player_plot_owner%
				return townblock != null ? String.valueOf(townblock.isOwner(resident)) : "false";
            case "player_plot_is_trusted": // %townyadvanced_player_plot_is_trusted%
                return townblock != null ? String.valueOf(townblock.hasTrustedResident(resident)) : "";
			case "player_location_town_or_wildname": // %townyadvanced_player_location_town_or_wildname%
				return townblock != null ? townblock.getTownOrNull().getName() : TownyAPI.getInstance().getTownyWorld(player.getWorld()).getFormattedUnclaimedZoneName();
			case "player_location_formattedtown_or_wildname": // %townyadvanced_player_location_formattedtown_or_wildname%
				return townblock != null ? townblock.getTownOrNull().getFormattedName() : TownyAPI.getInstance().getTownyWorld(player.getWorld()).getFormattedUnclaimedZoneName();
			case "player_location_plot_name": // %townyadvanced_player_location_plot_name%
				return townblock != null ? (!townblock.getName().isEmpty() ? townblock.getName() : (townblock.hasPlotObjectGroup() ? townblock.getPlotObjectGroup().getName() : "")) : "";
			case "player_location_plot_forsale": { // %townyadvanced_player_location_plot_forsale%
				if (townblock == null) return "";
				return townblock.isForSale() ? Translation.of("towny_map_forsale") : "";
			}
			case "player_location_plotgroup_name": // %townyadvanced_player_location_plotgroup_name%
				return townblock != null ? (townblock.hasPlotObjectGroup() ? townblock.getPlotObjectGroup().getName() : "") : "";
			case "player_location_plot_owner_name": // %townyadvanced_player_location_plot_owner_name%
				return (townblock != null && townblock.hasResident()) ? townblock.getResidentOrNull().getName() : ""; 
			case "player_location_town_prefix": // %townyadvanced_player_location_town_prefix%
				return townblock != null ? townblock.getTownOrNull().getPrefix(): "";
			case "player_location_town_postfix": // %townyadvanced_player_location_town_postfix%
				return townblock != null ? townblock.getTownOrNull().getPostfix(): "";
			case "player_location_pvp": // %townyadvanced_player_location_pvp%
				return townblock != null ? (townblock.getPermissions().pvp ? Translation.of("status_title_pvp"): Translation.of("status_title_nopvp")) : (TownyAPI.getInstance().getTownyWorld(player.getWorld()).isPVP() ? Translation.of("status_title_pvp"):"");
			case "player_location_town_resident_count": // %townyadvanced_player_location_town_resident_count%
				return townblock != null ? Integer.toString(townblock.getTownOrNull().getResidents().size()) : "";
			case "player_location_town_mayor_name": // %townyadvanced_player_location_town_mayor_name%
				return townblock != null ? townblock.getTownOrNull().getMayor().getName() : "";
			case "player_location_town_nation_name": // %townyadvanced_player_location_town_nation_name%
				return townblock != null ? (townblock.getTownOrNull().hasNation() ? townblock.getTownOrNull().getNationOrNull().getFormattedName() : "") : "";
			case "player_location_town_board": // %townyadvanced_player_location_town_board%
				return townblock != null ? townblock.getTownOrNull().getBoard() : "";
			case "player_location_nation_board": // %townyadvanced_player_location_nation_board%
				return townblock != null ? (townblock.getTownOrNull().hasNation() ? townblock.getTownOrNull().getNationOrNull().getBoard() : "") : "";
            case "player_town_is_trusted": // %townyadvanced_player_town_is_trusted%
                return townblock != null ? String.valueOf(townblock.getTownOrNull().hasTrustedResident(resident)) : "";
			case "number_of_towns_in_world": // %townyadvanced_number_of_towns_in_world%
				return String.valueOf(TownyUniverse.getInstance().getTowns().stream()
						.filter(t -> t.getHomeblockWorld().equals(townblock.getWorld()))
						.count());
			case "number_of_neutral_towns_in_world": // %townyadvanced_number_of_neutral_towns_in_world%
				return String.valueOf(TownyUniverse.getInstance().getTowns().stream()
						.filter(t -> t.isNeutral())
						.filter(t -> t.getHomeblockWorld().equals(townblock.getWorld()))
						.count());
			case "player_location_town_forsale_cost": // %townyadvanced_player_location_town_forsale_cost%
				return townblock == null ? "" : 
					townblock.getTownOrNull().isForSale()
						? getMoney(townblock.getTownOrNull().getForSalePrice())
						: Translation.of("msg_not_for_sale");
			default:
				return null;
		}
	}

	private String getMoney(double cost) {
		return TownyEconomyHandler.getFormattedBalance(cost);
	}
}