package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
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

public abstract class TownToggleEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Town town;
	private final CommandSender sender;
	private final boolean isAdminAction;
	private boolean isCancelled = false;
	private String cancellationMsg = Translation.of("msg_err_command_disable");
	
	/**
	 * A generic cancellable event thrown when a player uses the /town toggle {args} command.
	 * 
	 * @param sender CommandSender who has run the command.
	 * @param town Town which will have something cancelled.
	 * @param admin Whether this was executed by an admin.
	 */
	public TownToggleEvent(CommandSender sender, Town town, boolean admin) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.sender = sender;
		this.town = town;
		this.isAdminAction = admin;
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
		Player player = getPlayer();
		if (player != null)
			return TownyUniverse.getInstance().getResident(player.getUniqueId());
		
		return null;
	}
	
	@Nullable
	public Player getPlayer() {
		return sender instanceof Player ? (Player) sender : null;
	}

	public Town getTown() {
		return town;
	}
	
	public CommandSender getSender() {
		return sender;
	}

	public String getCancellationMsg() {
		return cancellationMsg;
	}

	public void setCancellationMsg(String cancellationMsg) {
		this.cancellationMsg = cancellationMsg;
	}

	/**
	 * @return true if this toggling is because of an admin or console.
	 */
	public boolean isAdminAction() {
		return isAdminAction;
	}
}
