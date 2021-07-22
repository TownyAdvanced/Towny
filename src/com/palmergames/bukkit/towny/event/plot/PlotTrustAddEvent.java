package com.palmergames.bukkit.towny.event.plot;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PlotTrustAddEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final List<TownBlock> townBlocks;
	private final Resident trustedResident;
	private final Player player;
	private String cancelMessage = Translation.of("msg_err_command_disable");
	private boolean cancelled = false;
	
	public PlotTrustAddEvent(TownBlock townBlock, Resident trustedResident, Player player) {
		this(Collections.singletonList(townBlock), trustedResident, player);
	}
	
	public PlotTrustAddEvent(List<TownBlock> townBlocks, Resident trustedResident, Player player) {
		super(!Bukkit.isPrimaryThread());
		this.townBlocks = townBlocks;
		this.trustedResident = trustedResident;
		this.player = player;
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

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
