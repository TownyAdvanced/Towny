package com.palmergames.bukkit.towny.object.typeapi;

import com.palmergames.bukkit.towny.object.TownBlock;

public abstract class TownBlockTypeHandler {
	/**
	 * Method executed on TownyBuildEvent
	 * @param townBlock Town Block affected to this event
	 * @return true if the event should be allowed to happen or false if it should be cancelled
	 */
	public abstract boolean onTownyBuild(TownBlock townBlock);

	/**
	 * Method executed on TownyDestroyEvent
	 * @param townBlock Town Block affected to this event
	 * @return true if the event should be allowed to happen or false if it should be cancelled
	 */
	public abstract boolean onTownyDestroy(TownBlock townBlock);

	/**
	 * Method executed on TownyItemUseEvent
	 * @param townBlock Town Block affected to this event
	 * @return true if the event should be allowed to happen or false if it should be cancelled
	 */
	public abstract boolean onTownyItemUse(TownBlock townBlock);

	/**
	 * Method executed on TownySwitchEvent
	 * @param townBlock Town Block affected to this event
	 * @return true if the event should be allowed to happen or false if it should be cancelled
	 */
	public abstract boolean onTownySwitch(TownBlock townBlock);

	/**
	 * Method executed on TownyBurnEvent
	 * @param townBlock Town Block affected to this event
	 * @return true if the event should be allowed to happen or false if it should be cancelled
	 */
	public abstract boolean onTownyBurn(TownBlock townBlock);

	/**
	 * Method executed on TownyExplodingBlocksEvent
	 * @param townBlock Town Block affected to this event
	 * @return true if the event should be allowed to happen or false if it should be cancelled
	 */
	public abstract boolean onTownyExplosion(TownBlock townBlock);

	/**
	 * Method executed on ???
	 * @param townBlock Town Block affected to this event
	 * @return true if the event should be allowed to happen or false if it should be cancelled
	 */
	public abstract boolean onTownyDamage(TownBlock townBlock);
}
