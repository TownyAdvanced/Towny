package com.palmergames.bukkit.towny.event.plot;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PlotTrustRemoveEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final List<TownBlock> townBlocks;
	private final Resident trustedResident;
	private final Player player;
	
	public PlotTrustRemoveEvent(TownBlock townBlock, Resident trustedResident, Player player) {
		this(Collections.singletonList(townBlock), trustedResident, player);
	}
	
	public PlotTrustRemoveEvent(List<TownBlock> townBlocks, Resident trustedResident, Player player) {
		this.townBlocks = townBlocks;
		this.trustedResident = trustedResident;
		this.player = player;
		setCancelMessage(Translation.of("msg_err_command_disable"));
	}

	/**
	 * @return The resident that is being removed as trusted.
	 */
	public Resident getTrustedResident() {
		return trustedResident;
	}

	/**
	 * @return The townBlock(s) where this resident is being removed as trusted.
	 */
	public List<TownBlock> getTownBlocks() {
		return townBlocks;
	}

	/**
	 * @return The player that is removing this resident as trusted.
	 */
	public Player getPlayer() {
		return player;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
