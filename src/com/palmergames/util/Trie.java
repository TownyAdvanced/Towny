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
		boolean endOfWord = false;
		
		TrieNode() {
		}
		
		public TrieNode(char child) {
			children.put(child, new TrieNode());
		}
	}
	
	private final TrieNode root;
	
	public Trie() {
		root = new TrieNode();
	}
	
	public TrieNode addKey(String key) {
		
		TrieNode trieNode = root;
		
		for (int i = 0; i < key.length(); i++) {
			char index = Character.toLowerCase(key.charAt(i));
			TrieNode lastNode = trieNode;
			trieNode = lastNode.children.get(index);
			if (trieNode == null)  {
				lastNode.children.put(index, new TrieNode());
				trieNode = lastNode.children.get(index);
			}
			if (i == key.length()-1) {
				trieNode.endOfWord = true;
				return trieNode;
			}
		}
		
		return null;
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
}
