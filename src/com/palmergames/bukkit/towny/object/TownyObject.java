package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class TownyObject {
	private String name;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public List<String> getTreeString(int depth) {
		return new ArrayList<String>();
	}
	
	public String getTreeDepth(int depth) {
		char[] fill = new char[depth*4];
		Arrays.fill(fill, ' ');
		if (depth > 0) {
			fill[0] = '|';
			int offset = (depth-1)*4;
			fill[offset] = '+';
			fill[offset+1] = '-';
			fill[offset+2] = '-';
		}
		return new String(fill);
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
