package com.palmergames.bukkit.towny.event.mobs;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Called when Towny wants to prevent a mob from spawning. If canceled, the mob will be allowed to spawn, but follow-up removal attempts can still happen. These would be handled using {@link com.palmergames.bukkit.towny.event.MobRemovalEvent}.
 */
public class MobSpawnRemovalEvent extends Event implements Cancellable {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private boolean cancelled = false;
	private final @Nullable Entity entity;

	private final EntityType entityType;
	private final Location location;
	private final CreatureSpawnEvent.SpawnReason spawnReason;

	@ApiStatus.Internal
	public MobSpawnRemovalEvent(final @Nullable Entity entity, final EntityType entityType, final Location location, final CreatureSpawnEvent.SpawnReason spawnReason) {
		this.entity = entity;
		this.entityType = entityType;
		this.location = location;
		this.spawnReason = spawnReason;
	}

	/**
	 * @deprecated An entity instance is not available if we are cancelling the mob spawn using the {@link com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent}. This method will
	 * attempt to create a default entity when it is not available, but you should use {@link #getEntityOrNull()} instead.
	 */
	@Deprecated(forRemoval = true, since = "0.103.0.5")
	public @NonNull Entity getEntity() {
		if (this.entity == null) {
			return location.getWorld().createEntity(location, Objects.requireNonNull(entityType.getEntityClass(), "entity class"));
		}

		return this.entity;
	}

	/**
	 * {@return the entity being attempted to be spawned, or null}
	 */
	public @Nullable Entity getEntityOrNull() {
		return this.entity;
	}

	/**
	 * {@return the type of the entity being attempted to be spawned}
	 */
	public EntityType getEntityType() {
		return this.entityType;
	}

	/**
	 * {@return the location at which the entity is being attempted to be spawned}
	 */
	public Location getLocation() {
		return this.location.clone();
	}

	/**
	 * {@return the reason for the entity being spawned}
	 */
	public CreatureSpawnEvent.SpawnReason getSpawnReason() {
		return spawnReason;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean isCancelled) {
		cancelled = isCancelled;
	}

	@Override
	public @NonNull HandlerList getHandlers() {
		return HANDLER_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
}
