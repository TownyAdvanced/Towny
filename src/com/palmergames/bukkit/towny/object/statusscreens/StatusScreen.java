package com.palmergames.bukkit.towny.object.statusscreens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.Nullable;
import solar.squares.pixelwidth.PixelWidthSource;

public class StatusScreen {

	Map<String, Component> components = new LinkedHashMap<>();
	final static int MAX_WIDTH = 320;
	private final PixelWidthSource source = PixelWidthSource.pixelWidth();
	private final CommandSender sender;
	
	public StatusScreen(CommandSender sender) {
		this.sender = sender;
	}

	public CommandSender getCommandSender() {
		return sender;
	}

	public void addComponentOf(String name, String text) {
		components.put(name, TownyComponents.miniMessage(text));
	}

	public void addComponentOf(String name, Component component) {
		components.put(name, component);
	}
	
	@Deprecated
	public void addComponentOf(String name, String text, ClickEvent click) {
		components.put(name, Component.text(text).clickEvent(click));
	}
	
	@Deprecated
	public void addComponentOf(String name, String text, HoverEvent hover) {
		components.put(name, Component.text(text).hoverEvent(hover));
	}
	
	@Deprecated
	public void addComponentOf(String name, String text, HoverEvent hover, ClickEvent click) {
		components.put(name, Component.text(text).hoverEvent(hover).clickEvent(click));
	}

	public void removeStatusComponent(String name) {
		components.remove(name);
	}
	
	public Collection<Component> getComponents() {
		return Collections.unmodifiableCollection(components.values());
	}
	
	public boolean hasComponent(String name) {
		return components.containsKey(name);
	}
	
	@Nullable
	public Component getComponentOrNull(String name) {
		return components.get(name);
	}
	
	public boolean replaceComponent(String name, Component replacement) {
		return components.replace(name, replacement) != null;
	}
	
	public Component getFormattedStatusScreen() {
		Component screen = Component.empty();
		Component currentLine = Component.empty();
		List<Component> components = new ArrayList<>(this.components.values());
		
		// Cycle over all components in the status screen.
		for (Component nextComp : components) {
			if (nextComp.equals(Component.newline())) {
				// We're dealing with a component which is just a new line, make a new line.
				if (!currentLine.equals(Component.empty()))
					screen = screen.append(currentLine);
				
				screen = screen.append(Component.newline());
				currentLine = Component.empty();
				continue;
			}
			if (currentLine.equals(Component.empty())) {
				// We're dealing with a new line and the nextComp has no children to process,
				// nextComp becomes the start of a line.
				currentLine = nextComp;
				continue;
			}
			// We're dealing with a component made of children, probably the ExtraFields or AdditionalLines.
			/*
			if (!nextComp.children().isEmpty()) {
				// nextComp starts with a new line component with children to follow, start a new line.
				if (nextComp.equals(Component.newline())) {
					screen = screen.append(currentLine).append(Component.newline());
					currentLine = Component.empty();
				}
				// Cycle over all child components.
				for (Component child : nextComp.children()) {
					if (child.equals(Component.newline())) {
						// We're dealing with a child component which is just a new line, make a new line.
						screen = screen.append(currentLine).append(Component.newline());
						currentLine = Component.empty();
						continue;
					}
					if (currentLine.equals(Component.empty())) {
						// We're dealing with a new line, our child component becomes the start of a line.
						currentLine = child;
						continue;
					}
					if (lineWouldBeTooLong(currentLine, child)) {
						// We've found a child which will make the line too long,
						// Dump currentLine into lines and start over with nextChild starting the new line.
						screen = screen.append(currentLine).append(Component.newline());
						currentLine = child;
						continue;
					}
					// We have a child which will fit onto the current line.
					currentLine = currentLine.append(Component.space().append(child));
				}
				// The loop is done, if anything was left in the currentLine dump it into lines.
				if (!currentLine.equals(Component.empty())) {
					screen = screen.append(currentLine).append(Component.newline());
					currentLine = Component.empty();
				}
				continue;
			}
			*/
			// We're dealing with a Component that has no children.
			if (lineWouldBeTooLong(currentLine, nextComp)) {
				// We've found a component which will make the line too long,
				// Dump currentLine into lines and start over with nextComp starting the new line.
				screen = screen.append(currentLine).append(Component.newline());
				currentLine = nextComp;
				continue;
			}
			// We have a component that will fit onto the current line.
			currentLine = currentLine.append(Component.space().append(nextComp));
		}
		
		// The loop is done, if anything was left in currentLine dump it into lines.
		if (!currentLine.equals(Component.empty()))
			screen = screen.append(currentLine).append(Component.newline());

		return screen;
	}
	
	private boolean lineWouldBeTooLong(Component line, Component comp) {
		return source.width(line) + source.width(comp) + 1 > MAX_WIDTH;
	}
}
