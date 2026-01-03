package com.palmergames.bukkit.towny.event.plot.group;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.PlotGroup;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlotGroupDeletedEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final PlotGroup plotGroup;
	private final Player player;
	private final Cause deletionCause;
	
	public PlotGroupDeletedEvent(@NotNull PlotGroup group, @Nullable Player player, @NotNull Cause deletionCause) {
		this.plotGroup = group;
		this.player = player;
		this.deletionCause = deletionCause;
	}

	/**
	 * @return The plot group that is being deleted.
	 */
	@NotNull
	public PlotGroup getPlotGroup() {
		return plotGroup;
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
		 * The plot group was deleted by a player via the /plot group delete command.
		 */
		DELETED,
		/**
		 * The plot group was deleted because all of its townblocks were removed. Not cancellable.
		 */
		NO_TOWNBLOCKS,
		/**
		 * The plot group was deleted because the town it was in being deleted/ruined. Not cancellable.
		 */
		TOWN_DELETED,
	}
}
