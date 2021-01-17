package com.palmergames.bukkit.towny.object.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface DataFieldDeserializer<T extends CustomDataField<?>> {
	
	@Nullable
	T deserialize(@NotNull String key, @Nullable String value);
	
}
