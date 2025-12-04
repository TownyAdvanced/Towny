package com.palmergames.bukkit.towny.object;

import com.google.common.base.Preconditions;

import java.util.UUID;

public record NameAndId(String name, UUID uuid) {
	public NameAndId {
		Preconditions.checkNotNull(name, "name must not be null");
		Preconditions.checkNotNull(uuid, "uuid must not be null");
	}
}
