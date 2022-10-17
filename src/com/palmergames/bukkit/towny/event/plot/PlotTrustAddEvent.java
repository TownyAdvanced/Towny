package com.palmergames.bukkit.towny.event.plot;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.entity.Player;
import java.util.Collections;
import java.util.List;

public class PlotTrustAddEvent extends CancellableTownyEvent {
	private final List<TownBlock> townBlocks;
	private final Resident trustedResident;
	private final Player player;
	
	public PlotTrustAddEvent(TownBlock townBlock, Resident trustedResident, Player player) {
		this(Collections.singletonList(townBlock), trustedResident, player);
	}
	
	public PlotTrustAddEvent(List<TownBlock> townBlocks, Resident trustedResident, Player player) {
		this.townBlocks = townBlocks;
		this.trustedResident = trustedResident;
		this.player = player;
		setCancelMessage(Translation.of("msg_err_command_disable"));
	}

	/**
	 * @return The townBlock(s) where this resident is being added as trusted.
	 */
	public List<TownBlock> getTownBlocks() {
		return townBlocks;
	}

	/**
	 * @return The resident that is being added as trusted.
	 */
	public Resident getTrustedResident() {
		return trustedResident;
	}

	/**
	 * @return The player that is adding this resident as trusted.
	 */
	public Player getPlayer() {
		return player;
	}
}
