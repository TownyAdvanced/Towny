package com.palmergames.bukkit.townywar.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.townywar.CellUnderAttack;


public class CellWonEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    //////////////////////////////
    
	
	private CellUnderAttack cellAttackData;
	
	public CellWonEvent(CellUnderAttack cellAttackData) {
		super();
		this.cellAttackData = cellAttackData;
	}
	
	public CellUnderAttack getCellAttackData() {
		return cellAttackData;
	}
}
