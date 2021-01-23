package com.palmergames.bukkit.towny.object.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Functional interface for deserializing a specific CustomDataField class.
 * 
 * @param <T> Specific CustomDataField class to deserialize.
 */
@FunctionalInterface
public interface DataFieldDeserializer<T extends CustomDataField<?>> {

	/**
	 * Returns a new class that extends CustomDataField given a key and string value.
	 * If this method returns {@code null}, the specific metadata with this key and value will
	 * not be loaded and skipped.
	 * 
	 * @param key Key for the CustomDataField
	 * @param value String value for the CustomDataField which can be {@code null}.
	 *                 
	 * @return the deserialized CustomDataField class.
	 */
	@Nullable
	T deserialize(@NotNull String key, @Nullable String value);
	
}
