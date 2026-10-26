package com.palmergames.bukkit.towny.object.resident.mode;

import com.palmergames.bukkit.towny.object.Resident;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class OptionResidentMode<T> extends AbstractResidentMode {

	private final Map<Resident, T> options = new ConcurrentHashMap<>();

	public OptionResidentMode(String name, String permissionNode) {
		super(name, permissionNode);
	}

	/**
	 * Get the resident mode option for a resident with the resident mode, or null.
	 *
	 * @param resident The resident whom to get the resident mode option for.
	 * @return the resident mode option for the resident, or null.
	 */
	@Nullable
	protected T getOption(Resident resident) {
		return options.get(resident);
	}

	/**
	 * Set the resident mode option for a resident with the resident mode, replacing any previous resident mode option for the resident.
	 *
	 * @param resident The resident whom to set the resident mode option for.
	 */
	protected void setOption(Resident resident, T option) {
		options.put(resident, option);
	}

	/**
	 * Clear the resident mode option for a resident with the resident mode.
	 *
	 * @param resident The resident whom to clear the resident mode option.
	 */
	protected void clearOptions(Resident resident) {
		options.remove(resident);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void toggle(Resident resident) {
		toggle(resident, new String[0]);
	}

	/**
	 * Toggle the resident mode on or off.
	 *
	 * @param resident Resident to toggle mode on or off.
	 * @param options  Resident mode options to set for toggling this resident mode.
	 */
	protected abstract void toggle(Resident resident, @NotNull String... options);
}
