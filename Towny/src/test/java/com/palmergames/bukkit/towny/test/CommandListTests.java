package com.palmergames.bukkit.towny.test;

import com.palmergames.bukkit.towny.object.CommandList;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class CommandListTests {
	
	@Test
	void testSubCommands() {
		CommandList list = new CommandList(Arrays.asList("/town spawn"));
		
		assertTrue(list.containsCommand("town spawn"));
		assertTrue(list.containsCommand("/towny:town spawn"));
		assertTrue(list.containsCommand("towny:town spawn"));
		assertTrue(list.containsCommand("/town spawn"));
		
		assertTrue(list.containsCommand("town spawn abc"));

		assertFalse(list.containsCommand("town"));
		assertFalse(list.containsCommand("town list"));
		assertFalse(list.containsCommand("/towny:town list"));
	}
	
	@Test
	void testNamespacedCommands() {
		CommandList list = new CommandList(Arrays.asList("/towny"));
		
		assertTrue(list.containsCommand("/plugin1:towny"));
		assertTrue(list.containsCommand("/plugin2:towny"));
		assertTrue(list.containsCommand("plugin3:towny"));
	}
	
	@Test
	void testLeadingSlashes() {
		CommandList list1 = new CommandList(Arrays.asList("/towny"));
		
		assertTrue(list1.containsCommand("towny"));
		assertTrue(list1.containsCommand("/towny"));
		
		CommandList list2 = new CommandList(Arrays.asList("towny"));
		assertTrue(list2.containsCommand("towny"));
		assertTrue(list2.containsCommand("/towny"));
	}
	
	@Test
	void testMultipleCommands() {
		CommandList list = new CommandList(Arrays.asList("/towny", "/warp", "/home"));
		
		assertTrue(list.containsCommand("/towny universe"));
		assertTrue(list.containsCommand("/warp"));
		
		assertTrue(list.containsCommand("/home 1"));
		assertTrue(list.containsCommand("home 2"));

		assertTrue(list.containsCommand("/warp spawn"));
		list.addCommand("/warp spawn");
		assertTrue(list.containsCommand("/warp spawn"));
	}
}
