package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translation;

/**
 * Event that gets fired when a nation's king changes.
 */
public class NationKingChangeEvent extends CancellableTownyEvent {

    private final Resident oldKing;
    private final Resident newKing;

    public NationKingChangeEvent(Resident oldKing, Resident newKing) {
        this.oldKing = oldKing;
        this.newKing = newKing;
        this.setCancelMessage(Translation.of("msg_err_command_disable"));
    }

    public Resident getOldKing() {
        return oldKing;
    }

    public Resident getNewKing() {
        return newKing;
    }

    public Nation getNation() {
        return newKing.getNationOrNull();
    }

    public boolean isCapitalChange() {
        return !TownyAPI.getInstance().getResidentTownOrNull(oldKing).hasResident(newKing);
    }
}
