package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TownPreUnclaimEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

    private final TownBlock townBlock;
    private final Town town;
    private final Cause cause;

    /**
     * Event thrown prior to a {@link TownBlock} being unclaimed by a {@link Town}.
     * <p>
     * This is cancellable but it is probably not a good idea to do
     * so without testing.
     *
     * @param town The Town unclaiming the TownBlock.
     * @param townBlock The TownBlock that will be unclaimed.
     */
    public TownPreUnclaimEvent(Town town, TownBlock townBlock, Cause cause) {
        this.town = town;
        this.townBlock = townBlock;
        this.cause = cause;

        // Don't even bother with things if town is null.
        if (this.town == null){
            setCancelMessage(Translation.of("msg_area_not_recog"));
            setCancelled(true);
        } else {
            setCancelMessage(Translation.of("msg_err_town_unclaim_canceled"));
        }
        
    }

    /**
     * @return the {@link Town}.
     */
    @Nullable
    public Town getTown() {
        return town;
    }
    
    /**
    * @return the soon-to-be unclaimed {@link TownBlock}.
    */
    public TownBlock getTownBlock() {
       return townBlock;
    }

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}

	public Cause getCause() {
		return cause;
	}

	public enum Cause {
		UNKNOWN,
		/**
		 * The townblock is being unclaimed for an unknown reason
		 */
		COMMAND,
		/**
		 * The townblock's town is being deleted.
		 * @see #isUpkeep() 
		 */
		DELETE;
		
		public boolean isCommand() {
			return this == COMMAND;
		}

		public boolean isDeleted() {
			return this == DELETE;
		}
		
		@ApiStatus.Internal
		public boolean ignoresPreEvent() {
			return this == DELETE;
		}
	}
}