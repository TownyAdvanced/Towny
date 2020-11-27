package com.palmergames.bukkit.towny.event.damage;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * @author LlmDl
 */
public class TownyPlayerDamagePlayerEvent extends TownyDamageEvent {

	private static final HandlerList handlers = new HandlerList();
	private final Player player;

	/**
	 * Event thrown when a player damages a player.
	 * Use ignorecancelled = true in order to filter out explosions Towny will already have stopped.
	 * 
	 * @param location Location of the entity being damaged.
	 * @param harmedEntity Entity getting exploded.
	 * @param cause DamageCause.
	 * @param townblock TownBlock or null if in the wilderness.
	 * @param cancelled Whether Towny will cancel this already.
	 * @param player Player causing the damage to the harmedEntity.
	 */
	public TownyPlayerDamagePlayerEvent(Location location, Entity harmedEntity, DamageCause cause, TownBlock townblock, boolean cancelled, Player attackingPlayer) {
		super(location, harmedEntity, cause, townblock, cancelled);
		this.player = attackingPlayer;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * @return Player harming the entity.
	 */
	public Player getAttackingPlayer() {
		return player;
	}
	
	public Player getVictimPlayer() {
		return (Player) entity;
	}
	
	@Nullable
	public Resident getAttackingResident() {
		return TownyUniverse.getInstance().getResident(player.getUniqueId());
	}
	
	@Nullable
	public Resident getVictimResident() {
		return TownyUniverse.getInstance().getResident(getVictimPlayer().getUniqueId());
	}
	
	@Nullable
	public Town getAttackerTown() {
		try {
			return getAttackingResident().getTown();
		} catch (NotRegisteredException ignored) {}
		return null;
	}

	@Nullable
	public Town getVictimTown() {
		try {
			return getVictimResident().getTown();
		} catch (NotRegisteredException ignored) {}
		return null;
	}
}
