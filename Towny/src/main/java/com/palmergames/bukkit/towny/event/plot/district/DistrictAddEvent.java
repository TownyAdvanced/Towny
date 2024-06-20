package com.palmergames.bukkit.towny.event.plot.district;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a townblock is added into a district
 */
public class DistrictAddEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final District district;
	private final TownBlock townBlock;
	private final Player player;
	
	public DistrictAddEvent(final District district, final TownBlock townBlock, final Player player) {
		this.district = district;
		this.townBlock = townBlock;
		this.player = player;
	}
	
	@NotNull
	public District getDistrict() {
		return district;
	}
	
	@NotNull
	public TownBlock getTownBlock() {
		return townBlock;
	}
	
	@NotNull
	public Player getPlayer() {
		return player;
	}
	
	@NotNull
	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
