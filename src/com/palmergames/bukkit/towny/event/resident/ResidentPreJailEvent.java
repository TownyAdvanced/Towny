package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.JailReason;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ResidentPreJailEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Resident resident;
	private final Jail jail;
	private final int cell;
	private final int hours;
	private final double bail;
	private final JailReason reason;
	
	public ResidentPreJailEvent(Resident resident, Jail jail, int cell, int hours, double bail, JailReason reason) {
		this.resident = resident;
		this.jail = jail;
		this.cell = cell;
		this.hours = hours;
		this.bail = bail;
		this.reason = reason;
		setCancelMessage(Translation.of("msg_err_command_disable"));
	}

	public Resident getResident() {
		return resident;
	}
	
	public Jail getJail() {
		return jail;
	}

	public Town getJailTown() {
		return jail.getTown();
	}
	
	public TownBlock getJailTownBlock() {
		return jail.getTownBlock();
	}
	
	public int getCell() {
		return cell;
	}

	public int getHours() {
		return hours;
	}

	public double getBail() {
		return bail;
	}

	public JailReason getReason() {
		return reason;
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
