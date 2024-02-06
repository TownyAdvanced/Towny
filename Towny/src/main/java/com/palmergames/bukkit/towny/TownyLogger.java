package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.object.economy.Account;
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
 * @author Articdive
 */
public class TownyLogger {
	private static final String TOWNY_MAIN_LOG = "Towny-Main-Log";
	private static final String TOWNY = "Towny";
	private static final String TOWNY_MONEY = "Towny-Money";
	private static final String TOWNY_DEBUG = "Towny-Debug";
	private static final String TOWNY_DATABASE = "Towny-Database";
	private static final TownyLogger instance = new TownyLogger();
	private static final Logger LOGGER_MONEY = LogManager.getLogger("com.palmergames.bukkit.towny.money");

	private TownyLogger() {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		// Get log location.
		String logFolderName = TownyUniverse.getInstance().getRootFolder() + File.separator + "logs";
		
		Appender townyMainAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "towny.log")
			.setName(TOWNY_MAIN_LOG)
			.withAppend(TownySettings.isAppendingToLog())
			.setIgnoreExceptions(false)
			.setBufferedIo(false)
			.setBufferSize(0)
			.setConfiguration(config)
			.setLayout(PatternLayout.newBuilder()
				.withCharset(StandardCharsets.UTF_8)
				.withPattern("%d [%t]: %m%n")
				.withConfiguration(config)
				.build())
			.build();
		Appender townyMoneyAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "money.csv")
			.setName(TOWNY_MONEY)
			.withAppend(TownySettings.isAppendingToLog())
			.setIgnoreExceptions(false)
			.setBufferedIo(false)
			.setBufferSize(0)
			.setConfiguration(config)
			.setLayout(PatternLayout.newBuilder()
				// The comma after the date is to seperate it in CSV, this is a really nice workaround
				// And avoids having to use apache-csv to make it work with Log4J
				.withCharset(StandardCharsets.UTF_8)
				.withPattern("%d{dd MMM yyyy HH:mm:ss},%m%n")
				.withConfiguration(config)
				.build())
			.build();
		Appender townyDebugAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "debug.log")
			.setName(TOWNY_DEBUG)
			.withAppend(TownySettings.isAppendingToLog())
			.setIgnoreExceptions(false)
			.setBufferedIo(false)
			.setBufferSize(0)
			.setConfiguration(config)
			.setLayout(PatternLayout.newBuilder()
				.withCharset(StandardCharsets.UTF_8)
				.withPattern("%d [%t]: %m%n")
				.withConfiguration(config)
				.build())
			.build();
		Appender townyDatabaseAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "database.log")
			.setName(TOWNY_DATABASE)
			.withAppend(TownySettings.isAppendingToLog())
			.setIgnoreExceptions(false)
			.setBufferedIo(false)
			.setBufferSize(0)
			.setConfiguration(config)
			.setLayout(PatternLayout.newBuilder()
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
		LoggerConfig townyMainConfig = LoggerConfig.newBuilder()
				.withAdditivity(true)
				.withLevel(Level.ALL)
				.withLoggerName(TOWNY)
				.withConfig(config)
				.withRefs(new AppenderRef[0])
				.build();
		townyMainConfig.addAppender(townyMainAppender, Level.ALL, null);
		config.addLogger(Towny.class.getName(), townyMainConfig);
		
		// Debug
		LoggerConfig townyDebugConfig = LoggerConfig.newBuilder()
				.withAdditivity(TownySettings.getDebug())
				.withLevel(Level.ALL)
				.withLoggerName(TOWNY_DEBUG)
				.withRefs(new AppenderRef[0])
				.withConfig(config)
				.build();
		townyDebugConfig.addAppender(townyDebugAppender, Level.ALL, null);
		config.addLogger("com.palmergames.bukkit.towny.debug", townyDebugConfig);
		
		// Money
		LoggerConfig townyMoneyConfig = LoggerConfig.newBuilder()
				.withAdditivity(false)
				.withLevel(Level.ALL)
				.withLoggerName(TOWNY_MONEY)
				.withConfig(config)
				.withRefs(new AppenderRef[0])
				.build();
		townyMoneyConfig.addAppender(townyMoneyAppender, Level.ALL, null);
		config.addLogger("com.palmergames.bukkit.towny.money", townyMoneyConfig);
		
		// Database
		LoggerConfig townyDatabaseConfig = LoggerConfig.newBuilder()
				.withAdditivity(false)
				.withLevel(Level.ALL)
				.withLoggerName(TOWNY_DATABASE)
				.withConfig(config)
				.withRefs(new AppenderRef[0])
				.build();
		townyDatabaseConfig.addAppender(townyDatabaseAppender, Level.ALL, null);
		
		ctx.updateLoggers();
	}
	
	public void refreshDebugLogger() {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig townyDebugConfig = config.getLoggerConfig("com.palmergames.bukkit.towny.debug");
		townyDebugConfig.setAdditive(TownySettings.getDebug());
		ctx.updateLoggers();
	}
	
	public void logMoneyTransaction(Account a, double amount, Account b, String reason) {
		
		String sender;
		String receiver;
		
		if (a == null) {
			sender = "None";
		} else {
			sender = a.getName();
		}
		
		if (b == null) {
			receiver = "None";
		} else {
			receiver = b.getName();
		}
		
		if (reason == null) {
			LOGGER_MONEY.info(String.format("%s,%s,%s,%s", "Unknown Reason", sender, amount, receiver));
		} else {
			LOGGER_MONEY.info(String.format("%s,%s,%s,%s", reason, sender, amount, receiver));
		}
	}
	
	public void logMoneyTransaction(String a, double amount, String b, String reason) {
		if (reason == null) {
			LOGGER_MONEY.info(String.format("%s,%s,%s,%s", "Unknown Reason", a, amount, b));
		} else {
			LOGGER_MONEY.info(String.format("%s,%s,%s,%s", reason, a, amount, b));
		}
	}

	public static TownyLogger getInstance() {
		return instance;
	}
}