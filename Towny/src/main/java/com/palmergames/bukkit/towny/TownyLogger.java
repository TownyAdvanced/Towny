package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.object.economy.Account;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author Articdive
 */
public final class TownyLogger {
	private TownyLogger() {

	}

	private static final Logger LOGGER_MONEY = LogManager.getLogger("com.palmergames.bukkit.towny.money");
	
	private static Appender townyDebugAppender;

	public static void initialize() {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		// Get log location.
		String logFolderName = TownyUniverse.getInstance().getRootFolder() + File.separator + "logs";

		Appender townyMainAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "towny.log")
			.setName("Towny-Main-Log")
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
			.setName("Towny-Money")
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

		townyDebugAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "debug.log")
			.setName("Towny-Debug")
			.withAppend(TownySettings.isAppendingToLog())
			.setIgnoreExceptions(false)
			.setBufferedIo(false)
			.setBufferSize(0)
			.setConfiguration(config)
			.setLayout(PatternLayout.newBuilder()
				.withCharset(StandardCharsets.UTF_8)
				.withPattern("%d [%t] [%C#%M:%L]: %m%n")
				.withConfiguration(config)
				.build())
			.build();

		Appender townyDatabaseAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "database.log")
			.setName("Towny-Database")
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
		townyDebugAppender.start();
		townyMoneyAppender.start();
		townyDatabaseAppender.start();

		// Towny Main
		LoggerConfig townyMainConfig = LoggerConfig.newBuilder().withAdditivity(true).withLevel(Level.ALL).withLoggerName("Towny").withConfig(config).build();
		townyMainConfig.addAppender(townyMainAppender, Level.INFO, null);
		if (TownySettings.getDebug()) {
			townyMainConfig.addAppender(townyDebugAppender, Level.DEBUG, null);
		}
		// Spigot/Paper decided to name the loggers with their plugin's simple name.
		config.addLogger("Towny", townyMainConfig);

		// Money
		LoggerConfig townyMoneyConfig = LoggerConfig.newBuilder().withAdditivity(false).withLevel(Level.ALL).withLoggerName("Towny-Money").withConfig(config).build();
		townyMoneyConfig.addAppender(townyMoneyAppender, Level.ALL, null);
		config.addLogger("com.palmergames.bukkit.towny.money", townyMoneyConfig);

		// Database
		LoggerConfig townyDatabaseConfig = LoggerConfig.newBuilder().withAdditivity(false).withLevel(Level.ALL).withLoggerName("Towny-Database").withConfig(config).build();
		townyDatabaseConfig.addAppender(townyDatabaseAppender, Level.ALL, null);

		ctx.updateLoggers();
	}

	public static void refreshDebugLogger() {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		LoggerConfig loggerConfig = ctx.getConfiguration().getLoggerConfig("Towny");
		Appender console = ctx.getConfiguration().getAppender("TerminalConsole");

		if (TownySettings.getDebug()) {
			// Make sure we don't add more appenders than necessary.
			loggerConfig.removeAppender("Towny-Debug");
			loggerConfig.removeAppender("TerminalConsole");
			loggerConfig.addAppender(townyDebugAppender, Level.DEBUG, null);
			loggerConfig.addAppender(console, Level.DEBUG, null);
		} else {
			loggerConfig.removeAppender("Towny-Debug");
			loggerConfig.removeAppender("TerminalConsole");
		}
		ctx.updateLoggers();
	}

	public static void logMoneyTransaction(Account a, double amount, Account b, String reason) {

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

	public static void logMoneyTransaction(String a, double amount, String b, String reason) {
		if (reason == null) {
			LOGGER_MONEY.info(String.format("%s,%s,%s,%s", "Unknown Reason", a, amount, b));
		} else {
			LOGGER_MONEY.info(String.format("%s,%s,%s,%s", reason, a, amount, b));
		}
	}
}