package com.palmergames.bukkit.towny.event.damage;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * An event thrown when Towny will determine the explosion status of 
 * a townblock, or plot, in a town.
 */
public class TownBlockExplosionTestEvent extends Event {

		private static final HandlerList handlers = new HandlerList();
		private final Town town;
		private final TownBlock townBlock;
		private boolean explosion;

		public TownBlockExplosionTestEvent(TownBlock townBlock, Town town, boolean explosion) {
			this.town = town;
			this.townBlock = townBlock;
			this.setExplosion(explosion);
		}


		public static HandlerList getHandlerList() {
			return handlers;
		}

		public HandlerList getHandlers() {
			return handlers;
		}

		/**
		 * @return the TownBlock which is having its explosion status decided.
		 */
		@NotNull
		public TownBlock getTownBlock() {
			return townBlock;
		}

		/**
		 * @return the Town where this test is made.
		 */
		@NotNull
		public Town getTown() {
			return town;
		}

		/**
		 * @return true if the townblock has explosions on.
		 */
		public boolean isExplosion() {
			return explosion;
		}

		/**
		 * Sets the explosion status and outcome of the event.
		 * @param explosion whether the event will result in explosions being on or off in the townblock.
		 */
		public void setExplosion(boolean explosion) {
			this.explosion = explosion;
		}
}
