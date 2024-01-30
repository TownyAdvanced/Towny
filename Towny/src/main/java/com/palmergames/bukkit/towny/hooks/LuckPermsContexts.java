package com.palmergames.bukkit.towny.hooks;

import com.github.bsideup.jabel.Desugar;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LuckPermsContexts implements ContextCalculator<Player> {
	private LuckPerms luckPerms;
	private final Set<Calculator> calculators = new HashSet<>();

	public LuckPermsContexts(@NotNull Towny plugin) {
		registerContext("towny:resident", resident -> Collections.singleton(String.valueOf(resident.hasTown())), () -> Arrays.asList("true", "false"));
		registerContext("towny:nation_resident", resident -> Collections.singleton(String.valueOf(resident.hasNation())), () -> Arrays.asList("true", "false"));
		registerContext("towny:mayor", resident -> Collections.singleton(String.valueOf(resident.isMayor())), () -> Arrays.asList("true", "false"));
		registerContext("towny:king", resident -> Collections.singleton(String.valueOf(resident.isKing())), () -> Arrays.asList("true", "false"));
		registerContext("towny:insidetown", resident -> {
			PlayerCache cache = plugin.getCacheOrNull(resident.getUUID());
			if (cache == null)
				return Collections.emptyList();
			
			return Optional.ofNullable(cache.getLastTownBlock()).map(wc -> Collections.singleton(String.valueOf(wc.hasTownBlock()))).orElse(Collections.emptySet());
		}, () -> Arrays.asList("true", "false"));
		registerContext("towny:insideowntown", resident -> {
			PlayerCache cache = plugin.getCacheOrNull(resident.getUUID());
			if (cache == null)
				return Collections.emptyList();

			return Optional.ofNullable(cache.getLastTownBlock().getTownOrNull()).map(town -> Collections.singleton(String.valueOf(town.hasResident(resident)))).orElse(Collections.emptySet());
		}, () -> Arrays.asList("true", "false"));
		registerContext("towny:insideownplot", resident -> {
			PlayerCache cache = plugin.getCacheOrNull(resident.getUUID());
			if (cache == null)
				return Collections.emptyList();

			return Optional.ofNullable(cache.getLastTownBlock().getTownBlockOrNull()).map(townBlock -> Collections.singleton(String.valueOf(townBlock.hasResident(resident)))).orElse(Collections.emptySet());
		}, () -> Arrays.asList("true", "false"));
		registerContext("towny:townrank", Resident::getTownRanks, TownyPerms::getTownRanks);
		registerContext("towny:nationrank", Resident::getNationRanks, TownyPerms::getNationRanks);
		registerContext("towny:town", resident -> resident.hasTown() ? Collections.singleton(resident.getTownOrNull().getName()) : Collections.emptyList(), () -> TownyUniverse.getInstance().getTowns().stream().map(Town::getName).collect(Collectors.toSet()));
		registerContext("towny:nation", resident -> resident.hasNation() ? Collections.singleton(resident.getNationOrNull().getName()) : Collections.emptyList(), () -> TownyUniverse.getInstance().getNations().stream().map(Nation::getName).collect(Collectors.toSet()));
	
		this.calculators.removeIf(calculator -> !TownySettings.isContextEnabled(calculator.context));
		plugin.getLogger().info("Enabled LuckPerms contexts: " + this.calculators.stream().map(Calculator::context).collect(Collectors.joining(", ")));
	}
	
	public void registerContexts() {
		RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
		if (provider != null) {
			this.luckPerms = provider.getProvider();
			luckPerms.getContextManager().registerCalculator(this);
		} else
			this.luckPerms = null;
	}
	
	public void unregisterContexts() {
		if (this.luckPerms != null)
			this.luckPerms.getContextManager().unregisterCalculator(this);
	}
	
	private void registerContext(String context, Function<Resident, Iterable<String>> calculator, Supplier<Iterable<String>> suggestions) {
		calculators.add(new Calculator(context, calculator, suggestions));
	}
	
	@Override
	public void calculate(@NotNull Player player, @NotNull ContextConsumer contextConsumer) {
		Resident resident = TownyAPI.getInstance().getResident(player);
		if (resident == null)
			return;
		
		for (Calculator calculator : this.calculators)
			calculator.function().apply(resident).forEach(value -> contextConsumer.accept(calculator.context, value));
	}

	@Override
	public ContextSet estimatePotentialContexts() {
		ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
		
		for (Calculator calculator : this.calculators)
			calculator.suggestions().get().forEach(value -> builder.add(calculator.context, value));
		
		return builder.build();
	}
	
	@Desugar
	private record Calculator(String context, Function<Resident, Iterable<String>> function, Supplier<Iterable<String>> suggestions) {}
}
