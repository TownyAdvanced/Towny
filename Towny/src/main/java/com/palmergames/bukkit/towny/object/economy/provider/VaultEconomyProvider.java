package com.palmergames.bukkit.towny.object.economy.provider;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.economy.adapter.EconomyAdapter;
import com.palmergames.bukkit.towny.object.economy.adapter.VaultEconomyAdapter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class VaultEconomyProvider extends EconomyProvider {
	private final Function<RegisteredServiceProvider<Economy>, EconomyAdapter> adapterFunction = registration -> !isLegacy() ? new VaultEconomyAdapter(registration.getProvider()) : new VaultEconomyAdapter.Legacy(registration.getProvider());

	@Override
	public String name() {
		return "Vault";
	}

	@Override
	public TownyEconomyHandler.EcoType economyType() {
		return TownyEconomyHandler.EcoType.VAULT;
	}

	@Override
	public EconomyAdapter mainAdapter() {
		RegisteredServiceProvider<Economy> registration = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
		if (registration == null)
			return null;
		
		return adapterFunction.apply(registration);
	}

	@Override
	public Collection<EconomyAdapter> economyAdapters() {
		return getEconomyRegistrations().values().stream().map(adapterFunction).collect(Collectors.toList());
	}

	@Override
	public @Nullable EconomyAdapter getEconomyAdapter(@NotNull String name) {
		return Optional.ofNullable(getEconomyRegistrations().get(name)).map(adapterFunction).orElse(null);
	}

	private Map<String, RegisteredServiceProvider<Economy>> getEconomyRegistrations() {
		return Bukkit.getServer().getServicesManager().getRegistrations(Economy.class).stream().collect(Collectors.toMap(registration -> registration.getProvider().getName(), registration -> registration));
	}
}
