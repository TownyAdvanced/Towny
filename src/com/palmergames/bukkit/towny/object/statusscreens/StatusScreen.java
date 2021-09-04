package com.palmergames.bukkit.towny.object.statusscreens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.util.ChatPaginator;

import com.palmergames.bukkit.util.Colors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;


public class StatusScreen {

	public StatusScreen() {}

	Map<String, TextComponent> components = new LinkedHashMap<>();
	final static int MAX_WIDTH = ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH;
	final TextComponent space = Component.text(" ").color(NamedTextColor.WHITE);

	public void addComponentOf(String name, String text) {
		components.put(name, Component.text(text));
	}

	public void addComponentOf(String name, TextComponent component) {
		components.put(name, component);
	}
	
	public void addComponentOf(String name, String text, ClickEvent click) {
		components.put(name, Component.text(text).clickEvent(click));
	}
	
	public void addComponentOf(String name, String text, HoverEvent hover) {
		components.put(name, Component.text(text).hoverEvent(hover));
	}
	
	public void addComponentOf(String name, String text, HoverEvent hover, ClickEvent click) {
		components.put(name, Component.text(text).hoverEvent(hover).clickEvent(click));
	}

	public void removeStatusComponent(String name) {
		components.remove(name);
	}
	
	public Collection<TextComponent> getComponents() {
		return Collections.unmodifiableCollection(components.values());
	}
	
	public boolean hasComponent(String name) {
		return components.containsKey(name);
	}
	
	public TextComponent getComponentOrNull(String name) {
		return components.get(name);
	}
	
	public boolean replaceComponent(String name, TextComponent component) {
		return components.replace(name, component) != null;
	}
	
	public List<TextComponent> getFormattedStatusScreen() {
		List<TextComponent> lines = new ArrayList<>();
		TextComponent currentLine = Component.empty();
		List<TextComponent> components = new ArrayList<>(this.components.values());
		String string = "";
		
		// Cycle over all components in the status screen.
		for (int i = 0; i < components.size(); i++) {
			TextComponent nextComp = components.get(i);
			if (currentLine.content().isEmpty() && currentLine.children().isEmpty()) {
				// We're dealing with a new line, our component becomes the start of a line.
				currentLine = nextComp;
				string = currentLine.content();
				continue;
			}
			if (nextComp.equals(Component.newline())) {
				// We're dealing with a component which is just a new line, make a new line.
				lines.add(currentLine);
				currentLine = Component.empty();
				string = "";
				continue;
			}
			if (!nextComp.children().isEmpty()) {
				// We're dealing with a component made of children, probably the ExtraFields or AdditionalLines.
				// Because there is no easy way of dealing with children of a Component, we dump currentLine and 
				// then dump in child.
				lines.add(currentLine);
				currentLine = Component.empty();
				string = "";
				for (Component child : nextComp.children())
					lines.add(Component.empty().append(child));
				continue;
			}
			// We're dealing with a Component that is has no children.
			if ((Colors.strip(string).length() + nextComp.content().length() + 1) > MAX_WIDTH) {
				// We've found a component which will make the line too long,
				// Dump currentLine into lines and start over with nextComp starting the new line.
				lines.add(currentLine);
				currentLine = nextComp;
				string = currentLine.content();
				continue;
			}
			// We have a component that will fit onto the current line.
			currentLine = currentLine.append(space).append(nextComp);
			string += " " + nextComp.content();
		}
		
		// The loop is done, if anything was left in currentLine dump it into lines.
		if (!currentLine.content().isEmpty())
			lines.add(currentLine);
		
		return lines;
	}
}
