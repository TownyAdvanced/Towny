package com.palmergames.bukkit.towny.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.NationPreTransactionEvent;
import com.palmergames.bukkit.towny.event.NationTransactionEvent;
import com.palmergames.bukkit.towny.event.TownPreTransactionEvent;
import com.palmergames.bukkit.towny.event.TownTransactionEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.towny.object.TransactionType;
import com.palmergames.bukkit.towny.object.Translation;
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
			
			TownPreTransactionEvent preEvent = new TownPreTransactionEvent(town, transaction);
			BukkitTools.getPluginManager().callEvent(preEvent);
			if (preEvent.isCancelled())
				throw new TownyException(preEvent.getCancelMessage());
			
			// Withdraw from bank.
			town.withdrawFromBank(resident, amount);

			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_xx_withdrew_xx", resident.getName(), amount, Translation.of("town_sing")));
			BukkitTools.getPluginManager().callEvent(new TownTransactionEvent(town, transaction));
			
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}

	}
	
	public static void townDeposit(Player player, Resident resident, Town town, Nation nation, int amount) {
		
		try {
			commonTests(amount, resident, town, player.getLocation(), false, false);

			Transaction transaction = new Transaction(TransactionType.DEPOSIT, player, amount);
			
			TownPreTransactionEvent preEvent = new TownPreTransactionEvent(town, transaction);
			BukkitTools.getPluginManager().callEvent(preEvent);
			if (preEvent.isCancelled())
				throw new TownyException(preEvent.getCancelMessage());
			
			if (nation == null) {
				// Deposit into town from a town resident.
				town.depositToBank(resident, amount);				
				TownyMessaging.sendPrefixedTownMessage(town, Translation.of("msg_xx_deposited_xx", resident.getName(), amount, Translation.of("town_sing")));
			} else {
				// Deposit into town from a nation member.
				resident.getAccount().payTo(amount, town, "Town Deposit from Nation member");
				TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_xx_deposited_xx", resident.getName(), amount, town + " " + Translation.of("town_sing")));
			}
			
			BukkitTools.getPluginManager().callEvent(new TownTransactionEvent(town, transaction));
			
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}

	}
	
	public static void nationWithdraw(Player player, Resident resident, Nation nation, int amount) {
		
		try {
			commonTests(amount, resident, nation.getCapital(), player.getLocation(), true, true);

			Transaction transaction = new Transaction(TransactionType.WITHDRAW, player, amount);
			
			NationPreTransactionEvent preEvent = new NationPreTransactionEvent(nation, transaction);
			BukkitTools.getPluginManager().callEvent(preEvent);
			
			if (preEvent.isCancelled())
				throw new TownyException(preEvent.getCancelMessage());

			// Withdraw from bank.
			nation.withdrawFromBank(resident, amount);
			TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_xx_withdrew_xx", resident.getName(), amount, Translation.of("nation_sing")));
			BukkitTools.getPluginManager().callEvent(new NationTransactionEvent(nation, transaction));
			
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
		
	}
	
	public static void nationDeposit(Player player, Resident resident, Nation nation, int amount) {

		try {
			commonTests(amount, resident, nation.getCapital(), player.getLocation(), true, false);

			Transaction transaction = new Transaction(TransactionType.DEPOSIT, player, amount);
			
			NationPreTransactionEvent preEvent = new NationPreTransactionEvent(nation, transaction);
			BukkitTools.getPluginManager().callEvent(preEvent);
			if (preEvent.isCancelled())
				throw new TownyException(preEvent.getCancelMessage());
			
			// Deposit into town.
			nation.depositToBank(resident, amount);
			
			TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_xx_deposited_xx", resident.getName(), amount, Translation.of("nation_sing")));
			BukkitTools.getPluginManager().callEvent(new NationTransactionEvent(nation, transaction));
			
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
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
		
		if (!TownyEconomyHandler.isActive())
			throw new TownyException(Translation.of("msg_err_no_economy"));
		
		if (amount < 0)
			throw new TownyException(Translation.of("msg_err_negative_money"));
		
		if (!withdraw && !resident.getAccount().canPayFromHoldings(amount))
			throw new TownyException(Translation.of("msg_insuf_funds"));
		
		if (!nation && town.isRuined())
			throw new TownyException(Translation.of("msg_err_cannot_use_command_because_town_ruined"));
		
		if (withdraw && ((nation && !TownySettings.getNationBankAllowWithdrawls()) || (!nation && !TownySettings.getTownBankAllowWithdrawls())))
			throw new TownyException(Translation.of("msg_err_withdraw_disabled"));
		
		if (!withdraw && (TownySettings.getTownBankCap() > 0 || TownySettings.getNationBankCap() > 0)) {
			double bankcap = 0;
			double balance = 0;
			if (!nation && TownySettings.getTownBankCap() > 0) {
				bankcap = TownySettings.getTownBankCap();
				balance = town.getAccount().getHoldingBalance();
			} else if (nation && TownySettings.getNationBankCap() > 0) {
				bankcap = TownySettings.getNationBankCap();
				balance = town.getNation().getAccount().getHoldingBalance();
			}
			if (bankcap > 0 && amount + balance > bankcap)
				throw new TownyException(Translation.of("msg_err_deposit_capped", bankcap));
		}
		
		if (TownySettings.isBankActionLimitedToBankPlots() && isNotInBankPlot(town, loc))
			throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_bank_plot"));
		
		if (TownySettings.isBankActionDisallowedOutsideTown() && isNotInOwnTown(town, loc)) {
			if (nation)
				throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_nation_capital"));
			else
				throw new TownyException(Translation.of("msg_err_unable_to_use_bank_outside_your_town"));
		}
			
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
	 * Will attempt to set a town's debtBalance if their old DebtAccount is above 0 and exists.
	 */
	public static void convertLegacyDebtAccounts() {
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
}
