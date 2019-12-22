package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.database.TownyDatabase;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author Lukas Mansour (Articdive)
 */
public class TownyLogger {
	private static final TownyLogger instance = new TownyLogger();
	private static final Logger LOGGER_MONEY = LogManager.getLogger("com.palmergames.bukkit.towny.money");
	
	private TownyLogger() {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		// Get log location.
		String logFolderName = TownyUniverse.getInstance().getRootFolder() + File.separator + "logs";
		
		Appender townyMainAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "towny.log")
			.withName("Towny-Main-Log")
			.withAppend(TownySettings.isAppendingToLog())
			.withIgnoreExceptions(false)
			.withBufferedIo(false)
			.withBufferSize(0)
			.setConfiguration(config)
			.withLayout(PatternLayout.newBuilder()
				.withCharset(StandardCharsets.UTF_8)
				.withPattern("%d [%t]: %m%n")
				.withConfiguration(config)
				.build())
			.build();
		Appender townyMoneyAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "money.csv")
			.withName("Towny-Money")
			.withAppend(TownySettings.isAppendingToLog())
			.withIgnoreExceptions(false)
			.withBufferedIo(false)
			.withBufferSize(0)
			.setConfiguration(config)
			.withLayout(PatternLayout.newBuilder()
				// The comma after the date is to seperate it in CSV, this is a really nice workaround
				// And avoids having to use apache-csv to make it work with Log4J
				.withCharset(StandardCharsets.UTF_8)
				.withPattern("%d{dd MMM yyyy HH:mm:ss},%m%n")
				.withConfiguration(config)
				.build())
			.build();
		Appender townyDebugAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "debug.log")
			.withName("Towny-Debug")
			.withAppend(TownySettings.isAppendingToLog())
			.withIgnoreExceptions(false)
			.withBufferedIo(false)
			.withBufferSize(0)
			.setConfiguration(config)
			.withLayout(PatternLayout.newBuilder()
				.withCharset(StandardCharsets.UTF_8)
				.withPattern("%d [%t]: %m%n")
				.withConfiguration(config)
				.build())
			.build();
		Appender townyDatabaseAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "database.log")
			.withName("Towny-Database")
			.withAppend(TownySettings.isAppendingToLog())
			.withIgnoreExceptions(false)
			.withBufferedIo(false)
			.withBufferSize(0)
			.setConfiguration(config)
			.withLayout(PatternLayout.newBuilder()
				.withCharset(StandardCharsets.UTF_8)
				.withPattern("%d [%t]: %m%n")
				.withConfiguration(config)
				.build())
			.build();
		
		townyMainAppender.start();
		townyMoneyAppender.start();
		townyDebugAppender.start();
		townyDatabaseAppender.start();
		
		// Towny Main
		LoggerConfig townyMainConfig = LoggerConfig.createLogger(true, Level.ALL, "Towny", null, new AppenderRef[0], null, config, null);
		townyMainConfig.addAppender(townyMainAppender, Level.ALL, null);
		config.addLogger(Towny.class.getName(), townyMainConfig);
		
		// Debug
		LoggerConfig townyDebugConfig = LoggerConfig.createLogger(TownySettings.getDebug(), Level.ALL, "Towny-Debug", null, new AppenderRef[0], null, config, null);
		townyDebugConfig.addAppender(townyDebugAppender, Level.ALL, null);
		config.addLogger("com.palmergames.bukkit.towny.debug", townyDebugConfig);
		
		// Money
		LoggerConfig townyMoneyConfig = LoggerConfig.createLogger(false, Level.ALL, "Towny-Money", null, new AppenderRef[0], null, config, null);
		townyMoneyConfig.addAppender(townyMoneyAppender, Level.ALL, null);
		config.addLogger("com.palmergames.bukkit.towny.money", townyMoneyConfig);
		
		// Database
		LoggerConfig townyDatabaseConfig = LoggerConfig.createLogger(false, Level.ALL, "Towny-Database", null, new AppenderRef[0], null, config, null);
		townyDatabaseConfig.addAppender(townyDatabaseAppender, Level.ALL, null);
		config.addLogger(TownyDatabase.class.getName(), townyDatabaseConfig);
		
		ctx.updateLoggers();
	}
	
	public void refreshDebugLogger() {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig townyDebugConfig = config.getLoggerConfig("com.palmergames.bukkit.towny.debug");
		townyDebugConfig.setAdditive(TownySettings.getDebug());
		ctx.updateLoggers();
	}
	
	public void logMoneyTransaction(TownyEconomyObject a, double amount, TownyEconomyObject b, String reason) {
		if (reason == null) {
			LOGGER_MONEY.info(String.format("%s,%s,%s,%s", "Unknown Reason", getObjectName(a), amount, getObjectName(b)));
		} else {
			LOGGER_MONEY.info(String.format("%s,%s,%s,%s", reason, getObjectName(a), amount, getObjectName(b)));
		}
	}
	
	private String getObjectName(TownyEconomyObject obj) {
		String type;
		if (obj == null) {
			type = "Server";
		} else if (obj instanceof Resident) {
			type = "Resident";
		} else if (obj instanceof Town) {
			type = "Town";
		} else if (obj instanceof Nation) {
			type = "Nation";
		} else {
			type = "?";
		}
		return String.format("[%s] %s", type, obj != null ? obj.getName() : "");
	}
	
	public static TownyLogger getInstance() {
		return instance;
	}
}