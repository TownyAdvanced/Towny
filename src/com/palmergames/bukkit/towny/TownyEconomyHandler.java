package com.palmergames.bukkit.towny;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.iConomy.iConomy;
import com.iConomy.system.Account;
import com.nijikokun.register.payment.Methods;
import com.nijikokun.register.payment.Method.MethodAccount;

/**
 * Economy handler to interface with Register, Vault or iConomy 5.01 directly.
 * 
 * @author ElgarL
 * 
 */
public class TownyEconomyHandler {

	private static Towny plugin = null;
	
	private static Economy vaultEconomy = null;

	private static EcoType Type = EcoType.NONE;

	private static String version = "";

	public enum EcoType {
		NONE, ICO5, REGISTER, VAULT
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
		 * Test for native iCo5 support
		 */
		economyProvider = plugin.getServer().getPluginManager().getPlugin("iConomy");

		if (economyProvider != null) {
			/*
			 * Flag as using native iCo5 hooks
			 */
			if (economyProvider.getDescription().getVersion().matches("5.01")) {
				setVersion(String.format("%s v%s", "iConomy", economyProvider.getDescription().getVersion()));
				Type = EcoType.ICO5;
				return true;
			}
		}

		/*
		 * Attempt to hook Register
		 */
		economyProvider = plugin.getServer().getPluginManager().getPlugin("Register");

		if (economyProvider != null) {
			/*
			 * Flag as using Register hooks
			 */
			setVersion(String.format("%s v%s", "Register", economyProvider.getDescription().getVersion()));
			Type = EcoType.REGISTER;
			return true;
		}

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
				setVersion(String.format("%s v%s", "Vault", vaultEcoProvider.getPlugin().getDescription().getVersion()));
				Type = EcoType.VAULT;
				return true;
			}
		} catch (NoClassDefFoundError ex) {
		}

		/*
		 * No compatible Economy system found.
		 */
		return false;
	}

	/**
	 * Returns the relevant players economy account
	 * 
	 * @param player
	 * @return
	 */
	private static Object getEconomyAccount(String accountName) {

		switch (Type) {

		case ICO5:
			return iConomy.getAccount(accountName);

		case REGISTER:
			if (!Methods.getMethod().hasAccount(accountName))
				Methods.getMethod().createAccount(accountName);

			return Methods.getMethod().getAccount(accountName);
			
		default:
			break;

		}

		return null;
	}
	
	/**
	 * Check if account exists
	 * 
	 * @param accountName
	 * @return
	 */
	public static boolean hasEconomyAccount(String accountName) {

		switch (Type) {

		case ICO5:
			return iConomy.hasAccount(accountName);

		case REGISTER:
			return Methods.getMethod().hasAccount(accountName);
			
		case VAULT:
			return vaultEconomy.hasAccount(accountName);
			
		default:
			break;

		}

		return false;
	}

	/**
	 * Attempt to delete the economy account.
	 */
	public static void removeAccount(String accountName) {

		try {
			switch (Type) {

			case ICO5:
				iConomy.getAccount(accountName).remove();
				break;

			case REGISTER:
				MethodAccount account = (MethodAccount) getEconomyAccount(accountName);
				account.remove();
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
	 * @param accountName
	 * @return double containing the total in the account
	 */
	public static double getBalance(String accountName, World world) {

		switch (Type) {

		case ICO5:
			Account icoAccount = (Account) getEconomyAccount(accountName);
			if (icoAccount != null)
				return icoAccount.getHoldings().balance();
			break;

		case REGISTER:
			MethodAccount registerAccount = (MethodAccount) getEconomyAccount(accountName);
			if (registerAccount != null)
				return registerAccount.balance(world);

			break;

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
	 * @param accountName
	 * @param amount
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
	 * @param accountName
	 * @param amount
	 * @return true if successful
	 */
	public static boolean subtract(String accountName, Double amount, World world) {

		switch (Type) {

		case ICO5:
			Account icoAccount = (Account) getEconomyAccount(accountName);
			if (icoAccount != null) {
				icoAccount.getHoldings().subtract(amount);
				return true;
			}
			break;

		case REGISTER:
			MethodAccount registerAccount = (MethodAccount) getEconomyAccount(accountName);
			if (registerAccount != null)
				return registerAccount.subtract(amount, world);
			break;

		case VAULT:
			if (!vaultEconomy.hasAccount(accountName))
				vaultEconomy.createPlayerAccount(accountName);

			return vaultEconomy.withdrawPlayer(accountName, amount).type == EconomyResponse.ResponseType.SUCCESS;
			
		default:
			break;

		}

		return false;
	}

	/**
	 * Add funds to an account.
	 * 
	 * @param accountName
	 * @param amount
	 * @param world
	 * @return true if successful
	 */
	public static boolean add(String accountName, Double amount, World world) {

		switch (Type) {

		case ICO5:
			Account icoAccount = (Account) getEconomyAccount(accountName);
			if (icoAccount != null) {
				icoAccount.getHoldings().add(amount);
				return true;
			}
			break;

		case REGISTER:
			MethodAccount registerAccount = (MethodAccount) getEconomyAccount(accountName);
			if (registerAccount != null)
				return registerAccount.add(amount, world);
			break;

		case VAULT:
			if (!vaultEconomy.hasAccount(accountName))
				vaultEconomy.createPlayerAccount(accountName);

			return vaultEconomy.depositPlayer(accountName, amount).type == EconomyResponse.ResponseType.SUCCESS;
			
		default:
			break;

		}

		return false;
	}

	public static boolean setBalance(String accountName, Double amount, World world) {

		switch (Type) {

		case ICO5:
			Account icoAccount = (Account) getEconomyAccount(accountName);
			if (icoAccount != null) {
				icoAccount.getHoldings().set(amount);
				return true;
			}
			break;

		case REGISTER:
			MethodAccount registerAccount = (MethodAccount) getEconomyAccount(accountName);
			if (registerAccount != null)
				return registerAccount.set(amount, world);
			break;

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
	 * @param balance
	 * @return string containing the formatted balance
	 */
	public static String getFormattedBalance(double balance) {

		try {
			switch (Type) {

			case ICO5:
				return iConomy.format(balance);

			case REGISTER:
				return Methods.getMethod().format(balance);

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
