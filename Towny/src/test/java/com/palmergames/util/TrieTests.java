package com.palmergames.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TrieTests {
	@Test
	void testInserts() {
		Trie trie = new Trie();
		trie.addKey("TestTown2");
		trie.addKey("TestTown");
		
		assertEquals(2, trie.getStringsFromKey("TestT").size());

		trie = new Trie();
		trie.addKey("TestTown");
		trie.addKey("TestTown2");
		
		// Regardless of insertion order, the strings returned should still be 2.
		assertEquals(2, trie.getStringsFromKey("TestT").size());
	}
	
	@Test
	void testNodeRemoval() {
		Trie trie = new Trie();
		trie.addKey("TestTown");
		trie.addKey("TestTown2");

		assertEquals(2, trie.getStringsFromKey("TestT").size());
		
		trie.removeKey("TestTown");
		assertEquals(1, trie.getStringsFromKey("TestT").size());
	}
}
