package com.palmergames.bukkit.townywar.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.townywar.CellUnderAttack;


public class CellAttackEvent extends Event implements Cancellable {
	private static final long serialVersionUID = -6413227132896218785L;
	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    //////////////////////////////
    
	private Player player;
	private Block flagBaseBlock;
	private boolean cancel = false;
	private String reason = null;
	
	public CellAttackEvent(Player player, Block flagBaseBlock) {
		super("CellAttack");
		this.player = player;
		this.flagBaseBlock = flagBaseBlock;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public Block getFlagBaseBlock() {
		return flagBaseBlock;
	}
	
	public CellUnderAttack getData() {
		return new CellUnderAttack(player.getName(), flagBaseBlock);
	}

	@Override
	public boolean isCancelled() {
	    return cancel;
	}
	
	@Override
	public void setCancelled(boolean cancel) {
	    this.cancel = cancel;
	}
	
	public String getReason() {
		return reason;
	}
	
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	public boolean hasReason() {
		return reason != null;
	}
}
