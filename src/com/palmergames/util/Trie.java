package com.palmergames.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic trie structure that can add new keys and recursively get matching strings for a key 
 * 
 * @author stzups
 */
public class Trie {

	/**
	 * TrieNode implementation that handles any character and keeps track of its own children and character
	 */
	public static class TrieNode {
		Map<Character, TrieNode> children = new HashMap<>();
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
			char index = Character.toLowerCase(key.charAt(i)); // Case insensitive

			TrieNode lastNode = trieNode;
			trieNode = lastNode.children.get(index);

			if (trieNode == null) { // No existing TrieNode here, so make a new one
				trieNode = new TrieNode(index);
				lastNode.children.put(index, trieNode); // Put this node as one of lastNode's children
			}

			if (i == key.length()-1) { // Check if this is the last character of the key, indicating a word ending
				trieNode.endOfWord = true;
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

		List<String> strings = new ArrayList<>();

		if (key.length() == 0){ // Find all nodes starting from the root node
			strings.addAll(getChildrenStrings(root, new ArrayList<>()));
		} else {
			TrieNode trieNode = root;

			for (int i = 0; i < key.length(); i++) {
				trieNode = trieNode.children.get(Character.toLowerCase(key.charAt(i))); // Case insensitive

				if (trieNode == null) { // No existing TrieNode here, stop searching
					break;
				}

				if (i == key.length() - 1) { // Check if this is the last character of the key, indicating a word ending. From here we need to find all the possible children
					for (String string : getChildrenStrings(trieNode, new ArrayList<>())){ // Recursively find all children
						strings.add(key+string); // Add the key to the front of each child string
					}
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

		for (TrieNode trieNode:find.children.values()) { // Loop through each child
			if(!trieNode.endOfWord) { // Not the end of the word, so loop through all children
				for (String string : getChildrenStrings(trieNode, new ArrayList<>())) { // Recursively find all children
					found.add(trieNode.character + string); // Add this TrieNode's character to the front of each string
				}
			} else { // End of word, so just add this TrieNode's character
				found.add(String.valueOf(trieNode.character));
			}
		}

		return found;
	}
}
