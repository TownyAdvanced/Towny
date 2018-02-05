package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Random;

public abstract class TownyObject extends Observable {

	private Integer UID = 0;
	private String name;
	private boolean isChangedName = true;

	public void setName(String name) {

		if (getUID() == 0)
			setUID(name.hashCode() + new Random(System.currentTimeMillis()).nextInt());

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

		char[] fill = new char[depth * 4];
		Arrays.fill(fill, ' ');
		if (depth > 0) {
			fill[0] = '|';
			int offset = (depth - 1) * 4;
			fill[offset] = '+';
			fill[offset + 1] = '-';
			fill[offset + 2] = '-';
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

	/**
	 * Returns the current objects UID
	 * 
	 * @return integer containing the objects unique identifier
	 */
	public Integer getUID() {

		return UID;
	}

	/**
	 * A function to set the objects UID.
	 * This should never be used unless duplicating an object.
	 * 
	 * @param uID - new Object's UID
	 */
	public void setUID(Integer uID) {

		UID = uID;
	}
}
