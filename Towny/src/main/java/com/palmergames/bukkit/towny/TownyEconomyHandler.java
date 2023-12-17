package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.event.economy.TownyPreTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownyTransactionEvent;
import com.palmergames.bukkit.towny.object.economy.adapter.ReserveEconomyAdapter;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.towny.object.TransactionType;
import com.palmergames.bukkit.towny.object.economy.TownyServerAccount;
import com.palmergames.bukkit.towny.object.economy.adapter.EconomyAdapter;
import com.palmergames.bukkit.towny.object.economy.adapter.VaultEconomyAdapter;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;

import net.milkbowl.vault.economy.Economy;
import net.tnemc.core.Reserve;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.Executor;

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
	
	private static final Executor ECONOMY_EXECUTOR = runnable -> {
		if (TownySettings.isEconomyAsync() && plugin.getScheduler().isTickThread())
			plugin.getScheduler().runAsync(runnable);
		else if (!TownySettings.isEconomyAsync() && !plugin.getScheduler().isTickThread())
			plugin.getScheduler().run(runnable);
		else
			runnable.run();
	};
	
	public enum EcoType {
		NONE, VAULT, RESERVE
	}
	
	public static String getServerAccount() {
		return TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT);
	}

	/**
	 * Method which can be used by Economy plugins in order to get a valid UUID from
	 * a Towny object, for use in making FakePlayer accounts.
	 * 
	 * @param accountName String name which Towny uses when interacting with Vault's
	 *                    Economy class.
	 * @return the TownyObject's UUID or null if no Towny Object could be resolved.
	 */
	@Nullable
	public static UUID getTownyObjectUUID(String accountName) {
	
		if (accountName.equalsIgnoreCase(getServerAccount()))
			return TownyServerAccount.getUUID();

		String name;
		if (accountName.startsWith(TownySettings.getNPCPrefix())) {
			name = accountName.substring(TownySettings.getNPCPrefix().length());
			Resident resident = TownyAPI.getInstance().getResident(name);
			return resident != null ? resident.getUUID() : null;
		}

		if (accountName.startsWith(TownySettings.getTownAccountPrefix())) {
			name = accountName.substring(TownySettings.getTownAccountPrefix().length());
			Town town = TownyAPI.getInstance().getTown(name);
			return town != null ? town.getUUID() : null;
		}

		if (accountName.startsWith(TownySettings.getNationAccountPrefix())) {
			name = accountName.substring(TownySettings.getNationAccountPrefix().length());
			Nation nation = TownyAPI.getInstance().getNation(name);
			return nation != null ? nation.getUUID() : null;
		}

		return null;
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
		return (Type != EcoType.NONE && TownySettings.isUsingEconomy());
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

		Plugin economyProvider;

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
		} catch (NoClassDefFoundError ignored) {
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
	public static boolean hasEnough(String accountName, double amount, World world) {
		return getBalance(accountName, world) >= amount;
	}
	
	private static boolean runPreChecks(Transaction transaction, String accountName) {
		TownyPreTransactionEvent preEvent = new TownyPreTransactionEvent(transaction);
		if (BukkitTools.isEventCancelled(preEvent)) {
			TownyMessaging.sendErrorMsg(transaction.getPlayer(), preEvent.getCancelMessage());
			return false;
		}

		checkNewAccount(accountName);
		return true;
	}
	

	/**
	 * Attempts to remove an amount from an account
	 * 
	 * @param accountName name of the account to modify
	 * @param amount amount of currency to remove from the account
	 * @param world name of the world in which to check in (TNE Reserve)   
	 * @return true if successful
	 */
	public static boolean subtract(String accountName, double amount, World world) {

		Player player = Bukkit.getServer().getPlayerExact(accountName);
		Transaction transaction = new Transaction(TransactionType.SUBTRACT, player, amount);
		TownyTransactionEvent event = new TownyTransactionEvent(transaction);
		
		if (!runPreChecks(transaction, accountName)) {
			return false;
		}
		
		if (economy.subtract(accountName, amount, world)) {
			BukkitTools.fireEvent(event);
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
	public static boolean add(String accountName, double amount, World world) {

		Player player = Bukkit.getServer().getPlayerExact(accountName);
		Transaction transaction = new Transaction(TransactionType.ADD, player, amount);
		TownyTransactionEvent event = new TownyTransactionEvent(transaction);

		if (!runPreChecks(transaction, accountName)) {
			return false;
		}

		if (economy.add(accountName, amount, world)) {
			BukkitTools.fireEvent(event);
			return true;
		}

		return false;
	}

	public static boolean setBalance(String accountName, double amount, World world) {
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
			return Colors.translateColorCodes(formattedBalance);
		}

		return Colors.translateColorCodes(String.format("%.2f", balance));

	}

	/**
	 * Adds money to the server account (used for towny closed economy.)
	 * 
	 * @param amount The amount to deposit.
	 * @param world The world of the deposit.
	 * @return A boolean indicating success.
	 */
	public static boolean addToServer(double amount, World world) {
		return add(getServerAccount(), amount, world);
	}

	/**
	 * Removes money to the server account (used for towny closed economy.)
	 *
	 * @param amount The amount to withdraw.
	 * @param world The world of the withdraw.
	 * @return A boolean indicating success.
	 */
	public static boolean subtractFromServer(double amount, World world) {
		return subtract(getServerAccount(), amount, world);
	}
	
	private static void checkNewAccount(String accountName) {
		// Check if the account exists, if not create one.
		if (!economy.hasAccount(accountName)) {
//			if (isEssentials()) {
//				plugin.getLogger().info("Vault told Towny that the " + accountName + " economy account does not exist yet. Requesting a new account.");
//			}
			economy.newAccount(accountName);
		}
	}
	
	public static boolean hasAccount(String accountName) {
		return economy.hasAccount(accountName);
	}

	public static boolean isEssentials() {
		return getVersion().startsWith("EssentialsX Economy") || getVersion().startsWith("Essentials Economy");
	}

	/**
	 * @return An executor that will schedule tasks on the right thread to respect {@link TownySettings#isEconomyAsync()}
	 */
	public static Executor economyExecutor() {
		return ECONOMY_EXECUTOR;
	}
}