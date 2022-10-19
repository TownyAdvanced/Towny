package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import org.jetbrains.annotations.Nullable;

public class TownPreUnclaimEvent extends CancellableTownyEvent {

    private final TownBlock townBlock;
    private final Town town;

    /**
     * Event thrown prior to a {@link TownBlock} being unclaimed by a {@link Town}.
     * <p>
     * This is cancellable but it is probably not a good idea to do
     * so without testing.
     *
     * @param town The Town unclaiming the TownBlock.
     * @param townBlock The TownBlock that will be unclaimed.
     */
    public TownPreUnclaimEvent(Town town, TownBlock townBlock) {
        this.town = town;
        this.townBlock = townBlock;

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
}