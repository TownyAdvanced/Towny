package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TownyLogger {
	private static final TownyLogger instance = new TownyLogger();
	private static final Logger LOGGER_MONEY = LogManager.getLogger("com.palmergames.bukkit.towny.money");
	
	private TownyLogger() {
	
	}
	
	public void setupLogger() {
		// Hook into Log4J2
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		final boolean append = TownySettings.isAppendingToLog();
		
		String logFolderName = TownyUniverse.getInstance().getRootFolder() + File.separator + "logs";
		// Get the standard layout for the new appenders
		Layout<String> standardLayout = PatternLayout.newBuilder()
			.withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
			.withAlwaysWriteExceptions(false)
			.withNoConsoleNoAnsi(false)
			.withConfiguration(config)
			.build();
		
		// Create a new list of appenders and their references that have to be added
		List<Appender> appenders = new ArrayList<>();
		// Towny main logger
		appenders.add(
			FileAppender.newBuilder()
				.withFileName(logFolderName + File.separator + "towny.log")
				.withLocking(false)
				.withName("Towny")
				.withIgnoreExceptions(false)
				.withBufferedIo(false)
				.withBufferSize(0)
				.withLayout(standardLayout)
				.withAdvertise(false)
				.setConfiguration(config)
				.withAppend(append)
				.build()
		);
		// Towny money logger
		appenders.add(
			FileAppender.newBuilder()
				.withFileName(logFolderName + File.separator + "money.csv")
				.withLocking(false)
				.withName("Towny-Money")
				.withIgnoreExceptions(false)
				.withBufferedIo(false)
				.withBufferSize(0)
				.withLayout(PatternLayout.newBuilder()
					// The comma after the date is to seperate it in CSV, this is a really nice workaround
					// And avoids having to use apache-csv to make it work with Log4J
					.withPattern("%d{dd MMM yyyy HH:mm:ss},%m%n")
					.withNoConsoleNoAnsi(false)
					.withAlwaysWriteExceptions(false)
					.withConfiguration(config)
					.build())
				.withAdvertise(false)
				.setConfiguration(config)
				.withAppend(append)
				.build()
		);
		// Towny debug logger
		appenders.add(
			FileAppender.newBuilder()
				.withFileName(logFolderName + File.separator + "debug.log")
				.withLocking(false)
				.withName("Towny-Debug")
				.withIgnoreExceptions(false)
				.withBufferedIo(false)
				.withBufferSize(0)
				.withLayout(standardLayout)
				.withAdvertise(false)
				.setConfiguration(config)
				.withAppend(append)
				.build()
		);
		appenders.add(
			ConsoleAppender.newBuilder()
				.withName("Towny-Console")
				.withLayout(PatternLayout.newBuilder()
					.withPattern(PatternLayout.DEFAULT_CONVERSION_PATTERN)
					.withAlwaysWriteExceptions(false)
					.withNoConsoleNoAnsi(false)
					.withConfiguration(config)
					.build())
				.withBufferedIo(false)
				.withBufferSize(0).build()
		);
		// store a list of appender references.
		List<AppenderRef[]> appenderRefs = new ArrayList<>();
		// Start appenders, give them the LoggerConfig and add their references to the referenceList
		for (Appender appender : appenders) {
			appender.start();
			config.addAppender(appender);
			appenderRefs.add(new AppenderRef[]{AppenderRef.createAppenderRef(appender.getName(), Level.ALL, null)});
		}
		LoggerConfig townyConfig = LoggerConfig.createLogger(false, Level.ALL, "Towny", null, appenderRefs.get(0), null, config, null);
		townyConfig.addAppender(appenders.get(0), Level.ALL, null);
		townyConfig.addAppender(appenders.get(3), Level.INFO, null);
		LoggerConfig townyMoneyConfig = LoggerConfig.createLogger(false, Level.ALL, "Towny-Money", null, appenderRefs.get(1), null, config, null);
		townyMoneyConfig.addAppender(appenders.get(1), Level.ALL, null);
		LoggerConfig townyDebugConfig = LoggerConfig.createLogger(false, Level.ALL, "Towny-Debug", null, appenderRefs.get(2), null, config, null);
		townyDebugConfig.addAppender(appenders.get(2), Level.ALL, null);
		
		config.addLogger("com.palmergames.bukkit.towny", townyConfig);
		config.addLogger("com.palmergames.bukkit.towny.money", townyMoneyConfig);
		config.addLogger("com.palmergames.bukkit.towny.debug", townyDebugConfig);
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