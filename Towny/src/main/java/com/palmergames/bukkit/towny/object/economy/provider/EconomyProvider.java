package com.palmergames.bukkit.towny.object.economy.provider;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.economy.adapter.EconomyAdapter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@ApiStatus.Internal
public sealed abstract class EconomyProvider permits VaultEconomyProvider, ReserveEconomyProvider, VaultUnlockedEconomyProvider {
	private boolean isLegacy = !TownySettings.getBoolean(ConfigNodes.ECO_ADVANCED_MODERN);

	/**
	 * @return The name of the plugin that provides the economy API in use, e.g. "Vault".
	 */
	public abstract String name();

	public abstract TownyEconomyHandler.EcoType economyType();

	/**
	 * @return The main economy adapter that should be used for all transactions.
	 */
	@Nullable
	public abstract EconomyAdapter mainAdapter();

	/**
	 * @return All existing registered adapters
	 */
	public abstract Collection<EconomyAdapter> economyAdapters();

	@Nullable
	public abstract EconomyAdapter getEconomyAdapter(final @NotNull String name);
	
	@ApiStatus.Internal
	public boolean isLegacy() {
		return this.isLegacy;
	}

	@ApiStatus.Internal
	public void setLegacy(final boolean legacy) {
		this.isLegacy = legacy;
	}
}
