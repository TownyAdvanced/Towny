package com.palmergames.bukkit.towny.event.damage;

import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jetbrains.annotations.NotNull;

/**
 * @since 0.97.0.9
 */
public class TownyDispenserDamageEntityEvent extends TownyDamageEvent {
	private static final HandlerList handlerList = new HandlerList();
	private final Block dispenserBlock;
	
	public TownyDispenserDamageEntityEvent(Location loc, Entity entity, DamageCause cause, TownBlock townblock, boolean cancelled, Block dispenserBlock) {
		super(loc, entity, cause, townblock, cancelled);
		this.dispenserBlock = dispenserBlock;
	}

	/**
	 * Gets the dispenser block that caused the damage.
	 */
	public Block getDispenserBlock() {
		return dispenserBlock;
	}
	
	public static HandlerList getHandlerList() {
		return handlerList;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlerList;
	}
}
