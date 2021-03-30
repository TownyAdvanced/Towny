package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.map.MinecraftFont;

import net.md_5.bungee.api.ChatColor;

/**
 * @author LlmDl
 */
public class BookFactory {

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
		BookMeta meta = (BookMeta) book.getItemMeta();
		meta.setTitle(title);
		meta.setAuthor(author);
		List<String> pages = getPages(rawText);
		for (String page : pages)
			meta.addPage(page);
		book.setItemMeta(meta);
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
		BookMeta meta = (BookMeta) book.getItemMeta();
		meta.setTitle(title);
		meta.setAuthor(author);
		for (String page : pages)
			meta.addPage(page);
		book.setItemMeta(meta);
		return book;
	}

	/**
	 * A method to feed raw text out of which lines for pages.
	 * 
	 * Heavily inspired by
	 * https://www.spigotmc.org/threads/book-multipage-wrapping-text.383001/#post-3474541
	 * 
	 * @param rawText
	 * @return lines as a List of Strings.
	 */
	private static List<String> getLines(String rawText) {
		// Note that the only flaw with using MinecraftFont is that it can't account for
		// some UTF-8 symbols, it will throw an IllegalArgumentException
		final MinecraftFont font = new MinecraftFont();
		final int maxLineWidth = font.getWidth("LLLLLLLLLLLLLLLLLLL"); // 113 pixels.

		// An arraylist to store all of the individual lines which are made to fit a
		// book's line width.
		List<String> lines = new ArrayList<>();

		try {
			// Each 'section' is separated by a line break (\n)
			for (String section : rawText.split("\n")) {
				// If the section is blank, that means we had a double line break there
				if (section.equals(""))
					lines.add("\n");
				// We have an actual section with some content
				else {
					// Iterate through all the words of the section
					String[] words = ChatColor.stripColor(section).split(" ");
					String line = "";
					for (int index = 0; index < words.length; index++) {
						String word = words[index];
						// New lines get the first word added. If the word is
						// too long it will be added to lines on the next loop.
						if (line.isEmpty()) {
							line = word;
							continue;
						}

						/*
						 * Required because Bukkit builds older than Nov 3 2020 (MC 1.16.3)
						 * believe a space is only 2 pixels wide while it is in fact 3 pixels wide.
						 */
						int spaces = 0; // Number of pixels to add to the line length test later on.
						if (font.getWidth(" ") == 2) {
							spaces = 1; // Because one space will be added in the test.
							for (int i = 0; i < line.length(); ++i)
								if (line.charAt(i) == ' ')
									spaces++;
						}

						// Current line + word is too long to be one line
						if (font.getWidth(line + " " + word) + spaces > maxLineWidth) {
							// Add our current line
							lines.add(line + "\n");
							// Set our next line to start off with this word
							line = word;
							continue;
						}
						// Add the current word to our current line
						line += " " + word;
					}
					// Make sure we add the line if it was the last word and
					// wasn't too long for the line to start a new one
					if (!line.equals("")) {
						lines.add(line + "\n");
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
	private static List<String> getPages(String rawText) {
		// Adding an empty line to take the place of the lines[0] which we will be skipping later on.
		rawText = "\n" + rawText; 
		List<String> pages = new ArrayList<String>();
		List<String> lines = getLines(rawText);
		String pageText = "";
		for (int i = 1; i < lines.size(); i++) {
			pageText += lines.get(i);
			// Dump every 14 lines into the pages Array (a MC book page can hold 14 lines.)
			if (i != 1 && i % 14 == 0) {
				pages.add(pageText);
				pageText = "";
			}
		}
		if (!pageText.isEmpty())
			pages.add(pageText);
		return pages;
	}
}
