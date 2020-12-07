package com.palmergames.bukkit.towny.war.common.events;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * An event fired when a '/town unclaim' command is issued. Useful for war systems wanting halt that
 * in it's tracks.
 */
public class WarPreUnclaimEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private final Town town;
  private final Resident resident;
  private final TownyWorld townyWorld;
  private String cancelMessage = Translation.of("msg_err_town_unclaim_canceled");
  private boolean isCancelled = false;

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return getHandlerList();
  }

  private static HandlerList getHandlerList() {
    return handlers;
  }

  /**
   * Constructs the WarPreUnclaimEvent and stores data an external war plugin may use.
   *
   * @param town The {@link Town} about to process un-claiming (a) plot(s).
   * @param resident The {@link Resident} who initiated the command.
   * @param world The {@link TownyWorld} in which the resident is in.
   */
  public WarPreUnclaimEvent(Town town, Resident resident, TownyWorld world) {
    this.town = town;
    this.resident = resident;
    this.townyWorld = world;
  }

  @Override
  public boolean isCancelled() {
    return isCancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.isCancelled = cancelled;
  }

  public Town getTown() {
    return town;
  }

  public Resident getResident() {
    return resident;
  }

  public TownyWorld getTownyWorld() {
    return townyWorld;
  }

  public String getCancelMessage() {
    return cancelMessage;
  }

  public void setCancelMessage(String cancelMessage) {
    this.cancelMessage = cancelMessage;
  }
}
