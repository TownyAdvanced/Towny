package com.palmergames.bukkit.towny.object;


import org.bukkit.World;

import com.iConomy.iConomy;
import com.iConomy.system.Account;
import com.nijikokun.register.payment.Method.MethodAccount;
import com.nijikokun.register.payment.Methods;
import com.palmergames.bukkit.towny.EconomyException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.util.StringMgmt;

public class TownyEconomyObject extends TownyObject {

	private static Towny plugin;
	private static final String townAccountPrefix = "town-";
	private static final String nationAccountPrefix = "nation-";

	public static void setPlugin(Towny plugin) {
		TownyEconomyObject.plugin = plugin;
	}

	/**
	 * Tries to pay from the players main bank account first, if it fails try
	 * their holdings
	 * 
	 * @param n
	 * @return if successfully payed amount to 'server'.
	 * @throws EconomyException
	 */
	public boolean pay(double n, World world, String reason) throws EconomyException {
		boolean payed = _pay(n, world);
		if (payed)
			TownyLogger.logMoneyTransaction(this, n, null, reason);
		return payed;
	}

	public boolean pay(double n, World world) throws EconomyException {
		return pay(n, world, null);
	}

	private boolean _pay(double n, World world) throws EconomyException {
		if (canPayFromHoldings(n, world)) {
			TownyMessaging.sendDebugMsg("Can Pay: " + n);
			if (plugin.isRegister())
				((MethodAccount) getEconomyAccount()).subtract(n,world);
			else
				((Account) getEconomyAccount()).getHoldings().subtract(n);
			return true;
		}
		return false;
	}

	/**
	 * When collecting money add it to the Accounts bank
	 * 
	 * @param n
	 * @throws EconomyException
	 */
	public void collect(double n, World world, String reason) throws EconomyException {
		_collect(n, world);
		TownyLogger.logMoneyTransaction(null, n, this, reason);
	}

	public void collect(double n, World world) throws EconomyException {
		collect(n, world, null);
	}

	private void _collect(double n, World world) throws EconomyException {
		if (plugin.isRegister())
			((MethodAccount) getEconomyAccount()).add(n, world);
		else
			((Account) getEconomyAccount()).getHoldings().add(n);
	}

	/**
	 * When one account is paying another account(Taxes/Plot Purchasing)
	 * 
	 * @param n
	 * @param collector
	 * @return if successfully payed amount to collector.
	 * @throws EconomyException
	 */
	public boolean payTo(double n, TownyEconomyObject collector, World world, String reason) throws EconomyException {
		boolean payed = _payTo(n, collector, world);
		if (payed)
			TownyLogger.logMoneyTransaction(this, n, collector, reason);
		return payed;
	}

	public boolean payTo(double n, TownyEconomyObject collector, World world) throws EconomyException {
		return payTo(n, collector, world, null);
	}

	private boolean _payTo(double n, TownyEconomyObject collector, World world) throws EconomyException {
		if (_pay(n, world)) {
			collector._collect(n, world);
			return true;
		} else {
			return false;
		}
	}

	public String getEconomyName() {
		// TODO: Make this less hard coded.
		if (this instanceof Nation)
			return StringMgmt.trimMaxLength(nationAccountPrefix + getName(), 32);
		else if (this instanceof Town)
			return StringMgmt.trimMaxLength(townAccountPrefix + getName(), 32);
		else
			return getName();
	}

	public void setBalance(double value, World world) {
		try {
			if (plugin.isRegister()) {
				MethodAccount account = (MethodAccount) getEconomyAccount();
				if (account != null) {
					account.set(value, world);
				} else {
					TownyMessaging.sendDebugMsg("Account is still null!");
				}
			} else {
				Account account = (Account) getEconomyAccount();
				if (account != null) {
					account.getHoldings().set(value);
				} else {
					TownyMessaging.sendDebugMsg("Account is still null!");
				}
			}

		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			TownyMessaging.sendDebugMsg("Economy error getting holdings from " + getEconomyName());
		} catch (EconomyException e) {
			e.printStackTrace();
			TownyMessaging.sendDebugMsg("Economy error getting Account for " + getEconomyName());
		}
	}

	public double getHoldingBalance(World world) throws EconomyException {
		try {
			TownyMessaging.sendDebugMsg("Economy Balance Name: " + getEconomyName());

			if (plugin.isRegister()) {
				MethodAccount account = (MethodAccount) getEconomyAccount();
				if (account != null) {
					TownyMessaging.sendDebugMsg("Economy Balance: " + account.balance(world));
					return account.balance(world);
				} else {
					TownyMessaging.sendDebugMsg("Account is still null!");
					return 0;
				}
			} else {
				Account account = (Account) getEconomyAccount();
				if (account != null) {
					TownyMessaging.sendDebugMsg("Economy Balance: " + account.getHoldings().balance());
					return account.getHoldings().balance();
				} else {
					TownyMessaging.sendDebugMsg("Account is still null!");
					return 0;
				}
			}
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			throw new EconomyException("Economy error getting holdings for " + getEconomyName());
		}
	}

	public Object getEconomyAccount() throws EconomyException {
		try {
			if (plugin.isRegister()) {

				if (!Methods.getMethod().hasAccount(getEconomyName()))
					Methods.getMethod().createAccount(getEconomyName());

				return Methods.getMethod().getAccount(getEconomyName());

			} else if (plugin.isIConomy()) {
				return iConomy.getAccount(getEconomyName());
			}
			return null;
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			throw new EconomyException("Economy error. Incorrect install.");
		}
	}

	public void removeAccount() {
		try {
			if (plugin.isRegister()) {
				MethodAccount account = (MethodAccount) getEconomyAccount();
				account.remove();

			} else if (plugin.isIConomy()) {
				iConomy.getAccount(getEconomyName()).remove();
			}
			return;
		} catch (NoClassDefFoundError e) {
		} catch (EconomyException e) {
		}

	}

	public boolean canPayFromHoldings(double n, World world) throws EconomyException {
		if (getHoldingBalance(world) - n >= 0)
			return true;
		else
			return false;
	}

	public static void checkEconomy() throws EconomyException {

		if (plugin.isRegister()) {
			return;
		} else if (plugin.isIConomy()) {
			return;
		} else
			throw new EconomyException("No Economy plugins are configured.");
	}

	public static String getEconomyCurrency() {
		if (plugin.isRegister()) {
			String[] split = Methods.getMethod().format(0).split("0");
			return split[split.length - 1].trim();
		} else if (plugin.isIConomy()) {
			String[] split = iConomy.format(0).split("0");
			return split[split.length - 1].trim();
		}
		return "";
	}

	/* Used To Get Balance of Players holdings in String format for printing*/
	public String getHoldingFormattedBalance(World world) {
		try {
			double balance = getHoldingBalance(world);
			try {
				if (plugin.isRegister()) {
					return Methods.getMethod().format(balance);
				} else if (plugin.isIConomy()) {
					return iConomy.format(balance);
				}

			} catch (Exception eInvalidAPIFunction) {
			}
			return String.format("%.2f", balance);
		} catch (EconomyException eNoIconomy) {
			return "Error Accessing Bank Account";
		}
	}

	public static String getFormattedBalance(double balance) {

		try {
			if (plugin.isRegister()) {
				return Methods.getMethod().format(balance);
			} else if (plugin.isIConomy()) {
				return iConomy.format(balance);
			}

		} catch (Exception eInvalidAPIFunction) {
		}
		return String.format("%.2f", balance);
	}
}
