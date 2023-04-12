package com.palmergames.bukkit.towny.object.map;

import com.palmergames.bukkit.towny.object.WorldCoord;

import net.kyori.adventure.text.TextComponent;

/**
 * Object that is used to cache information used to display 
 * the Towny ASCII Map. Considered old after 30 seconds.
 * 
 * @author LlmDl
 * @since 0.97.0.10
 */
public class TownyMapData {

	private final WorldCoord worldCoord;
	private final long time;
	private final String symbol;
	private final TextComponent hoverText;
	private final String clickCommand;
	
	public TownyMapData(WorldCoord worldCoord, String symbol, TextComponent hoverText, String clickCommand) {
		this.worldCoord = worldCoord;
		this.time = System.currentTimeMillis();
		this.symbol = symbol;
		this.hoverText = hoverText;
		this.clickCommand = clickCommand;
	}

	public WorldCoord getWorldCoord() {
		return worldCoord;
	}

	public long getTime() {
		return time;
	}

	public String getSymbol() {
		return symbol;
	}

	public TextComponent getHoverText() {
		return hoverText;
	}

	public String getClickCommand() {
		return clickCommand;
	}

	public boolean isOld() {
		return System.currentTimeMillis() - getTime() > 30000;
	}
}
