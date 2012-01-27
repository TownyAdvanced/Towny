package com.palmergames.bukkit.townywar.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.townywar.CellUnderAttack;


public class CellAttackCanceledEvent extends Event {
	private static final long serialVersionUID = 2036661065011346448L;
	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }

    //////////////////////////////
	private CellUnderAttack cell;
	
	public CellAttackCanceledEvent(CellUnderAttack cell) {
		super("CellAttackCanceled");
		this.cell = cell;
		
	}
		
	public CellUnderAttack getCell() {
		return cell;
	}
}
