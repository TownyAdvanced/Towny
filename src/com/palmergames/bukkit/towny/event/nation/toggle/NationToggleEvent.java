package com.palmergames.bukkit.towny.event.nation.toggle;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

public abstract class NationToggleEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final CommandSender sender;
	private Player player = null;
	private final Nation nation;
	private final boolean isAdminAction;
	
	public NationToggleEvent(CommandSender sender, Nation nation, boolean admin) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.sender = sender;
		if (sender instanceof Player)
			this.player = (Player) sender;
		this.nation = nation;
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

	public CommandSender getSender() {
		return sender;
	}
	
	@Nullable
	public Player getPlayer() {
		return player;
	}

	public Nation getNation() {
		return nation;
	}
	
	/**
	 * @return true if this toggling is because of an admin or console.
	 */
	public boolean isAdminAction() {
		return isAdminAction;
	}

}
