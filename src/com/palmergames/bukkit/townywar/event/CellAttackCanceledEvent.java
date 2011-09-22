package com.palmergames.bukkit.townywar.event;

import org.bukkit.event.Event;

import com.palmergames.bukkit.townywar.CellUnderAttack;


public class CellAttackCanceledEvent extends Event {
	private static final long serialVersionUID = 2036661065011346448L;
	private CellUnderAttack cell;
	
	public CellAttackCanceledEvent(CellUnderAttack cell) {
		super("CellAttackCanceled");
		this.cell = cell;
		
	}
		
	public CellUnderAttack getCell() {
		return cell;
	}
}
