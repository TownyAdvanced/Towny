package com.palmergames.bukkit.towny;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import com.palmergames.util.FileMgmt;

public class TownyLogger {
	public static final Logger log = Logger.getLogger("com.palmergames.bukkit.towny.log");
	public static final Logger money = Logger.getLogger("com.palmergames.bukkit.towny.moneylog");
	public static final Logger debug = Logger.getLogger("com.palmergames.bukkit.towny.debug");
	
	public static void setup(String root, boolean append) {
		String logFolder = root + FileMgmt.fileSeparator() + "logs";
		FileMgmt.checkFolders(new String[]{logFolder});
		
		setupLogger(log, logFolder, "towny.log", new TownyLogFormatter(), TownySettings.isAppendingToLog());
		
		setupLogger(money, logFolder, "money.csv", new TownyMoneyLogFormatter(), TownySettings.isAppendingToLog());
		money.setUseParentHandlers(false);
		
		//if (TownySettings.getDebug()) {
			setupLogger(debug, logFolder, "debug.log", new TownyLogFormatter(), TownySettings.isAppendingToLog());
			//debug.setUseParentHandlers(false);	//if enabled this prevents the messages from showing in the console.
		//}
	}
	
	public static void shutDown() {
		CloseDownLogger(log);
		CloseDownLogger(money);
		CloseDownLogger(debug);
	}
	
	
	public static void setupLogger(Logger logger, String logFolder, String filename, Formatter formatter, boolean append) {
		try {
            FileHandler fh = new FileHandler(logFolder + FileMgmt.fileSeparator() + filename, append);
            fh.setFormatter(formatter);
            logger.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public static void CloseDownLogger(Logger logger) {
		
		for (Handler fh: logger.getHandlers()) {
			logger.removeHandler(fh);
			fh.close();
		}
		
	}
	
	public static void logMoneyTransaction(TownyEconomyObject a, double amount, TownyEconomyObject b, String reason) {
		money.info(String.format("%s,%s,%s,%s", reason == null ? "" : reason, getObjectName(a), amount, getObjectName(b)));
		//money.info(String.format("   %-48s --[ %16.2f ]--> %-48s", getObjectName(a), amount, getObjectName(b)));
	}
	
	private static String getObjectName(TownyEconomyObject obj) {
		String type;
		if (obj == null)
			type = "Server";
		else if (obj instanceof Resident)
			type = "Resident";
		else if (obj instanceof Town)
			type = "Town";
		else if (obj instanceof Nation)
			type = "Nation";
		else
			type = "Server";
		
		return String.format("[%s] %s", type, obj != null ? obj.getName() : "");
	}
}