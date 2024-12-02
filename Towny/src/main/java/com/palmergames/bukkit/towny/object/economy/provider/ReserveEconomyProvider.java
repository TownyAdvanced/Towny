package com.palmergames.bukkit.towny.object.economy.provider;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.economy.adapter.EconomyAdapter;
import com.palmergames.bukkit.towny.object.economy.adapter.ReserveEconomyAdapter;
import net.tnemc.core.Reserve;
import net.tnemc.core.economy.EconomyAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ReserveEconomyProvider extends EconomyProvider {
	private final Reserve reserve;
	private final Function<EconomyAPI, ReserveEconomyAdapter> adapterFunction = api -> !isLegacy() ? new ReserveEconomyAdapter(api) :  new ReserveEconomyAdapter.Legacy(api);

	public ReserveEconomyProvider(Reserve reserve) {
		this.reserve = reserve;
	}

	@Override
	public String name() {
		return "Reserve";
	}

	@Override
	public TownyEconomyHandler.EcoType economyType() {
		return TownyEconomyHandler.EcoType.RESERVE;
	}

	@Override
	public @Nullable EconomyAdapter mainAdapter() {
		if (reserve.economy() == null)
			return null;
		
		return adapterFunction.apply(reserve.economy());
	}

	@Override
	public Collection<EconomyAdapter> economyAdapters() {
		return reserve.getRegisteredEconomies().values().stream().map(adapterFunction).collect(Collectors.toSet());
	}

	@Override
	public @Nullable EconomyAdapter getEconomyAdapter(@NotNull String name) {
		return Optional.ofNullable(reserve.getRegisteredEconomies().get(name)).map(adapterFunction).orElse(null);
	}
}
