package com.palmergames.bukkit.towny.event.nation.toggle;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translation;

public abstract class NationToggleEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private Player player = null;
	private final CommandSender sender;
	private final Nation nation;
	private final boolean isAdminAction;
	
	public NationToggleEvent(CommandSender sender, Nation nation, boolean admin) {
		this.sender = sender;
		if (sender instanceof Player)
			this.player = (Player) sender;
		this.nation = nation;
		this.isAdminAction = admin;
		setCancelMessage(Translation.of("msg_err_command_disable"));
	}

	@Nullable
	public Resident getResident() {
		return TownyUniverse.getInstance().getResident(player.getUniqueId());
	}
	
	@Nullable
	public Player getPlayer() {
		return player;
	}
	
	public CommandSender getSender() {
		return sender;
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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
