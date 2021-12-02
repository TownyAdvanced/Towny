package com.palmergames.bukkit.towny.object.statusscreens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.util.ChatPaginator;

import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;


public class StatusScreen {

	public StatusScreen() {}

	Map<String, Component> components = new LinkedHashMap<>();
	final static int MAX_WIDTH = ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH;

	public void addComponentOf(String name, String text) {
		components.put(name, Component.text(text));
	}

	public void addComponentOf(String name, Component component) {
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
	
	public Collection<Component> getComponents() {
		return Collections.unmodifiableCollection(components.values());
	}
	
	public boolean hasComponent(String name) {
		return components.containsKey(name);
	}
	
	public Component getComponentOrNull(String name) {
		return components.get(name);
	}
	
	public boolean replaceComponent(String name, TextComponent component) {
		return components.replace(name, component) != null;
	}
	
	public List<Component> getFormattedStatusScreen() {
		List<Component> lines = new ArrayList<>();
		Component currentLine = Component.empty();
		List<Component> components = new ArrayList<>(this.components.values());
		String string = "";
		
		// Cycle over all components in the status screen.
		for (int i = 0; i < components.size(); i++) {
			Component nextComp = components.get(i);
			if (nextComp.equals(Component.newline()) && nextComp.children().isEmpty()) { 
				// We're dealing with a component which is just a new line, make a new line.
				lines.add(currentLine);
				currentLine = Component.empty();
				string = "";
				continue;
			}
			if (currentLine.equals(Component.empty()) && nextComp.children().isEmpty()) {
				// We're dealing with a new line and the nextComp has no children to process,
				// nextComp becomes the start of a line.
				currentLine = nextComp;
				string = getContent(currentLine);
				continue;
			}
			// We're dealing with a component made of children, probably the ExtraFields or AdditionalLines.
			if (!nextComp.children().isEmpty()) {
				// nextComp starts with a new line component with children to follow, start a new line.
				if (nextComp.equals(Component.newline())) {
					lines.add(currentLine);
					currentLine = Component.empty();
					string = "";
				}
				// Cycle over all child components.
				for (Component child : nextComp.children()) {
					if (child.equals(Component.newline())) {
						// We're dealing with a child component which is just a new line, make a new line.
						lines.add(currentLine);
						currentLine = Component.empty();
						string = "";
						continue;
					}
					if (currentLine.equals(Component.empty())) {
						// We're dealing with a new line, our child component becomes tShe start of a line.
						currentLine = child;
						string = getContent(currentLine);
						continue;
					}
					if (lineWouldBeTooLong(string, child)) {
						// We've found a child which will make the line too long,
						// Dump currentLine into lines and start over with nextChild starting the new line.
						lines.add(currentLine);
						currentLine = child;
						string = getContent(currentLine);
						continue;
					}
					// We have a child which will fit onto the current line.
					currentLine = currentLine.append(Component.space().append(child));
					string += " " + getContent(child);
				}
				// The loop is done, if anything was left in the currentLine dump it into lines.
				if (!currentLine.equals(Component.empty())) {
					lines.add(currentLine);
					currentLine = Component.empty();
					string = "";
				}
				continue;
			}
			// We're dealing with a Component that has no children.
			if (lineWouldBeTooLong(string, nextComp)) {
				// We've found a component which will make the line too long,
				// Dump currentLine into lines and start over with nextComp starting the new line.
				lines.add(currentLine);
				currentLine = nextComp;
				string = getContent(currentLine);
				continue;
			}
			// We have a component that will fit onto the current line.
			currentLine = currentLine.append(Component.space().append(nextComp));
			string += " " + getContent(nextComp);
		}
		
		// The loop is done, if anything was left in currentLine dump it into lines.
		if (!currentLine.equals(Component.empty()))
			lines.add(currentLine);

		return lines;
	}
	
	private String getContent(Component comp) {
		if (comp.children().isEmpty())
			return ((TextComponent) comp).content();
		List<String> content = new ArrayList<>();
		if (!comp.children().isEmpty())
			for (Component child : comp.children())
				content.add(((TextComponent) child).content());
		return StringMgmt.join(content, " ");
	}

	private boolean lineWouldBeTooLong(String string, Component comp) {
		return (Colors.strip(string).length() + Colors.strip(getContent(comp)).length() + 1) > MAX_WIDTH;
	}
}
