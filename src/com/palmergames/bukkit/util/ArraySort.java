package com.palmergames.bukkit.util;

import java.util.Comparator;

import org.bukkit.block.Block;

/**
 * @author ElgarL
 * @deprecated Deprecated as of 0.98.3.13, please use {@code Comparator.comparingInt(Block::getY())} instead.
 */
@Deprecated
public class ArraySort implements Comparator<Block> {

	@Override
	public int compare(Block blockA, Block blockB) {

		return blockA.getY() - blockB.getY();
	}

	private static ArraySort instance;

	public static ArraySort getInstance() {

		if (instance == null) {
			instance = new ArraySort();
		}
		return instance;
	}
}
