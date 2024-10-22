package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.economy.NationPreTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.NationTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownPreTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownTransactionEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache.CacheType;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.util.BukkitTools;

import net.kyori.adventure.text.Component;

public class MoneyUtil {

	public static double getTownDebtCap(Town town, double upkeep) {
		return TownySettings.isDebtCapAFixedNumberOfDays() ? upkeep * TownySettings.getDebtCapFixedDays() : getEstimatedValueOfTown(town);
	}
	
	/**
	 * Get estimated value of town
	 * Useful when calculating the allowed debt cap for a town
	 * Covers new town costs, claimed land costs, purchased outposts costs.
	 *
	 * @param town The town to estimate a value for.
	 * @return the estimated monetary value of the town.
	 */
	public static double getEstimatedValueOfTown(Town town) {
		return TownySettings.getNewTownPrice() // New Town cost. 
				+ ((town.getTownBlocks().size() - 1) * TownySettings.getClaimPrice()) // Claimed land costs. (-1 because the homeblock comes with the NewTownPrice.) 
				+ (town.getAllOutpostSpawns().size() * (TownySettings.getOutpostCost() - TownySettings.getClaimPrice())); // Outposts costs. 
	}
	
	public static void townWithdraw(Player player, Resident resident, Town town, int amount) {
		
		try {
			commonTests(amount, resident, town, player.getLocation(), false, true);

			Transaction transaction = Transaction.withdraw(amount).paidBy(town).paidTo(resident).build();

			BukkitTools.ifCancelledThenThrow(new TownPreTransactionEvent(town, transaction));
			
			// Withdraw from bank.
			town.withdrawFromBank(resident, amount);

			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_xx_withdrew_xx", resident.getName(), amount, Translatable.of("town_sing")));
			BukkitTools.fireEvent(new TownTransactionEvent(town, transaction));
			
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}

	}
	
	public static void townDeposit(Player player, Resident resident, Town town, Nation nation, int amount) {
		
		try {
			commonTests(amount, resident, town, player.getLocation(), false, false);

			Transaction transaction = Transaction.deposit(amount).paidBy(resident).paidTo(town).build();

			BukkitTools.ifCancelledThenThrow(new TownPreTransactionEvent(town, transaction));
			
			if (nation == null) {
				// Deposit into town from a town resident.
				town.depositToBank(resident, amount);				
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_xx_deposited_xx", resident.getName(), amount, Translatable.of("town_sing")));
			} else {
				// Deposit into town from a nation member.
				resident.getAccount().payTo(amount, town, "Town Deposit from Nation member");
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_xx_deposited_xx", resident.getName(), amount, Translatable.literal(town.getName() + " ").append(Translatable.of("town_sing"))));
			}
			
			BukkitTools.fireEvent(new TownTransactionEvent(town, transaction));
			
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}

	}
	
	public static void nationWithdraw(Player player, Resident resident, Nation nation, int amount) {
		
		try {
			commonTests(amount, resident, nation.getCapital(), player.getLocation(), true, true);

			Transaction transaction = Transaction.withdraw(amount).paidBy(nation).paidTo(resident).build();
			
			BukkitTools.ifCancelledThenThrow(new NationPreTransactionEvent(nation, transaction));

			// Withdraw from bank.
			nation.withdrawFromBank(resident, amount);
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_xx_withdrew_xx", resident.getName(), amount, Translatable.of("nation_sing")));
			BukkitTools.fireEvent(new NationTransactionEvent(nation, transaction));
			
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
		
	}
	
	public static void nationDeposit(Player player, Resident resident, Nation nation, int amount) {

		try {
			commonTests(amount, resident, nation.getCapital(), player.getLocation(), true, false);

			Transaction transaction = Transaction.deposit(amount).paidBy(resident).paidTo(nation).build();
			
			BukkitTools.ifCancelledThenThrow(new NationPreTransactionEvent(nation, transaction));
			
			// Deposit into nation.
			nation.depositToBank(resident, amount);
			
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_xx_deposited_xx", resident.getName(), amount, Translatable.of("nation_sing")));
			BukkitTools.fireEvent(new NationTransactionEvent(nation, transaction));
			
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
	}
	
	/**
	 * Executes tests common to the town and nation deposit/withdraw commands.
	 * 
	 * @param amount int value of the money being transacted.
	 * @param resident Resident making the transaction.
	 * @param town Town involved in the transaction, in the case of a nation transaction it is the capital.
	 * @param loc Location of the player using the command.
	 * @param nation boolean that is True if this is a nation transaction and not a town transaction.
	 * @param withdraw boolean that is True if this is withdraw and not a deposit. 
	 * @throws TownyException thrown if any of the tests are failed.
	 */
	private static void commonTests(int amount, Resident resident, Town town, Location loc, boolean nation, boolean withdraw) throws TownyException {
		Nation townNation = nation ? town.getNationOrNull() : null; 

		if (!TownyEconomyHandler.isActive())
			throw new TownyException(Translatable.of("msg_err_no_economy"));
		
		if (amount < 0)
			throw new TownyException(Translatable.of("msg_err_negative_money"));
		
		if (!withdraw && !resident.getAccount().canPayFromHoldings(amount))
			throw new TownyException(Translatable.of("msg_insuf_funds"));
		
		if (!nation && town.isRuined())
			throw new TownyException(Translatable.of("msg_err_cannot_use_command_because_town_ruined"));
		
		if (withdraw && ((nation && !TownySettings.getNationBankAllowWithdrawls()) || (!nation && !TownySettings.getTownBankAllowWithdrawls())))
			throw new TownyException(Translatable.of("msg_err_withdraw_disabled"));
		
		if (!withdraw && ((!nation && TownySettings.getTownBankCap(town) > 0) || (nation && TownySettings.getNationBankCap(townNation) > 0))) {
			double bankcap = 0;
			double balance = 0;
			if (!nation && town.getBankCap() > 0) {
				bankcap = town.getBankCap();
				balance = town.getAccount().getHoldingBalance();
			} else if (nation && townNation.getBankCap() > 0) {
				bankcap = townNation.getBankCap();
				balance = townNation.getAccount().getHoldingBalance();
			}
			if (bankcap > 0 && amount + balance > bankcap)
				throw new TownyException(Translatable.of("msg_err_deposit_capped", bankcap));
		}
		
		if (TownySettings.isBankActionLimitedToBankPlots())
			testBankPlotRules(town, loc);
		
		if (TownySettings.isBankActionDisallowedOutsideTown() && isNotInOwnTown(town, loc)) {
			if (nation)
				throw new TownyException(Translatable.of("msg_err_unable_to_use_bank_outside_nation_capital"));
			else
				throw new TownyException(Translatable.of("msg_err_unable_to_use_bank_outside_your_town"));
		}
		
		int minAmount = 0;
		if (withdraw)
			minAmount = nation ? TownySettings.getNationMinWithdraw() : TownySettings.getTownMinWithdraw();
		else
			minAmount = nation ? TownySettings.getNationMinDeposit() : TownySettings.getTownMinDeposit();
		if (amount < minAmount)
			throw new TownyException(Translatable.of("msg_err_must_be_greater_than_or_equal_to", formatMoney(minAmount)));
			
	}

	private static void testBankPlotRules(Town town, Location loc) throws TownyException {
		if (isNotInOwnTown(town, loc))
			throw new TownyException(Translatable.of("msg_err_unable_to_command_outside_of_town"));

		TownBlock tb = TownyAPI.getInstance().getTownBlock(loc);
		// TownBlock is a bank, we're good.
		if (tb.getType().equals(TownBlockType.BANK))
			return;

		// The config doesn't allow towns to use their homeblock after they have one or more bank plots.
		if (TownySettings.doHomeblocksNoLongerWorkWhenATownHasBankPlots() && town.getTownBlockTypeCache().getNumTownBlocks(TownBlockType.BANK, CacheType.ALL) > 0)
			throw new TownyException(Translatable.of("msg_err_unable_to_use_bank_outside_bank_plot_no_homeblock"));

		// The config does allow towns to use their homeblocks, or the town has no bank plots.
		if (!tb.isHomeBlock())
			throw new TownyException(Translatable.of("msg_err_unable_to_use_bank_outside_bank_plot"));
	}
	
	private static boolean isNotInOwnTown(Town town, Location loc) {
		return TownyAPI.getInstance().isWilderness(loc) || !town.equals(TownyAPI.getInstance().getTown(loc));
	}
	
	public static double getMoneyAboveZeroOrThrow(String input) throws TownyException {
		double amount;
		try {
			amount = Double.parseDouble(input);
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_num"));
		}
		if (amount < 0)
			throw new TownyException(Translatable.of("msg_err_negative_money"));
		return amount;
	}

	public static double returnPurchasedBlocksCost(int alreadyPurchased, int toPurchase, Town town) {
		int n;
		if (alreadyPurchased + toPurchase > TownySettings.getMaxPurchasedBlocks(town)) {
			n = TownySettings.getMaxPurchasedBlocks(town) - alreadyPurchased;
		} else {
			n = toPurchase;
		}

		if (n == 0)
			return n;

		final double increaseValue = TownySettings.getPurchasedBonusBlocksIncreaseValue();
		final double baseCost = TownySettings.getPurchasedBonusBlocksCost();
		final double maxPrice = TownySettings.getPurchasedBonusBlocksMaxPrice();
		final boolean hasMaxPrice = maxPrice >= 0;

		if (increaseValue == 1) {
			// No exponential increase, short circuit to a simpler calculation
			final double perBlockCost = hasMaxPrice ? Math.min(baseCost, maxPrice) : baseCost;

			return Math.round(perBlockCost * n);
		}

		// Adjust the base cost to the amount of already purchased blocks
		final double blockCost = baseCost * Math.pow(increaseValue, alreadyPurchased);

		if (hasMaxPrice) {
			// We've already hit the max price.
			if (blockCost >= maxPrice) {
				return Math.round(maxPrice * n);
			}
			
			// Check if we're going to reach the max price
			final int increases = (int) Math.ceil((Math.log(maxPrice) - Math.log(blockCost)) / Math.log(increaseValue));
			
			if (increases < n) {
				// The amount of increments needed to reach the max price is less than the amount we're buying
				// Calculate the price of the exponential increase until we reach the max price, then add up the remainder
				final double cost = (blockCost * (1 - Math.pow(increaseValue, increases)) / (1 - increaseValue)) + (n - increases) * maxPrice;
				return Math.round(cost);
			}
		}
		
		// sum = a * (1 - r^n) / (1 - r) where r != 1
		final double cost = blockCost * (1 - Math.pow(increaseValue, n)) / (1 - increaseValue);
		return Math.round(cost);
	}


	/**
	 * Populates the StatusScreen with the various bank and money components.
	 * @param town Town of which to generate a bankstring.
	 * @param translator Translator used in choosing language.
	 * @param screen StatusScreen to add components to.
	 */
	public static void addTownMoneyComponents(Town town, Translator translator, StatusScreen screen) {
		screen.addComponentOf("moneynewline", Component.newline());
		screen.addComponentOf("bankString", TownyFormatter.colourKeyValue(translator.of("status_bank"), town.getAccount().getHoldingFormattedBalance()));
		if (town.isBankrupt()) {
			if (town.getAccount().getDebtCap() == 0)
				town.getAccount().setDebtCap(getTownDebtCap(town, TownySettings.getTownUpkeepCost(town)));

			screen.addComponentOf("bankrupt", translator.of("status_bank_bankrupt") +
					" " + TownyFormatter.colourKeyValue(translator.of("status_debtcap"), "-" + formatMoney(town.getAccount().getDebtCap())));
		}

		if (!TownySettings.isTaxingDaily())
			return;

		if (town.hasUpkeep())
			screen.addComponentOf("upkeep", translator.of("status_splitter") + TownyFormatter.colourKey(translator.of("status_bank_town2")) +
					" " + TownyFormatter.colourKeyImportant(formatMoney(BigDecimal.valueOf(TownySettings.getTownUpkeepCost(town)).setScale(2, RoundingMode.HALF_UP).doubleValue())));

		if (TownySettings.getUpkeepPenalty() > 0 && town.isOverClaimed())
			screen.addComponentOf("upkeepPenalty", translator.of("status_splitter") + TownyFormatter.colourKey(translator.of("status_bank_town_penalty_upkeep")) +
					" " + TownyFormatter.colourKeyImportant(formatMoney(TownySettings.getTownPenaltyUpkeepCost(town))));

		if (town.isNeutral()) {
			double neutralCost = TownySettings.getTownNeutralityCost(town);
			if (neutralCost > 0)
				screen.addComponentOf("neutralityCost", translator.of("status_splitter") + TownyFormatter.colourKey(translator.of("status_neutrality_cost") +
						" " + TownyFormatter.colourKeyImportant(formatMoney(neutralCost))));
		}

		screen.addComponentOf("towntax", translator.of("status_splitter") + TownyFormatter.colourKey(translator.of("status_bank_town3")) +
				" " + TownyFormatter.colourKeyImportant(town.isTaxPercentage() ? town.getTaxes() + "%" : formatMoney(town.getTaxes())));
	}

	/**
	 * Populates the StatusScreen with the various bank and money components.
	 * @param nation Nation of which to generate a bankstring.
	 * @param translator Translator used in choosing language.
	 * @param screen StatusScreen to add components to.
	 */
	public static void addNationMoneyComponentsToScreen(Nation nation, Translator translator, StatusScreen screen) {
		screen.addComponentOf("moneynewline", Component.newline());
		screen.addComponentOf("bankString", TownyFormatter.colourKeyValue(translator.of("status_bank"), nation.getAccount().getHoldingFormattedBalance()));

		if (!TownySettings.isTaxingDaily())
			return;

		if (TownySettings.getNationUpkeepCost(nation) > 0)
			screen.addComponentOf("nationupkeep", translator.of("status_splitter") + TownyFormatter.colourKey(translator.of("status_bank_town2") +
					" " + TownyFormatter.colourKeyImportant(formatMoney(TownySettings.getNationUpkeepCost(nation)))));
		if (nation.isNeutral()) {
			double neutralCost = TownySettings.getNationNeutralityCost(nation);
			if (neutralCost > 0)
				screen.addComponentOf("neutralityCost", translator.of("status_splitter") + TownyFormatter.colourKey(translator.of("status_neutrality_cost") +
						" " + TownyFormatter.colourKeyImportant(formatMoney(neutralCost))));
		}

		screen.addComponentOf("nationtax", translator.of("status_splitter") + TownyFormatter.colourKey(translator.of("status_nation_tax")) +
				" " + TownyFormatter.colourKeyImportant(nation.isTaxPercentage() ? nation.getTaxes() + "%" : formatMoney(nation.getTaxes())));
		screen.addComponentOf("nationConqueredTax", translator.of("status_splitter") + TownyFormatter.colourKey(translator.of("status_nation_conquered_tax")) +
				" " + TownyFormatter.colourKeyImportant(formatMoney(nation.getConqueredTax())));

	}

	private static String formatMoney(double money) {
		return TownyEconomyHandler.getFormattedBalance(money);
	}
}
