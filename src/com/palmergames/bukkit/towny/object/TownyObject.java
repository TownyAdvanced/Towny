package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;

import com.palmergames.bukkit.towny.TownyFormatter;

public abstract class TownyObject extends Observable {
	private String name;
	private boolean isChangedName = true;

	public void setName(String name) {
        setChanged();
        notifyObservers(TownyObservableType.OBJECT_NAME);
		this.name = name;
		setChangedName(true);
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
	
	public String getFormattedName() {
		return TownyFormatter.getFormattedName(this);
	}
	
	/**
	 * @return the isChangedName
	 */
	public boolean isChangedName() {
		return isChangedName;
	}

	/**
	 * @param isChangedName the isChangedName to set
	 */
	public void setChangedName(boolean isChangedName) {
		this.isChangedName = isChangedName;
	}
}
