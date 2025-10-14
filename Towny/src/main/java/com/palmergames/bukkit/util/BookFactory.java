package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * @author LlmDl
 */
public class BookFactory {
	
	private static final float MAX_LINE_WIDTH = FontUtil.measureWidth("LLLLLLLLLLLLLLLLLLL"); // 113 pixels.

	/**
	 * Returns a book itemstack with the given title, author and rawText.
	 * 
	 * @param title   Title of the book.
	 * @param author  Author of the book.
	 * @param rawText The unprocessed text to be converted into book-friendly
	 *                lines/pages.
	 * @return book ItemStack suitable for giving directly to a player inventory.
	 */
	public static ItemStack makeBook(String title, String author, String rawText) {
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		book.editMeta(BookMeta.class, meta -> {
			meta.setTitle(title);
			meta.setAuthor(author);

			List<Component> pages = getPages(rawText);
			for (Component page : pages)
				meta.addPages(page);
		});
		return book;
	}
	
	/**
	 * Returns a book itemstack with the given title, author and pages.
	 * 
	 * @param title   Title of the book.
	 * @param author  Author of the book.
	 * @param pages List of Strings, each string intended to become one page in a book. 
	 * @return book ItemStack suitable for giving directly to a player inventory or opening using Player#openBook().
	 */
	public static ItemStack makeBook(String title, String author, List<String> pages) {
		
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		book.editMeta(BookMeta.class, meta -> {
			meta.setTitle(title);
			meta.setAuthor(author);

			for (String page : pages)
				meta.addPages(TownyComponents.miniMessage(page));
		});

		return book;
	}

	/**
	 * A method to feed raw text out of which lines for pages.
	 * <p> 
	 * Heavily inspired by
	 * <a href="https://www.spigotmc.org/threads/book-multipage-wrapping-text.383001/#post-3474541">https://www.spigotmc.org/threads/book-multipage-wrapping-text.383001/#post-3474541</a>
	 * 
	 * @param rawText The raw text
	 * @return lines as a List of Strings.
	 */
	private static List<Component> getLines(String rawText) {
		// An arraylist to store all of the individual lines which are made to fit a
		// book's line width.
		List<Component> lines = new ArrayList<>();

		try {
			// Each 'section' is separated by a line break (\n)
			for (String section : rawText.split("\n")) {
				// If the section is blank, that means we had a double line break there
				if (section.isEmpty())
					lines.add(Component.newline());
				// We have an actual section with some content
				else {
					// Iterate through all the words of the section
					String[] words = Colors.strip(section).split(" ");
					Component line = Component.empty();
					for (int index = 0; index < words.length; index++) {
						Component word = Component.text(words[index]);
						// New lines get the first word added. If the word is
						// too long it will be added to lines on the next loop.
						if (line.equals(Component.empty())) {
							line = word;
							continue;
						}

						// Current line + word is too long to be one line
						if (FontUtil.measureWidth(line.appendSpace().append(word)) > MAX_LINE_WIDTH) {
							// Add our current line
							lines.add(line.appendNewline());
							// Set our next line to start off with this word
							line = word;
							continue;
						}
						// Add the current word to our current line
						line = line.appendSpace().append(word);
					}
					// Make sure we add the line if it was the last word and
					// wasn't too long for the line to start a new one
					if (!line.equals(Component.empty())) {
						lines.add(line.appendNewline());
					}
				}
			}
		} catch (IllegalArgumentException ex) {
			lines.clear();
		}
		return lines;
	}

	/**
	 * A method to feed raw text out of which lines for pages and then pages are
	 * made.
	 * 
	 * @param rawText
	 * @return pages as a List of Strings.
	 */
	private static List<Component> getPages(String rawText) {
		// Adding an empty line to take the place of the lines[0] which we will be skipping later on.
		rawText = "\n" + rawText; 
		List<Component> pages = new ArrayList<>();
		List<Component> lines = getLines(rawText);
		TextComponent.Builder pageText = Component.text();
		for (int i = 1; i < lines.size(); i++) {
			pageText.append(lines.get(i));
			// Dump every 14 lines into the pages Array (a MC book page can hold 14 lines.)
			if (i != 1 && i % 14 == 0) {
				pages.add(pageText.build());
				pageText = Component.text();
			}
		}

		final Component finalPage = pageText.build();
		if (!finalPage.equals(Component.empty()))
			pages.add(finalPage);

		return pages;
	}
}
