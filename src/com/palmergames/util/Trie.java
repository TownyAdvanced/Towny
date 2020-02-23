package com.palmergames.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

/**
 * Dynamic trie structure that can add/remove keys and recursively get matching strings for a key 
 * 
 * @author stzups
 */
public class Trie {

	private static final int MAX_RETURNS = 100;
	/**
	 * TrieNode implementation that handles any character and keeps track of its own children and character
	 */
	public static class TrieNode {
		List<TrieNode> children = new ArrayList<>();
		char character;
		boolean endOfWord = false;

		TrieNode(char character) {
			this.character = character;
		}
	}

	private final TrieNode root;

	/**
	 * Constructor that creates a new trie with a null root
	 */
	public Trie() {
		root = new TrieNode(Character.MIN_VALUE);
	}

	/**
	 * Adds and links new TrieNodes to the trie for each character in the string
	 * 
	 * @param key key to add to trie, can be longer than one character
	 */
	public void addKey(String key) {
		// Current trieNode to crawl through
		TrieNode trieNode = root;

		// Loop through each character of key
		for (int i = 0; i < key.length(); i++) {
			char index = key.charAt(i);

			TrieNode lastNode = trieNode;
			Optional<TrieNode> optional = lastNode.children.stream()
				        .filter(e -> e.character == index).findFirst();

			if (!optional.isPresent()) { // No existing TrieNode here, so make a new one
				trieNode = new TrieNode(index);
				lastNode.children.add(trieNode); // Put this node as one of lastNode's children
			} else {
				trieNode = optional.get();
			}

			if (i == key.length()-1) { // Check if this is the last character of the key, indicating a word ending
				trieNode.endOfWord = true;
			}
		}
	}

	/**
	 * Removes TrieNodes for a key
	 * 
	 * @param key key to remove
	 */
	public void removeKey(String key) {
		// Current trieNode to crawl through
		TrieNode trieNode = root;
		Queue<TrieNode> found = Collections.asLifoQueue(new LinkedList<>());

		// Loop through each character of key
		for (int i = 0; i < key.length(); i++) {
			char index = key.charAt(i);

			TrieNode lastNode = trieNode;
			Optional<TrieNode> optional = lastNode.children.stream()
				        .filter(e -> e.character == index).findFirst();

			if (optional.isPresent()) {
				trieNode = optional.get();
				found.add(trieNode);
				if (i == key.length()-1) { // Check if this is the last character of the key, indicating a word ending
					foundLoop:
					for (TrieNode trieNode1 : found) {
						Iterator<TrieNode> iterator = trieNode1.children.iterator();
						while (iterator.hasNext()) {
							TrieNode child = iterator.next();
							if (found.contains(child) && child.children.size() < 2) { // Only remove if in found and there are one or no children
								iterator.remove();
							} else {
								break foundLoop;
							}
						}
					}
				}
			} else {
				break; // This shouldn't happen
			}
		}
	}

	/**
	 * Gets all matching strings and their children for a key
	 * 
	 * @param key string to search for in tree
	 * @return matching strings and their children
	 */
	public List<String> getStringsFromKey(String key) {
		// Empty key means find all nodes, starting from the root node
		if (key.length() == 0) {
			return getChildrenStrings(root, new ArrayList<>());
		}

		List<String> strings = new ArrayList<>();
		TrieNode trieNode = root;
		StringBuilder realKey = new StringBuilder(); // Used if the key is not the correct case

		for (int i = 0; i < key.length(); i++) {
			int finalI = i;
			Optional<TrieNode> optional = trieNode.children.stream()
				.filter(e -> Character.toLowerCase(e.character) == Character.toLowerCase(key.charAt(finalI))).findFirst(); // Find matches for lower and upper case

			if (!optional.isPresent()) { // No existing TrieNode here, stop searching
				break;
			}

			trieNode = optional.get();
			realKey.append(trieNode.character);
			if (i == key.length() - 1) { // Check if this is the last character of the key, indicating a word ending. From here we need to find all the possible children
				for (String string : getChildrenStrings(trieNode, new ArrayList<>())) { // Recursively find all children
					strings.add(realKey + string); // Add the key to the front of each child string
				}
			}
		}

		return strings;
	}

	/**
	 * Recursively find all children of a TrieNode, and add to a list of strings
	 * 
	 * @param find the current TrieNode to search through its own children
	 * @param found strings that have already been found
	 * @return strings of all children found, with this TrieNode's character in front of each string
	 */
	private static List<String> getChildrenStrings(TrieNode find, List<String> found) {
		for (TrieNode trieNode : find.children) { // Loop through each child
			if (found.size() + 1 > MAX_RETURNS) {
				return found;
			}

			if (!trieNode.endOfWord) { // Not the end of the word, so loop through all children
				for (String string : getChildrenStrings(trieNode, new ArrayList<>())) {
					if (found.size() + 1 > MAX_RETURNS) {
						return found;
					} else {
						found.add(trieNode.character + string);
					}
				}
			} else { // End of word, so just add this TrieNode's character
				found.add(String.valueOf(trieNode.character));
			}
		}

		return found;
	}
}
