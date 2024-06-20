package com.palmergames.bukkit.towny.event.plot.district;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.District;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DistrictDeletedEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final District district;
	private final Player player;
	private final Cause deletionCause;
	
	public DistrictDeletedEvent(@NotNull District district, @Nullable Player player, @NotNull Cause deletionCause) {
		this.district = district;
		this.player = player;
		this.deletionCause = deletionCause;
	}

	/**
	 * @return The district that is being deleted.
	 */
	@NotNull
	public District getDistrict() {
		return district;
	}

	/**
	 * @return The player associated with the deletion, if applicable.
	 */
	@Nullable
	public Player getPlayer() {
		return player;
	}

	public Cause getDeletionCause() {
		return deletionCause;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
	
	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
	
	public enum Cause {
		UNKNOWN,
		/**
		 * The district was deleted by a player via the /plot district delete command.
		 */
		DELETED,
		/**
		 * The district was deleted because all of its townblocks were removed.
		 */
		NO_TOWNBLOCKS,
		/**
		 * The district was deleted because the town it was in being deleted/ruined.
		 */
		TOWN_DELETED,
	}
}
