package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event called whenever nation bonus blocks are being fetched.
 */
public class NationBonusCalculationEvent extends Event {
	private static HandlerList handlers = new HandlerList();
	
	private Nation nation;
	private int bonusBlocks;
	
	public NationBonusCalculationEvent(Nation nation, int bonusBlocks) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.bonusBlocks = bonusBlocks;
	}
	
	public Nation getNation() {
		return nation;
	}
		
	public int getBonusBlocks() {
		return bonusBlocks;
	}
	
	public void setBonusBlocks(int bonusBlocks) {
		this.bonusBlocks = bonusBlocks;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
