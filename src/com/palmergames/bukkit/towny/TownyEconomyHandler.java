package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.event.TownyPreTransactionEvent;
import com.palmergames.bukkit.towny.event.TownyTransactionEvent;
import com.palmergames.bukkit.towny.object.ReserveEconomyAdapter;
import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.towny.object.TransactionType;
import com.palmergames.bukkit.towny.object.economy.EconomyAdapter;
import com.palmergames.bukkit.towny.object.economy.VaultEconomyAdapter;
import com.palmergames.bukkit.util.BukkitTools;
import net.milkbowl.vault.economy.Economy;
import net.tnemc.core.Reserve;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

/**
 * Economy handler to interface with Register or Vault directly.
 * 
 * @author ElgarL
 * @author Suneet Tipirneni (Siris)
 */
public class TownyEconomyHandler {

	private static Towny plugin = null;
	private static EconomyAdapter economy = null;
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
	 * @param version The version of this eco.
	 */
	private static void setVersion(String version) {
		TownyEconomyHandler.version = version;
	}

	/**
	 * Find and configure a suitable economy provider
	 * 
	 * @return true if successful.
	 */
	public static boolean setupEconomy() {

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
				economy = new VaultEconomyAdapter(vaultEcoProvider.getProvider());
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
			economy = new ReserveEconomyAdapter(((Reserve) economyProvider).economy());
			setVersion(String.format("%s %s", ((Reserve) economyProvider).economy().name(), "via Reserve" ));
			Type = EcoType.RESERVE;
			return true;
		}

		/*
		 * No compatible Economy system found.
		 */
		return false;
	}

	
	// This was removed because:
	// 1.) Only we should handle concrete account classes
	// 2.) This was unused anyways.
//	/**
//	 * Returns the relevant player's economy account
//	 * 
//	 * @param accountName - Name of the player's account (usually playername)
//	 * @return - The relevant player's economy account
//	 */
//	@SuppressWarnings("unused")
//	private static Object getEconomyAccount(String accountName) {
//
//		switch (Type) {
//
//		case RESERVE:
//			if(reserveEconomy instanceof ExtendedEconomyAPI)
//				return ((ExtendedEconomyAPI)reserveEconomy).getAccount(accountName);
//			break;
//		
//		default:
//			break;
//
//		}
//
//		return null;
//	}
	
	// We don't even use UUID's right now?
	/**
	 * Check if account exists
	 * 
	 * @param uniqueId the UUID of the account to check
	 * @return true if the account exists
	 */
	public static boolean hasEconomyAccount(UUID uniqueId) {
//		switch (Type) {
//
//		case RESERVE:
//		    return reserveEconomy.hasAccountDetail(uniqueId).success();
//			
//		case VAULT:
//			return vaultEconomy.hasAccount(Bukkit.getOfflinePlayer(uniqueId));
//			
//		default:
//			break;
//
//		}
//
		return false;
	}

	/**
	 * Attempt to delete the economy account.
	 * 
	 * @param accountName name of the account to delete
	 */
	public static void removeAccount(String accountName) {
		economy.deleteAccount(accountName);
	}

	/**
	 * Returns the accounts current balance
	 * 
	 * @param accountName name of the economy account
	 * @param world name of world to check in (for TNE Reserve)   
	 * @return double containing the total in the account
	 */
	public static double getBalance(String accountName, World world) {
		checkNewAccount(accountName);

		return economy.getBalance(accountName, world);
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
		TownyMessaging.sendErrorMsg(getBalance(accountName, world) + "");
		return getBalance(accountName, world) >= amount;
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

		checkNewAccount(accountName);
		
		if (economy.subtract(accountName, amount, world)) {
			BukkitTools.getPluginManager().callEvent(event);
			return true;
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
		
		checkNewAccount(accountName);

		if (economy.add(accountName, amount, world)) {
			BukkitTools.getPluginManager().callEvent(event);
			return true;
		}

		return false;
	}

	public static boolean setBalance(String accountName, Double amount, World world) {
		checkNewAccount(accountName);
		return economy.setBalance(accountName, amount, world);
	}

	/**
	 * Format this balance according to the current economy systems settings.
	 * 
	 * @param balance account balance passed by the economy handler
	 * @return string containing the formatted balance
	 */
	public static String getFormattedBalance(double balance) {

		String formattedBalance = economy.getFormattedBalance(balance);
		if (formattedBalance != null) {
			return formattedBalance;
		}

		return String.format("%.2f", balance);

	}


	private static void checkNewAccount(String accountName) {
		// Check if the account exists, if not create one.
		if (!economy.hasAccount(accountName)) {
			economy.newAccount(accountName);
		}
	}

}