package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public abstract class TownToggleEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private Player player = null;
	private final Town town;
	private final CommandSender sender;
	private final boolean isAdminAction;
	
	/**
	 * A generic cancellable event thrown when a player uses the /town toggle {args} command.
	 * 
	 * @param player Player who has run the command.
	 * @param town Town which will have something cancelled.
	 * @param admin Whether this was executed by an admin.
	 */
	public TownToggleEvent(CommandSender sender, Town town, boolean admin) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.sender = sender;
		if (sender instanceof Player)
			this.player = (Player) sender;
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

	@Nullable
	public Resident getResident() {
		try {
			return TownyUniverse.getInstance().getDataSource().getResident(player.getName());
		} catch (NotRegisteredException ignored) {}
		return null;
	}
	
	@Nullable
	public Player getPlayer() {
		return player;
	}

	public CommandSender getSender() {
		return sender;
	}
	
	public Town getTown() {
		return town;
	}

	/**
	 * @return true if this toggling is because of an admin or console.
	 */
	public boolean isAdminAction() {
		return isAdminAction;
	}
	
}
