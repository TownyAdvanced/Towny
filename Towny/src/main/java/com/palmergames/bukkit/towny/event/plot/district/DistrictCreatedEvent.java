package com.palmergames.bukkit.towny.event.plot.district;

import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a district is created.
 */
public class DistrictCreatedEvent extends DistrictAddEvent {
	public DistrictCreatedEvent(District district, TownBlock townBlock, Player player) {
		super(district, townBlock, player);
	}

	/**
	 * @return The initial townblock that this district is being created with.
	 */
	@Override
	@NotNull
	public TownBlock getTownBlock() {
		return super.getTownBlock();
	}
}
