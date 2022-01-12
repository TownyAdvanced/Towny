package com.palmergames.bukkit.towny.exceptions.initialization;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when Towny fails to initialize.
 */
public class TownyInitException extends RuntimeException {
	private static final long serialVersionUID = -1943705202251722549L;
	private final TownyError error;
	private boolean safeMode = true;

	public TownyInitException(@NotNull String message, @NotNull TownyError error) {
		super(message);
		this.error = error;
	}

	public TownyInitException(@NotNull String message, @NotNull TownyError error, @NotNull Throwable t) {
		super(message, t);
		this.error = error;
	}

	public TownyInitException(@NotNull String message, @NotNull TownyError error, boolean safeMode) {
		super(message);
		this.error = error;
		this.safeMode = safeMode;
	}

	public TownyError getError() {
		return error;
	}
	
	public boolean shouldSafeMode() {
		return safeMode;
	}

	public enum TownyError {
		OTHER,
		MAIN_CONFIG,
		LOCALIZATION,
		DATABASE,
		DATABASE_CONFIG,
		PERMISSIONS,
	}
}
