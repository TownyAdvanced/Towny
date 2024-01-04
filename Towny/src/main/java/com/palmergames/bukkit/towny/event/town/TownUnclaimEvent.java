package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This event fires after a TownBlock has been unclaimed and stores the {@link WorldCoord}
 * <p>
 * If you need to cancel the unclaim process, please use TownPreUnclaimEvent.
 */
public class TownUnclaimEvent extends Event {
  private static final HandlerList handlers = new HandlerList();
  private final Town town;
  private final WorldCoord worldCoord;
  private boolean isOverClaim;

  public TownUnclaimEvent(Town town, WorldCoord worldCoord, boolean isOverClaim) {
    super(!Bukkit.getServer().isPrimaryThread());
    this.town = town;
    this.worldCoord = worldCoord;
	this.isOverClaim = isOverClaim;
  }

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return getHandlerList();
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

	/**
	 * Whether the unclaim has resulted from takeoverclaim.
	 * @return true if the unclaim resulted from /t takeoverclaim.
	 */
  public boolean isOverClaim() {
  	return isOverClaim;
  }

	/**
   * @return {@link Town} which is unclaiming land.
   */
  @Nullable
  public Town getTown() {
    return town;
  }


  /**
   * Gets the unclaimed {@link WorldCoord}.
   * @return The {@link WorldCoord} unclaimed.
   */
  public WorldCoord getWorldCoord() {
    return worldCoord;
  }
}
