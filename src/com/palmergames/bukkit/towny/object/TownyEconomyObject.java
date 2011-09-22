package com.palmergames.bukkit.towny.object;


import com.iConomy.iConomy;
import com.iConomy.system.Account;
import com.palmergames.bukkit.towny.EconomyException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.nijikokun.register.payment.Method.MethodAccount;
import com.nijikokun.register.payment.Methods;

public class TownyEconomyObject extends TownyObject {
	
	private static Towny plugin;

    public static void setPlugin(Towny plugin) {
    	TownyEconomyObject.plugin = plugin;
    }
        
    /*Tries to pay from the players main bank account first, if it fails try their holdings */
    public boolean pay(double n) throws EconomyException 
    {
            if(canPayFromHoldings(n))
            {
                plugin.sendDebugMsg("Can Pay: " + n);
                if(TownySettings.isUsingRegister())
                	((MethodAccount) getEconomyAccount()).subtract(n);
                else
                	((Account) getEconomyAccount()).getHoldings().subtract(n);
                return true;
            }
            return false;
     }

        /* When collecting money add it to the Accounts bank */
        public void collect(double n) throws EconomyException 
        {
        	if(TownySettings.isUsingRegister())
        		((MethodAccount) getEconomyAccount()).add(n);
        	else
        		((Account) getEconomyAccount()).getHoldings().add(n);
        }

        /*When one account is paying another account(Taxes/Plot Purchasing)*/
        public boolean pay(double n, TownyEconomyObject collector) throws EconomyException {
                if (pay(n)) {
                        collector.collect(n);
                        return true;
                } else
                        return false;
        }

        public String getEconomyName() {
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
        public double getHoldingBalance() throws EconomyException
        {
                try
                {
                	plugin.sendDebugMsg("Economy Balance Name: " + getEconomyName());
                	
                	if(TownySettings.isUsingRegister()) {
                		MethodAccount account = (MethodAccount)getEconomyAccount();
                		if(account!=null) {
                        	plugin.sendDebugMsg("Economy Balance: " + account.balance());
                            return account.balance() ;
                        }
                        else
                        {
                            plugin.sendDebugMsg("Account is still null!");
                            return 0;
                        }
                	} else {
                		Account account = (Account)getEconomyAccount();
                		if(account!=null) {
                        	plugin.sendDebugMsg("Economy Balance: " + account.getHoldings().balance());
                            return account.getHoldings().balance();
                        }
                        else
                        {
                            plugin.sendDebugMsg("Account is still null!");
                            return 0;
                        }
                	}

                    
                }
                catch(NoClassDefFoundError e)
                {
                        e.printStackTrace();
                        throw new EconomyException("Economy error getting holdings for " + getEconomyName());
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
        
        public Holdings getEconomyHolding() throws EconomyException
        {
        	try
                {
                	return iConomy.getAccount(getEconomyName()).getHoldings();
                }
                catch(NoClassDefFoundError e)
                {
                    e.printStackTrace();
                    throw new EconomyException("Economy error. Incorrect install.");
                }
        }
        */

        public Object getEconomyAccount() throws EconomyException 
        {
                try 
                {
                	if(TownySettings.isUsingRegister()) {
                		if(Methods.getMethod().hasAccount(getEconomyName()))
                		{
                			plugin.sendDebugMsg("Economy Has account: true");
                			
                			MethodAccount account = Methods.getMethod().getAccount(getEconomyName());
                			if(account!=null)
                            {
                                return account;
                            }
                			
                		}
                	} else if(TownySettings.isUsingIConomy()){
                        if(iConomy.hasAccount(getEconomyName()));
                        {
                            Account account = iConomy.getAccount(getEconomyName());
                            if(account!=null)
                            {
                                return account;
                            }
                        }
                	}
                    return null;
                } 
                catch (NoClassDefFoundError e) 
                {
                        e.printStackTrace();
                        throw new EconomyException("Economy error. Incorrect install.");
                }
                
        }
        
        public boolean canPayFromHoldings(double n) throws EconomyException 
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
        
        public static void checkIConomy() throws EconomyException {
                
                if(TownySettings.isUsingRegister()) {
                	return;
                } else if(TownySettings.isUsingIConomy()){
                	return;
                } else
                	throw new EconomyException("No Economy plugins are configured.");

                /*
                Plugin test = plugin.getServer().getPluginManager().getPlugin("iConomy");
                
                try {
                        if (test != null)
                                return (iConomy) test;
                        else
                                throw new EconomyException("IConomy has not been installed.");
                } catch (Exception e) {
                        throw new EconomyException("Incorrect iConomy plugin. Try updating.");
                }
                */
        }
        
        public static String getEconomyCurrency() {
                return "";      
        }
        
        /* Used To Get Balance of Players holdings in String format for printing*/
		public String getHoldingFormattedBalance() {
			try {
				double balance = getHoldingBalance();
				try {
					if(TownySettings.isUsingRegister()) {
						return Methods.getMethod().format(balance);
					} else if(TownySettings.isUsingIConomy()){
						return iConomy.format(balance);
					}
					
				} catch (Exception eInvalidAPIFunction) {
				}
				return String.format("%.2f", balance);
			} catch (EconomyException eNoIconomy) {
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
