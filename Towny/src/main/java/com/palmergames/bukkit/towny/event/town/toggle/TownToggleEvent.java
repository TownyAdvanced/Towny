package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

public abstract class TownToggleEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Town town;
	private final CommandSender sender;
	private final boolean isAdminAction;

	/**
	 * A generic cancellable event thrown when a player uses the /town toggle {args} command.
	 * 
	 * @param sender CommandSender who has run the command.
	 * @param town Town which will have something cancelled.
	 * @param admin Whether this was executed by an admin.
	 */
	public TownToggleEvent(CommandSender sender, Town town, boolean admin) {
		this.sender = sender;
		this.town = town;
		this.isAdminAction = admin;
		setCancelMessage(Translation.of("msg_err_command_disable"));
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
