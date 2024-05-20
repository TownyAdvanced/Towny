package com.palmergames.bukkit.towny.object.economy;

import java.util.UUID;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import org.jetbrains.annotations.Nullable;

/**
 * For internal use only.
 */
public class TownyServerAccount extends Account {
	private static final UUID uuid = UUID.fromString("a73f39b0-1b7c-2930-b4a3-ce101812d926");
	private static final String name = TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT);
	private static final ThreadLocal<TownyWorld> world = ThreadLocal.withInitial(() -> TownyUniverse.getInstance().getTownyWorlds().get(0));

	public TownyServerAccount() {
		super(name, uuid, world::get);
	}

	@Override
	protected synchronized boolean addMoney(double amount) {
		return TownyEconomyHandler.add(this, amount);
	}

	@Override
	protected synchronized boolean subtractMoney(double amount) {
		return TownyEconomyHandler.subtract(this, amount);
	}
	
	public static void setWorld(@Nullable TownyWorld townyWorld) {
		if (townyWorld == null) {
			world.remove();
		} else {
			world.set(townyWorld);
		}
	}
}
