package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.object.Translatable;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
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
import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.towny.object.TransactionType;
import com.palmergames.bukkit.util.BukkitTools;

public class MoneyUtil {

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
			
			Transaction transaction = new Transaction(TransactionType.WITHDRAW, player, amount);
			
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

			Transaction transaction = new Transaction(TransactionType.DEPOSIT, player, amount);
			
			BukkitTools.ifCancelledThenThrow(new TownPreTransactionEvent(town, transaction));
			
			if (nation == null) {
				// Deposit into town from a town resident.
				town.depositToBank(resident, amount);				
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_xx_deposited_xx", resident.getName(), amount, Translatable.of("town_sing")));
			} else {
				// Deposit into town from a nation member.
				resident.getAccount().payTo(amount, town, "Town Deposit from Nation member");
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_xx_deposited_xx", resident.getName(), amount, town + " " + Translatable.of("town_sing")));
			}
			
			BukkitTools.fireEvent(new TownTransactionEvent(town, transaction));
			
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}

	}
	
	public static void nationWithdraw(Player player, Resident resident, Nation nation, int amount) {
		
		try {
			commonTests(amount, resident, nation.getCapital(), player.getLocation(), true, true);

			Transaction transaction = new Transaction(TransactionType.WITHDRAW, player, amount);
			
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

			Transaction transaction = new Transaction(TransactionType.DEPOSIT, player, amount);
			
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
		
		if (TownySettings.isBankActionLimitedToBankPlots() && isNotInBankPlot(town, loc))
			throw new TownyException(Translatable.of("msg_err_unable_to_use_bank_outside_bank_plot"));
		
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
			throw new TownyException(Translatable.of("msg_err_must_be_greater_than_or_equal_to", TownyEconomyHandler.getFormattedBalance(minAmount)));
			
	}

	private static boolean isNotInBankPlot(Town town, Location loc) {
		if (isNotInOwnTown(town, loc))
			return true;
		
		TownBlock tb = TownyAPI.getInstance().getTownBlock(loc);
		if (!tb.getType().equals(TownBlockType.BANK) && !tb.isHomeBlock())
			return true;

		return false;
	}
	
	private static boolean isNotInOwnTown(Town town, Location loc) {
		return TownyAPI.getInstance().isWilderness(loc) || !town.equals(TownyAPI.getInstance().getTown(loc));
	}
	
	/**
	 * For a short time Towny stored debt accounts in the server's economy plugin.
	 * This practice had to end, being replaced with the debtBalance which is stored
	 * in the Town object.
	 */
	public static void checkLegacyDebtAccounts() {
		File f = new File(TownyUniverse.getInstance().getRootFolder(), "debtAccountsConverted.txt");
		if (!f.exists())
			Bukkit.getScheduler().runTaskLaterAsynchronously(Towny.getPlugin(), () -> convertLegacyDebtAccounts(), 600l);
	}
	
	/**
	 * Will attempt to set a town's debtBalance if their old DebtAccount is above 0 and exists.
	 */
	private static void convertLegacyDebtAccounts() {
		for (Town town : TownyUniverse.getInstance().getTowns()) {
			final String name = "[DEBT]-" + town.getName();
			if (TownyEconomyHandler.hasAccount(name)) {
				if (!TownySettings.isEconomyAsync()) {
					town.setDebtBalance(TownyEconomyHandler.getBalance(name, town.getAccount().getBukkitWorld()));
					TownyEconomyHandler.setBalance(name, 0.0, town.getAccount().getBukkitWorld());
				} else {
					Bukkit.getScheduler().runTaskAsynchronously(Towny.getPlugin(), () -> {
						town.setDebtBalance(TownyEconomyHandler.getBalance(name, town.getAccount().getBukkitWorld()));
						TownyEconomyHandler.setBalance(name, 0.0, town.getAccount().getBukkitWorld());
					});
				}
			}
		}
		Towny.getPlugin().saveResource("debtAccountsConverted.txt", false);
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
}
