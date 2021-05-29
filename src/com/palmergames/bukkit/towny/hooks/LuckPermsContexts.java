package com.palmergames.bukkit.towny.hooks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class LuckPermsContexts implements ContextCalculator<Player>, Listener {
	
	private static final String RESIDENT_CONTEXT = "towny:resident";
	private static final String MAYOR_CONTEXT = "towny:mayor";
	private static final String KING_CONTEXT = "towny:king";
	
	private static final List<String> booleanContexts = Arrays.asList(RESIDENT_CONTEXT, MAYOR_CONTEXT, KING_CONTEXT);
	
	private static LuckPerms luckPerms;

	public LuckPermsContexts() {
		RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
		if (provider != null) {
			luckPerms = provider.getProvider();
			luckPerms.getContextManager().registerCalculator(this);
		}
	}
	
	@Override
	public void calculate(@NotNull Player player, @NotNull ContextConsumer contextConsumer) {
		Resident resident = TownyAPI.getInstance().getResident(player.getName());
		if (resident == null)
			return;
		
		contextConsumer.accept(RESIDENT_CONTEXT, Boolean.toString(resident.hasTown()));
		contextConsumer.accept(MAYOR_CONTEXT, Boolean.toString(resident.isMayor()));
		contextConsumer.accept(KING_CONTEXT, Boolean.toString(resident.isKing()));
	}

	@Override
	public ContextSet estimatePotentialContexts() {
		ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
		for (String context : booleanContexts) {
			builder.add(context, "true");
			builder.add(context, "false");
		}
		return builder.build();
	}
	
	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		if (event.getPlugin() instanceof Towny) {
			luckPerms.getContextManager().unregisterCalculator(this);
		}
	}
}
