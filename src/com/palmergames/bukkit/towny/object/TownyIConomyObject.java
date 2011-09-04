package com.palmergames.bukkit.towny.object;

import org.bukkit.plugin.Plugin;

import com.iConomy.iConomy;
import com.iConomy.system.Account;
import com.iConomy.system.Holdings;
import com.palmergames.bukkit.towny.IConomyException;
import com.palmergames.bukkit.towny.Towny;
public class TownyIConomyObject extends TownyObject {
        private static Towny plugin;

        public static Towny getPlugin() {
                return plugin;
        }

        public static void setPlugin(Towny plugin) {
                TownyIConomyObject.plugin = plugin;
        }
        
        /*Tries to pay from the players main bank account first, if it fails try their holdings */
        public boolean pay(double n) throws IConomyException 
        {
                if(canPayFromHoldings(n))
                {
                        plugin.sendDebugMsg("Can Pay: " + n);
                    getIConomyHolding().subtract(n);
                    return true;
                }
                return false;
        }

        /* When collecting money add it to the Accounts bank */
        public void collect(double n) throws IConomyException 
        {
                getIConomyHolding().add(n);
        }

        /*When one account is paying another account(Taxes/Plot Purchasing)*/
        public boolean pay(double n, TownyIConomyObject collector) throws IConomyException {
                if (pay(n)) {
                        collector.collect(n);
                        return true;
                } else
                        return false;
        }

        public String getIConomyName() {
                // TODO: Make this less hard coded.
                if (this instanceof Nation)
                        return "nation-" + getName();
                else if (this instanceof Town)
                        return "town-" + getName();
                else
                        return getName();
        }

        /*
        public double getBankBalance() throws IConomyException 
        {
                try
                {
                        checkIConomy();
                        return iConomy.getAccount(getIConomyName()).getBankHoldings(0).balance();
                }
                catch(NoClassDefFoundError e)
                {
                        e.printStackTrace();
                        throw new IConomyException("IConomy error getting balance for " + getIConomyName());
                }
        }
        */
        public double getHoldingBalance() throws IConomyException
        {
                try
                {
                        checkIConomy();
                        plugin.sendDebugMsg("IConomy Balance Name: " + getIConomyName());
                        Account account = getIConomyAccount();
                        if(account!=null)
                        return iConomy.getAccount(getIConomyName()).getHoldings().balance();
                        else
                        {
                        plugin.sendDebugMsg("Account is still null!");
                        return 0;
                        }
                }
                catch(NoClassDefFoundError e)
                {
                        e.printStackTrace();
                        throw new IConomyException("IConomy error getting holdings for " + getIConomyName());
                }
        }
        
        /*Gets the Players bank account, if it does not exist it creates one and makes it their main*/
        /*
        public BankAccount getIConomyBankAccount() throws IConomyException
        {
            try
            {
                BankAccount baccount = iConomy.getAccount(getIConomyName()).getMainBankAccount();
                if(baccount!=null)
                {
                        plugin.sendDebugMsg("Got Bank Account: " + baccount.toString());
                    return baccount;
                }
                else
                {
                        plugin.sendDebugMsg("Creating Bank Account");
                    Bank bank = iConomy.getBank(getIConomyName());
                    int count = iConomy.Banks.count(getIConomyName());
                    //plugin.sendDebugMsg("Bank: " + bank.toString());
                    if(bank==null)
                    {
                        plugin.sendDebugMsg("Bank was Null");
                        String test = getIConomyName();
                        plugin.sendDebugMsg(test);
                        bank.createAccount(getIConomyName());
                        plugin.sendDebugMsg("Making Account: " + getIConomyName());
                        if (count == 0)
                        {
                                plugin.sendDebugMsg("Player has 0 Accounts, Making this their main");
                            iConomy.getAccount(getIConomyName()).setMainBank(bank.getId());
                        }
                    }
                    return iConomy.getAccount(getIConomyName()).getMainBankAccount();
                }
            }
                    
            catch(NoClassDefFoundError e)
            {
                    e.printStackTrace();
                    throw new IConomyException("IConomy error getting Bank: " + getIConomyName());
            }
        }
        */
        public Holdings getIConomyHolding() throws IConomyException
        {
                try
                {
                        return iConomy.getAccount(getIConomyName()).getHoldings();
                }
                catch(NoClassDefFoundError e)
                {
                        e.printStackTrace();
                        throw new IConomyException("IConomy error. Incorrect install.");
                }
        }
        
        public Account getIConomyAccount() throws IConomyException 
        {
                try 
                {
                        if(!iConomy.hasAccount(getIConomyName()));
                        {
                                Account account = iConomy.getAccount(getIConomyName());
                                if(account!=null)
                                {
                                        return account;
                                }
                        }
                        return null;
                } 
                catch (NoClassDefFoundError e) 
                {
                        e.printStackTrace();
                        throw new IConomyException("IConomy error. Incorrect install.");
                }
                
        }
        
        public boolean canPayFromHoldings(double n) throws IConomyException 
        {
                if(getHoldingBalance()-n>=0)
                        return true;
                else
                        return false;
        }

        /*
        public boolean canPayFromBank(double n) throws IConomyException 
        {
                if(getIConomyBankAccount().getHoldings().hasEnough(getBankBalance() - n ))
                        return true;
                else
                        return false;
        }
        */
        public static iConomy checkIConomy() throws IConomyException {
                if (plugin == null)
                        throw new IConomyException("IConomyObject has not had plugin configured.");

                Plugin test = plugin.getServer().getPluginManager().getPlugin("iConomy");
                
                try {
                        if (test != null)
                                return (iConomy) test;
                        else
                                throw new IConomyException("IConomy has not been installed.");
                } catch (Exception e) {
                        throw new IConomyException("Incorrect iConomy plugin. Try updating.");
                }
        }
        
        public static String getIConomyCurrency() {
                return "";      
        }
        
        /* Used To Get Balance of Players holdings in String format for printing*/
		public String getHoldingFormattedBalance() {
			try {
				double balance = getHoldingBalance();
				try {
					return iConomy.format(balance);
				} catch (Exception eInvalidAPIFunction) {
					return String.format("%.2f", balance);
				}
			} catch (IConomyException eNoIconomy) {
			        return "Error Accessing Bank Account";
			}
		}
        
        
        /* Used To Get Balance of Players Bank in String format for printing*/
        /*
        @SuppressWarnings("static-access")
        public String getBankFormattedBalance() {
                try {
                        return iConomy.format(getBankBalance());
                } catch (IConomyException e) {
                        return "0 " + getIConomyCurrency();
                }
        }
        */
}
