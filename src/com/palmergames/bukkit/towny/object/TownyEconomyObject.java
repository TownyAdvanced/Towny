package com.palmergames.bukkit.towny.object;


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
	 * Tries to pay from the players main bank account first, if it fails try their holdings
	 * 
	 * @param n
	 * @return if successfully payed amount to 'server'.
	 * @throws EconomyException
	 */
	public boolean pay(double n, String reason) throws EconomyException {
	    boolean payed = _pay(n);
	    if (payed)
	    	TownyLogger.logMoneyTransaction(this, n, null, reason);
	    return payed;
	}
	
	public boolean pay(double n) throws EconomyException {
	    return pay(n, null);
	}
	
	private boolean _pay(double n) throws EconomyException {
		if(canPayFromHoldings(n)) {
	    	TownyMessaging.sendDebugMsg("Can Pay: " + n);
	        if(plugin.isRegister())
	        	((MethodAccount) getEconomyAccount()).subtract(n);
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
	public void collect(double n, String reason) throws EconomyException {
		_collect(n);
	    TownyLogger.logMoneyTransaction(null, n, this, reason);
	}
	
	public void collect(double n) throws EconomyException {
		collect(n, null);
	}
	
	private void _collect(double n) throws EconomyException {
		if(plugin.isRegister())
    		((MethodAccount) getEconomyAccount()).add(n);
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
	public boolean payTo(double n, TownyEconomyObject collector, String reason) throws EconomyException {
		boolean payed = _payTo(n, collector);
	    if (payed)
	    	TownyLogger.logMoneyTransaction(this, n, collector, reason);
	    return payed;
	}
	
	public boolean payTo(double n, TownyEconomyObject collector) throws EconomyException {
		return payTo(n, collector, null);
	}
	
	private boolean _payTo(double n, TownyEconomyObject collector) throws EconomyException {
		if (_pay(n)) {
			collector._collect(n);
            return true;
		} else {
			return false;
		}
	}
	
	public String getEconomyName() {
		// TODO: Make this less hard coded.
        if (this instanceof Nation)
            return StringMgmt.trimMaxLength(nationAccountPrefix + getName(),32);
        else if (this instanceof Town)
            return StringMgmt.trimMaxLength(townAccountPrefix + getName(),32);
        else
            return getName();
	}

        public void setBalance(double value) 
        {
        	try
            {            	
            	if(plugin.isRegister()) {
            		MethodAccount account = (MethodAccount)getEconomyAccount();
            		if(account!=null) {
                        account.set(value);                        
                    }
                    else
                    {
                    	TownyMessaging.sendDebugMsg("Account is still null!");
                    }
            	} else {
            		Account account = (Account)getEconomyAccount();
            		if(account!=null) {
                        account.getHoldings().set(value);
                    }
                    else
                    {
                    	TownyMessaging.sendDebugMsg("Account is still null!");
                    }
            	}

                
            }
            catch(NoClassDefFoundError e)
            {
                e.printStackTrace();
                TownyMessaging.sendDebugMsg("Economy error getting holdings from " + getEconomyName());
            } catch (EconomyException e) {
            	e.printStackTrace();
            	TownyMessaging.sendDebugMsg("Economy error getting Account for " + getEconomyName());
			}
        }
        
        public double getHoldingBalance() throws EconomyException
        {
                try
                {
                	TownyMessaging.sendDebugMsg("Economy Balance Name: " + getEconomyName());
                	
                	if(plugin.isRegister()) {
                		MethodAccount account = (MethodAccount)getEconomyAccount();
                		if(account!=null) {
                			TownyMessaging.sendDebugMsg("Economy Balance: " + account.balance());
                            return account.balance() ;
                        }
                        else
                        {
                        	TownyMessaging.sendDebugMsg("Account is still null!");
                            return 0;
                        }
                	} else {
                		Account account = (Account)getEconomyAccount();
                		if(account!=null) {
                			TownyMessaging.sendDebugMsg("Economy Balance: " + account.getHoldings().balance());
                            return account.getHoldings().balance();
                        }
                        else
                        {
                        	TownyMessaging.sendDebugMsg("Account is still null!");
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

        public Object getEconomyAccount() throws EconomyException 
        {
        	try 
            {
            	if(plugin.isRegister()) { 
            		
            		if (!Methods.getMethod().hasAccount(getEconomyName()))
            			Methods.getMethod().createAccount(getEconomyName());
            		
            		return Methods.getMethod().getAccount(getEconomyName());
            		
            	} else if(plugin.isIConomy()){
                            return iConomy.getAccount(getEconomyName());
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
        
        public static void checkEconomy() throws EconomyException {
                
                if(plugin.isRegister()) {
                	return;
                } else if(plugin.isIConomy()){
                	return;
                } else
                	throw new EconomyException("No Economy plugins are configured.");
        }
        
        public static String getEconomyCurrency() {
        	if(plugin.isRegister()) {
        		String[] split = Methods.getMethod().format(0).split("0");
        		return split[split.length-1].trim();
        	} else if(plugin.isIConomy()){
        		String[] split = iConomy.format(0).split("0");
        		return split[split.length-1].trim();
        	}
        	return ""; 
        }
        
        /* Used To Get Balance of Players holdings in String format for printing*/
		public String getHoldingFormattedBalance() {
			try {
				double balance = getHoldingBalance();
				try {
					if(plugin.isRegister()) {
						return Methods.getMethod().format(balance);
					} else if(plugin.isIConomy()){
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
				if(plugin.isRegister()) {
					return Methods.getMethod().format(balance);
				} else if(plugin.isIConomy()){
					return iConomy.format(balance);
				}
				
			} catch (Exception eInvalidAPIFunction) {
			}
			return String.format("%.2f", balance);
		}
}
