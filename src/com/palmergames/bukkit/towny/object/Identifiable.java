package com.palmergames.bukkit.towny.object;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface Identifiable {
	@NotNull
	UUID getUniqueIdentifier();
}
