package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

public abstract class TownPreToggleEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private final Town town;
	private boolean isCancelled = false;
	private String cancellationMsg = Translation.of("msg_err_command_disable");
	
	/**
	 * A generic cancellable event thrown when a player uses the /town toggle {args} command.
	 * 
	 * @param player Player who has run the command.
	 * @param town Town which will have something cancelled.
	 */
	public TownPreToggleEvent(Player player, Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.town = town;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.isCancelled = cancel;
	}

	@Nullable
	public Resident getResident() {
		try {
			return TownyUniverse.getInstance().getDataSource().getResident(player.getName());
		} catch (NotRegisteredException ignored) {}
		return null;
	}
	
	public Player getPlayer() {
		return player;
	}

	public Town getTown() {
		return town;
	}

	public String getCancellationMsg() {
		return cancellationMsg;
	}

	public void setCancellationMsg(String cancellationMsg) {
		this.cancellationMsg = cancellationMsg;
	}
}
