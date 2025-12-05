package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;


public class DeleteTownEvent extends TownyObjDeleteEvent  {
    private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Resident mayor;
	private final Cause cause;
	private final CommandSender sender;
	private final int numTownBlocks;
    
    public DeleteTownEvent(@NotNull Town town, @Nullable Resident mayor, @NotNull int numTownBlocks, @NotNull Cause cause, @Nullable CommandSender sender) {
    	super(town.getName(), town.getUUID(), town.getRegistered());
		this.mayor = mayor;
		this.cause = cause;
		this.numTownBlocks = numTownBlocks;
		this.sender = sender;
    }

    /**
     *
     * @return the deleted town name.
     */
    public String getTownName() {
        return name;
    }

	/**
	 * 
	 * @return the deleted town uuid.
	 */
	public UUID getTownUUID() {
    	return uuid;
	}

	/**
	 * 
	 * @return the deleted town's time of creation (in epoch ms).
	 */
	public long getTownCreated() {
    	return registered;
	}

	/**
	 * @return the deleted town's mayor's UUID, or {@code null}.
	 */
	@Nullable
	public UUID getMayorUUID() {
		return mayor != null ? mayor.getUUID() : null;
	}

	/**
	 * @return The deleted town's mayor, or {@code null}.
	 */
	@Nullable
	public Resident getMayor() {
		return mayor;
	}

	@NotNull
	public Cause getCause() {
		return cause;
	}

	@NotNull
	public int getNumTownBlocks() {
		return numTownBlocks;
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
		 * The town was removed during database loading
		 */
		LOAD,
		/**
		 * The mayor of the town used the delete command.
		 * @see #isCommand() 
		 */
		COMMAND,
		/**
		 * An admin used the townyadmin command to delete a town.
		 * @see #isCommand() 
		 */
		ADMIN_COMMAND,
		/**
		 * All residents left or were removed from the town.
		 */
		NO_RESIDENTS,
		/**
		 * The town has no townblocks left and {@link TownySettings#isNewDayDeleting0PlotTowns()} is enabled.
		 */
		NO_TOWNBLOCKS,
		/**
		 * The town was deleted because it merged into another town.
		 */
		MERGED,
		/**
		 * The town was previously ruined and its ruined time expired.
		 */
		RUINED,
		/**
		 * The town couldn't pay its daily upkeep.
		 * @see #isUpkeep() 
		 */
		UPKEEP,
		/**
		 * The town reached its debt cap and couldn't pay upkeep
		 * @see #isUpkeep() 
		 */
		BANKRUPTCY;
		
		public boolean isCommand() {
			return this == COMMAND || this == ADMIN_COMMAND;
		}

		public boolean isUpkeep() {
			return this == UPKEEP || this == BANKRUPTCY;
		}
		
		@ApiStatus.Internal
		public boolean ignoresPreEvent() {
			return this == LOAD || this == ADMIN_COMMAND || this == MERGED || this == NO_RESIDENTS;
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