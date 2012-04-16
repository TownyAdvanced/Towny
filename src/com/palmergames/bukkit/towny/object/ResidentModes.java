package com.palmergames.bukkit.towny.object;

import java.util.List;

/**
 * 
 * @author ElgarL
 *
 */
public interface ResidentModes {

	public List<String> getModes();
	
	public boolean hasMode(String mode);
	
	public void toggleMode(String newModes[], boolean notify);
	
	public void setModes(String modes[], boolean notify);
	
	public void clearModes();
	

}
