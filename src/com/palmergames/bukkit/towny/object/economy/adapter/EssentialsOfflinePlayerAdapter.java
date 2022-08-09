package com.palmergames.bukkit.towny.object.economy.adapter;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class EssentialsOfflinePlayerAdapter extends OfflinePlayerAdapter {
	public EssentialsOfflinePlayerAdapter(@NotNull String name, @NotNull UUID uuid) {
		super(name, uuid);
	}
	
	@Override
	public @NotNull UUID getUniqueId() {
		if (Thread.currentThread().getStackTrace()[2].getClassName().equals("com.earth2me.essentials.economy.vault.VaultEconomyProvider"))
			return UUID.nameUUIDFromBytes(super.getUniqueId().toString().getBytes(StandardCharsets.UTF_8));
		
		return super.getUniqueId();
	}
}
