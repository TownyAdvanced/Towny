package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * An event fired when a '/town unclaim [args]' command is issued, prior to any other calculations.
 * <p>
 * Useful for plugins (like war systems) wanting halt the command in it's tracks.
 * For an example, see Flag War's FlagWarCustomListener.onWarPreUnclaim().
 * <p>
 * Not to be confused with {@link TownPreUnclaimEvent}, which is handled within the
 * {@link com.palmergames.bukkit.towny.db.TownyDatabaseHandler}.
 */
public class TownPreUnclaimCmdEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private final Town town;
  private final Resident resident;
  private final TownyWorld townyWorld;
  private String cancelMessage = Translation.of("msg_err_town_unclaim_canceled");
  private boolean isCancelled = false;

  /**
   * @return the event's {@link HandlerList}
   */
  @NotNull
  @Override
  public HandlerList getHandlers() {
    return getHandlerList();
  }

  private static HandlerList getHandlerList() {
    return handlers;
  }

  /**
   * Constructs the TownPreUnclaimCmdEvent and stores data an external war plugin may use.
   *
   * @param town The {@link Town} about to process un-claiming (a) plot(s).
   * @param resident The {@link Resident} who initiated the command.
   * @param world The {@link TownyWorld} in which the resident is in.
   */
  public TownPreUnclaimCmdEvent(Town town, Resident resident, TownyWorld world) {
    this.town = town;
    this.resident = resident;
    this.townyWorld = world;
  }

  /**
   * @return Tells the listener if the event has previously been caught and cancelled.
   */
  @Override
  public boolean isCancelled() {
    return isCancelled;
  }

  /**
   * @param cancelled Sets the event as cancelled, or unsets a cancellation from a previous handler.
   */
  @Override
  public void setCancelled(boolean cancelled) {
    this.isCancelled = cancelled;
  }

  /**
   * @return Gets the {@link Town} which would have it's TownBlocks unclaimed.
   */
  public Town getTown() {
    return town;
  }

  /**
   * @return Gets the {@link Resident} that issued the '/t unclaim ...' command.
   */
  public Resident getResident() {
    return resident;
  }

  /**
   * @return Gets the {@link TownyWorld} where the land is being unclaimed.
   */
  public TownyWorld getTownyWorld() {
    return townyWorld;
  }

  /**
   * @return Gets the message for if the event were cancelled.
   */
  public String getCancelMessage() {
    return cancelMessage;
  }

  /**
   * Overrides the cancellation message previously set by the event, or by an event handler up the stack.
   *
   * @param cancelMessage The message to send back to the server when the event is cancelled.
   */
  public void setCancelMessage(String cancelMessage) {
    this.cancelMessage = cancelMessage;
  }
}
