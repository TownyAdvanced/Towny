package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.event.economy.TownyPreTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownyTransactionEvent;
import com.palmergames.bukkit.towny.object.EconomyAccount;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.towny.object.TransactionType;
import com.palmergames.bukkit.towny.object.economy.TownyServerAccount;
import com.palmergames.bukkit.towny.object.economy.adapter.EconomyAdapter;
import com.palmergames.bukkit.towny.object.economy.provider.EconomyProvider;
import com.palmergames.bukkit.towny.object.economy.provider.ReserveEconomyProvider;
import com.palmergames.bukkit.towny.object.economy.provider.VaultEconomyProvider;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;

import net.tnemc.core.Reserve;

import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
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
	private static EconomyProvider provider = null;
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
	
	@Deprecated
	public static String getServerAccount() {
		return EconomyAccount.SERVER_ACCOUNT.getName();
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
		return Optional.ofNullable(getTownyObjectAccount(accountName)).map(Account::getUUID).orElse(null);
	}
	
	@Nullable
	public static Account getTownyObjectAccount(String accountName) {
	
		if (accountName.equalsIgnoreCase(EconomyAccount.SERVER_ACCOUNT.getName()))
			return EconomyAccount.SERVER_ACCOUNT;

		String name;
		if (accountName.startsWith(TownySettings.getNPCPrefix())) {
			name = accountName.substring(TownySettings.getNPCPrefix().length());
			Resident resident = TownyAPI.getInstance().getResident(name);
			return resident != null ? resident.getAccount() : null;
		}

		if (accountName.startsWith(TownySettings.getTownAccountPrefix())) {
			name = accountName.substring(TownySettings.getTownAccountPrefix().length());
			Town town = TownyAPI.getInstance().getTown(name);
			return town != null ? town.getAccount() : null;
		}

		if (accountName.startsWith(TownySettings.getNationAccountPrefix())) {
			name = accountName.substring(TownySettings.getNationAccountPrefix().length());
			Nation nation = TownyAPI.getInstance().getNation(name);
			return nation != null ? nation.getAccount() : null;
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
		return provider == null ? EcoType.NONE : provider.economyType();
	}

	/**
	 * Are we using any economy system?
	 * 
	 * @return true if we found one.
	 */
	public static boolean isActive() {
		return (getType() != EcoType.NONE && TownySettings.isUsingEconomy());
	}

	/**
	 * @return The current economy providers version string
	 */
	public static String getVersion() {
		return version;
	}

	/**
	 * Find and configure a suitable economy provider
	 * 
	 * @return true if successful.
	 */
	public static boolean setupEconomy() {

		if (plugin.getServer().getPluginManager().isPluginEnabled("Vault"))
			provider = new VaultEconomyProvider();
		else if (plugin.getServer().getPluginManager().isPluginEnabled("Reserve"))
			provider = new ReserveEconomyProvider((Reserve) plugin.getServer().getPluginManager().getPlugin("Reserve"));
		
		if (provider != null) {
			economy = provider.mainAdapter();

			if (economy != null) {
				version = economy.name() + " via " + provider.name();
				
				if (provider.isLegacy())
					version += " (Legacy)";
				
				return true;
			}
		}

		/*
		 * No compatible Economy system found.
		 */
		return false;
	}

	@Deprecated
	public static void removeAccount(String accountName) {
		final Account account = getTownyObjectAccount(accountName);
		if (account != null)
			removeAccount(account);
	}

	/**
	 * Attempt to delete the economy account.
	 * 
	 * @param account account to delete
	 */
	public static void removeAccount(Account account) {
		economy.deleteAccount(account);
	}

	/**
	 * Returns the accounts current balance
	 * 
	 * @param account The economy account
	 * @return double containing the total in the account
	 */
	public static double getBalance(final @NotNull Account account) {
		checkNewAccount(account);
		return economy.getBalance(account);
	}
	
	@Deprecated
	public static double getBalance(String accountName, World world) {
		final Account account = getTownyObjectAccount(accountName);
		
		return account == null ? 0 : getBalance(account);
	}

	@Deprecated
	public static boolean hasEnough(String accountName, double amount, World world) {
		final Account account = getTownyObjectAccount(accountName);
		
		return account != null && hasEnough(account, amount);
	}

	/**
	 * Returns true if the account has enough money
	 * 
	 * @param account economy account
	 * @param amount minimum amount to check against (Double)
	 * @return true if there is enough in the account
	 */
	public static boolean hasEnough(Account account, double amount) {
		return getBalance(account) >= amount;
	}
	
	private static boolean runPreChecks(Transaction transaction) {
		TownyPreTransactionEvent preEvent = new TownyPreTransactionEvent(transaction);
		if (BukkitTools.isEventCancelled(preEvent)) {
			TownyMessaging.sendErrorMsg(transaction.getPlayer(), preEvent.getCancelMessage());
			return false;
		}

		checkNewAccount(transaction.getAccount());
		return true;
	}
	
	@Deprecated
	public static boolean subtract(String accountName, double amount, World world) {
		final Account account = getTownyObjectAccount(accountName);
		
		return account != null && subtract(account, amount);
	}

	/**
	 * Attempts to remove an amount from an account
	 * 
	 * @param account account to modify
	 * @param amount amount of currency to remove from the account
	 * @return true if successful
	 */
	public static boolean subtract(Account account, double amount) {

		Transaction transaction = new Transaction(account, TransactionType.SUBTRACT, amount);
		TownyTransactionEvent event = new TownyTransactionEvent(transaction);
		
		if (!runPreChecks(transaction)) {
			return false;
		}
		
		if (economy.subtract(account, amount)) {
			BukkitTools.fireEvent(event);
			return true;
		}
		
		return false;
	}

	@Deprecated
	public static boolean add(String accountName, double amount, World world) {
		final Account account = getTownyObjectAccount(accountName);
		
		return account != null && add(account, amount);
	}

	/**
	 * Add funds to an account.
	 * 
	 * @param account account to add funds to
	 * @param amount amount of currency to add
	 * @return true if successful
	 */
	public static boolean add(Account account, double amount) {

		Transaction transaction = new Transaction(account, TransactionType.ADD, amount);
		TownyTransactionEvent event = new TownyTransactionEvent(transaction);

		if (!runPreChecks(transaction)) {
			return false;
		}

		if (economy.add(account, amount)) {
			BukkitTools.fireEvent(event);
			return true;
		}

		return false;
	}

	@Deprecated
	public static boolean setBalance(String accountName, double amount, World world) {
		final Account account = getTownyObjectAccount(accountName);
		
		return account != null && setBalance(account, amount);
	}

	public static boolean setBalance(Account account, double amount) {
		checkNewAccount(account);
		return economy.setBalance(account, amount);
	}

	/**
	 * Format this balance according to the current economy systems settings.
	 * 
	 * @param balance account balance passed by the economy handler
	 * @return string containing the formatted balance
	 */
	public static String getFormattedBalance(double balance) {
		if (!isActive())
			return String.valueOf(balance);

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
		TownyServerAccount.setWorld(TownyAPI.getInstance().getTownyWorld(world));

		try {
			return add(EconomyAccount.SERVER_ACCOUNT, amount);
		} finally {
			TownyServerAccount.setWorld(null);
		}
	}

	/**
	 * Removes money to the server account (used for towny closed economy.)
	 *
	 * @param amount The amount to withdraw.
	 * @param world The world of the withdraw.
	 * @return A boolean indicating success.
	 */
	public static boolean subtractFromServer(double amount, World world) {
		TownyServerAccount.setWorld(TownyAPI.getInstance().getTownyWorld(world));

		try {
			return subtract(EconomyAccount.SERVER_ACCOUNT, amount);
		} finally {
			TownyServerAccount.setWorld(null);
		}
	}
	
	private static void checkNewAccount(Account account) {
		// Check if the account exists, if not create one.
		if (!economy.hasAccount(account)) {
//			if (isEssentials()) {
//				plugin.getLogger().info("Vault told Towny that the " + accountName + " economy account does not exist yet. Requesting a new account.");
//			}
			economy.newAccount(account);
		}
	}
	
	public static boolean hasAccount(Account account) {
		return economy.hasAccount(account);
	}
	
	@Deprecated
	public static boolean hasAccount(String accountName) {
		final Account account = getTownyObjectAccount(accountName);
		
		return account != null && hasAccount(account);
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
	
	@ApiStatus.Internal
	public static EconomyProvider getProvider() {
		return provider;
	}
	
	@ApiStatus.Internal
	@Nullable
	public static EconomyAdapter activeAdapter() {
		return economy;
	}
}