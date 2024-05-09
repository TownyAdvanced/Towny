package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DeleteNationEvent extends TownyObjDeleteEvent  {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Resident king;
	private final Cause cause;
	private final CommandSender sender;

    public DeleteNationEvent(Nation nation, Resident king, Cause cause, CommandSender sender) {
        super(nation.getName(), nation.getUUID(), nation.getRegistered());
        this.king = king;
		this.cause = cause;
		this.sender = sender;
    }

    /**
     *
     * @return the deleted nation name.
     */
    public String getNationName() {
        return name;
    }

	/**
	 * @return the deleted nation uuid.
	 */
	public UUID getNationUUID() {
    	return uuid;
    }

	/**
	 * @return deleted nation time of creation (in ms).
	 */
	public long getNationCreated() {
    	return registered;
	}

    /**
	 * @return the deleted nation's leader's UUID, or {@code null}.
	 */
	@Nullable
	public UUID getLeaderUUID() {
		return king != null ? king.getUUID() : null;
	}

	/**
	 * @return The deleted nation's leader's Resident object, or {@code null}.
	 */
	@Nullable
	public Resident getLeader() {
		return king;
	}

	public Cause getCause() {
		return cause;
	}

	/**
	 * @return The command sender who caused the deletion
	 */
	@Nullable
	public CommandSender getSender() {
		return sender;
	}

	/**
	 * @return The {@link #getSender()} as a resident.
	 */
	@Nullable
	public Resident getSenderResident() {
		return sender instanceof Player player ? TownyAPI.getInstance().getResident(player) : null;
	}
	
	public enum Cause {
		UNKNOWN,
		/**
		 * The nation was removed during database loading.
		 */
		LOAD,
		/**
		 * The owner of the nation used the delete command.
		 * @see #isCommand() 
		 */
		COMMAND,
		/**
		 * An admin used the townyadmin command to delete the nation.
		 * @see #isCommand()
		 */
		ADMIN_COMMAND,
		/**
		 * The nation was removed for not having any towns in it.
		 */
		NO_TOWNS,
		/**
		 * The nation did not have a town that could satisfy {@link TownySettings#getNumResidentsCreateNation()} in order to be the capital.
		 */
		NOT_ENOUGH_RESIDENTS,
		/**
		 * The nation could not pay its daily upkeep.
		 */
		UPKEEP;
		
		public boolean isCommand() {
			return this == COMMAND || this == ADMIN_COMMAND;
		}
		
		@ApiStatus.Internal
		public boolean ignoresPreEvent() {
			return this == ADMIN_COMMAND || this == NO_TOWNS;
		}
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLER_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
}