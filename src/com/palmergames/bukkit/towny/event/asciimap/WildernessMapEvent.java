package com.palmergames.bukkit.towny.event.asciimap;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class WildernessMapEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private String mapSymbol = "-";
	private TextComponent hoverText;
	private String clickCommand = "/towny:townyworld";
	final private WorldCoord worldCoord;
	
	public WildernessMapEvent(TownyWorld world, int x, int z) {
		this.worldCoord = WorldCoord.parseWorldCoord(world.getName(), x * TownySettings.getTownBlockSize(), z * TownySettings.getTownBlockSize());
		this.hoverText = Component.text(world.getUnclaimedZoneName()).color(NamedTextColor.DARK_RED).append(Component.text(" (" + x + ", " + z + ")").color(NamedTextColor.WHITE));
	}

	/**
	 * The symbol used for the WorldCoord on the map.
	 * @return symbol used on the map;
	 */
	public String getMapSymbol() {
		return mapSymbol;
	}

	/**
	 * Set the symbol used for the WorldCoord on the map.
	 * @param mapSymbol String which can only be a single character long.
	 */
	public void setMapSymbol(String mapSymbol) {
		this.mapSymbol = mapSymbol;
	}

	/**
	 * Get the text shown on hover.
	 * @return TextComponent which builds the hover text.
	 */
	public TextComponent getHoverText() {
		return hoverText;
	}

	/**
	 * Set the hovertext.
	 * @param hoverText TextComponent which becomes the hover text.
	 */
	public void setHoverText(TextComponent hoverText) {
		this.hoverText = hoverText;
	}

	/**
	 * Get the command used when clicking on a symbol on the map.
	 * @return String which consists of the command.
	 */
	public String getClickCommand() {
		return clickCommand;
	}

	/**
	 * Set the command which is run when the map is clicked on.
	 * @param clickCommand String which consists of the command.
	 */
	public void setClickCommand(String clickCommand) {
		this.clickCommand = clickCommand;
	}

	/**
	 * Get the WorldCoord which is being shown on the map.
	 * @return WorldCoord which is being shown on the map.
	 */
	public WorldCoord getWorldCoord() {
		return worldCoord;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}

}
