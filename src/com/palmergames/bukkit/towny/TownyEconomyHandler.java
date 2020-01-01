package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.event.TownyPreTransactionEvent;
import com.palmergames.bukkit.towny.event.TownyTransactionEvent;
import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.towny.object.TransactionType;
import com.palmergames.bukkit.util.BukkitTools;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.tnemc.core.Reserve;
import net.tnemc.core.economy.EconomyAPI;
import net.tnemc.core.economy.ExtendedEconomyAPI;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Economy handler to interface with Register or Vault directly.
 * 
 * @author ElgarL
 * 
 */
@SuppressWarnings("deprecation")
public class TownyEconomyHandler {

	private static Towny plugin = null;
	
	private static Economy vaultEconomy = null;

	private static EconomyAPI reserveEconomy = null;

	private static EcoType Type = EcoType.NONE;

	private static String version = "";

	public enum EcoType {
		NONE, VAULT, RESERVE
	}

	public static void initialize(Towny plugin) {

		TownyEconomyHandler.plugin = plugin;
	}

	/**
	 * @return the economy type we have detected.
	 */
	public static EcoType getType() {

		return Type;
	}

	/**
	 * Are we using any economy system?
	 * 
	 * @return true if we found one.
	 */
	public static boolean isActive() {

		return (Type != EcoType.NONE);
	}

	/**
	 * @return The current economy providers version string
	 */
	public static String getVersion() {

		return version;
	}

	/**
	 * Internal function to set the version string.
	 * 
	 * @param version
	 */
	private static void setVersion(String version) {

		TownyEconomyHandler.version = version;
	}

	/**
	 * Find and configure a suitable economy provider
	 * 
	 * @return true if successful.
	 */
	public static Boolean setupEconomy() {

		Plugin economyProvider = null;

		/*
		 * Attempt to find Vault for Economy handling
		 */
		try {
			RegisteredServiceProvider<Economy> vaultEcoProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
			if (vaultEcoProvider != null) {
				/*
				 * Flag as using Vault hooks
				 */
				vaultEconomy = vaultEcoProvider.getProvider();
				setVersion(String.format("%s %s", vaultEcoProvider.getProvider().getName(), "via Vault" ));
				Type = EcoType.VAULT;
				return true;
			}
		} catch (NoClassDefFoundError ex) {
		}

		/*
		 * Attempt to find Reserve for Economy handling
		 */
		economyProvider = plugin.getServer().getPluginManager().getPlugin("Reserve");
		if(economyProvider != null && ((Reserve)economyProvider).economyProvided()) {
			/*
			 * Flat as using Reserve Hooks.
			 */
			reserveEconomy = ((Reserve) economyProvider).economy();
			setVersion(String.format("%s %s", reserveEconomy.name(), "via Reserve" ));
			Type = EcoType.RESERVE;
			return true;
		}

		/*
		 * No compatible Economy system found.
		 */
		return false;
	}

	/**
	 * Returns the relevant player's economy account
	 * 
	 * @param accountName - Name of the player's account (usually playername)
	 * @return - The relevant player's economy account
	 */
	@SuppressWarnings("unused")
	private static Object getEconomyAccount(String accountName) {

		switch (Type) {

		case RESERVE:
			if(reserveEconomy instanceof ExtendedEconomyAPI)
				return ((ExtendedEconomyAPI)reserveEconomy).getAccount(accountName);
			break;
		
		default:
			break;

		}

		return null;
	}
	
	/**
	 * Check if account exists
	 * 
	 * @param accountName the economy account to check
	 * @return true if the account exists
	 */
	public static boolean hasEconomyAccount(String accountName) {

		switch (Type) {

		case RESERVE:
		    return reserveEconomy.hasAccountDetail(accountName).success();
			
		case VAULT:
			return vaultEconomy.hasAccount(accountName);
			
		default:
			break;

		}

		return false;
	}
	
	/**
	 * Check if account exists
	 * 
	 * @param uniqueId the UUID of the account to check
	 * @return true if the account exists
	 */
	public static boolean hasEconomyAccount(UUID uniqueId) {
		switch (Type) {

		case RESERVE:
		    return reserveEconomy.hasAccountDetail(uniqueId).success();
			
		case VAULT:
			return vaultEconomy.hasAccount(Bukkit.getOfflinePlayer(uniqueId));
			
		default:
			break;

		}

		return false;
	}

	/**
	 * Attempt to delete the economy account.
	 * 
	 * @param accountName name of the account to delete
	 */
	public static void removeAccount(String accountName) {

		try {
			switch (Type) {

			case RESERVE:
				reserveEconomy.deleteAccountDetail(accountName);
				break;
				
			case VAULT: // Attempt to zero the account as Vault provides no delete method.
				if (!vaultEconomy.hasAccount(accountName))
					vaultEconomy.createPlayerAccount(accountName);
				
				vaultEconomy.withdrawPlayer(accountName, (vaultEconomy.getBalance(accountName)));

				return;
				
			default:
				break;

			}


		} catch (NoClassDefFoundError e) {
		}

		return;
	}

	/**
	 * Returns the accounts current balance
	 * 
	 * @param accountName name of the economy account
	 * @param world name of world to check in (for TNE Reserve)   
	 * @return double containing the total in the account
	 */
	public static double getBalance(String accountName, World world) {

		switch (Type) {

		case RESERVE:
			if (!reserveEconomy.hasAccountDetail(accountName).success()) {
				if(!reserveEconomy.createAccountDetail(accountName).success()) return 0.0;
			}

			return reserveEconomy.getHoldings(accountName, world.getName()).doubleValue();

		case VAULT:
			if (!vaultEconomy.hasAccount(accountName))
				vaultEconomy.createPlayerAccount(accountName);

			return vaultEconomy.getBalance(accountName);
			
		default:
			break;

		}

		return 0.0;
	}

	/**
	 * Returns true if the account has enough money
	 * 
	 * @param accountName name of an economy account
	 * @param amount minimum amount to check against (Double)
	 * @param world name of the world to check in (for TNE Reserve)   
	 * @return true if there is enough in the account
	 */
	public static boolean hasEnough(String accountName, Double amount, World world) {

		if (getBalance(accountName, world) >= amount)
			return true;

		return false;
	}

	/**
	 * Attempts to remove an amount from an account
	 * 
	 * @param accountName name of the account to modify
	 * @param amount amount of currency to remove from the account
	 * @param world name of the world in which to check in (TNE Reserve)   
	 * @return true if successful
	 */
	public static boolean subtract(String accountName, Double amount, World world) {

		Player player = Bukkit.getServer().getPlayer(accountName);
		Transaction transaction = new Transaction(TransactionType.SUBTRACT, player, amount.intValue());
		TownyTransactionEvent event = new TownyTransactionEvent(transaction);
		TownyPreTransactionEvent preEvent = new TownyPreTransactionEvent(transaction);

		BukkitTools.getPluginManager().callEvent(preEvent);

		if (preEvent.isCancelled()) {
			TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
			return false;
		}

		switch (Type) {

		case RESERVE:
			if (!reserveEconomy.hasAccountDetail(accountName).success()) {
				if(!reserveEconomy.createAccountDetail(accountName).success()) return false;
			}
			
			BukkitTools.getPluginManager().callEvent(event);
			return reserveEconomy.removeHoldingsDetail(accountName, new BigDecimal(amount), world.getName()).success();

		case VAULT:
			if (!vaultEconomy.hasAccount(accountName))
				vaultEconomy.createPlayerAccount(accountName);

			BukkitTools.getPluginManager().callEvent(event);
			return vaultEconomy.withdrawPlayer(accountName, amount).type == EconomyResponse.ResponseType.SUCCESS;
			
		default:
			break;

		}

		return false;
	}

	/**
	 * Add funds to an account.
	 * 
	 * @param accountName account to add funds to
	 * @param amount amount of currency to add
	 * @param world name of world (for TNE Reserve)
	 * @return true if successful
	 */
	public static boolean add(String accountName, Double amount, World world) {

		Player player = Bukkit.getServer().getPlayer(accountName);
		Transaction transaction = new Transaction(TransactionType.ADD, player, amount.intValue());
		TownyTransactionEvent event = new TownyTransactionEvent(transaction);
		TownyPreTransactionEvent preEvent = new TownyPreTransactionEvent(transaction);

		BukkitTools.getPluginManager().callEvent(preEvent);
		
		if (preEvent.isCancelled()) {
			TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
			return false;
		}

		switch (Type) {

		case RESERVE:
			if (!reserveEconomy.hasAccountDetail(accountName).success()) {
				if(!reserveEconomy.createAccountDetail(accountName).success()) return false;
			}

			BukkitTools.getPluginManager().callEvent(event);
			return reserveEconomy.addHoldingsDetail(accountName, new BigDecimal(amount), world.getName()).success();

		case VAULT:
			if (!vaultEconomy.hasAccount(accountName))
				vaultEconomy.createPlayerAccount(accountName);
			
			Bukkit.getPluginManager().callEvent(event);
			return vaultEconomy.depositPlayer(accountName, amount).type == EconomyResponse.ResponseType.SUCCESS;
			
		default:
			break;

		}

		return false;
	}

	public static boolean setBalance(String accountName, Double amount, World world) {

		switch (Type) {

		case RESERVE:
			if (!reserveEconomy.hasAccountDetail(accountName).success()) {
				if(!reserveEconomy.createAccountDetail(accountName).success()) return false;
			}

			return reserveEconomy.setHoldingsDetail(accountName, new BigDecimal(amount), world.getName()).success();

		case VAULT:
			if (!vaultEconomy.hasAccount(accountName))
				vaultEconomy.createPlayerAccount(accountName);

			return vaultEconomy.depositPlayer(accountName, (amount - vaultEconomy.getBalance(accountName))).type == EconomyResponse.ResponseType.SUCCESS;
			
		default:
			break;

		}

		return false;
	}

	/**
	 * Format this balance according to the current economy systems settings.
	 * 
	 * @param balance account balance passed by the economy handler
	 * @return string containing the formatted balance
	 */
	public static String getFormattedBalance(double balance) {

		try {
			switch (Type) {

			case RESERVE:
				return reserveEconomy.format(new BigDecimal(balance));

			case VAULT:
				return vaultEconomy.format(balance);
				
			default:
				break;

			}

		} catch (Exception InvalidAPIFunction) {
		}

		return String.format("%.2f", balance);

	}



}