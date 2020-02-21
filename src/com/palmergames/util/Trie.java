package com.palmergames.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author stzups
 */
public class Trie {
	
	public static class TrieNode {
		Map<Character, TrieNode> children = new HashMap<>();
		char character;
		boolean endOfWord = false;
		
		TrieNode(char character) {
			this.character = character;
		}
	}
	
	private final TrieNode root;
	
	public Trie() {
		root = new TrieNode(Character.MIN_VALUE);
	}
	
	public void addKey(String key) {
		
		TrieNode trieNode = root;
		
		for (int i = 0; i < key.length(); i++) {
			char index = Character.toLowerCase(key.charAt(i));
			TrieNode lastNode = trieNode;
			trieNode = lastNode.children.get(index);
			if (trieNode == null)  {
				lastNode.children.put(index, new TrieNode(index));
				trieNode = lastNode.children.get(index);
			}
			if (i == key.length()-1) {
				trieNode.endOfWord = true;
				return;
			}
		}

	}
	
	public List<TrieNode> getTrieNodeLeavesFromKey(String key) {
		
		List<TrieNode> trieNodeLeaves = new ArrayList<>();//todo linkedlist?
		
		if (key.length() == 0){
			return getChildren(root, new ArrayList<>());
		} else {
			TrieNode trieNode = root;
			for (int i = 0; i < key.length(); i++) {
				TrieNode lastNode = trieNode;
				trieNode = lastNode.children.get(Character.toLowerCase(key.charAt(i)));
				//System.out.println(trieNode);
				if (trieNode == null) {
					break;
				}
				if (trieNode.endOfWord) {
					trieNodeLeaves.add(trieNode);
				} else if (i == key.length() - 1) {
					trieNodeLeaves.addAll(getChildren(trieNode, new ArrayList<>()));
				}
			}
		}
		//System.out.println(trieNodeLeaves.size());
		return trieNodeLeaves;
	}
	
	private static List<TrieNode> getChildren(TrieNode find, List<TrieNode> found) {
		//System.out.println("Searching for children from "+find+", found "+found);
		for (TrieNode trieNode:find.children.values()) {
			if (!trieNode.endOfWord && trieNode.children.size() > 0) {
				//System.out.println("not the end of the word, searching "+trieNode+", found is "+found.size());
				found = getChildren(trieNode, found);
				//System.out.println("now found is "+found.size());
			} else {
				//System.out.println("end of word, adding "+trieNode);
				found.add(trieNode);
			}
		}
		//System.out.println("retuing "+found.size());
		return found;
	}


	public List<String> getStringsFromKey(String key) {

		List<String> strings = new ArrayList<>();//todo linkedlist?

		if (key.length() == 0){
			strings.addAll(getChildrenStrings(root, new ArrayList<>()));
		} else {
			TrieNode trieNode = root;
			for (int i = 0; i < key.length(); i++) {
				TrieNode lastNode = trieNode;
				char index = key.charAt(i);
				trieNode = lastNode.children.get(Character.toLowerCase(index));
				//System.out.println(trieNode);
				if (trieNode == null) {
					break;
				}
				if (i == key.length() - 1) {
					for (String string:getChildrenStrings(trieNode, new ArrayList<>())){
						strings.add(key+string);
					}
				}
			}
		}
		//System.out.println(trieNodeLeaves.size());
		return strings;
	}
	
	private List<String> getChildrenStrings(TrieNode find, List<String> found) {
		//System.out.println("find: "+find.character+", found: "+ String.join("", found));
		for (TrieNode trieNode:find.children.values()) {
			if(!trieNode.endOfWord) {
				for (String string : getChildrenStrings(trieNode, new ArrayList<>())) {
					//System.out.println("adding " + trieNode.character + "::" + string);
					found.add(trieNode.character + string);
				}
			} else {
				found.add(String.valueOf(trieNode.character));
			}
		}
		//System.out.println("returning "+String.join("", found));
		return found;
	}
}
