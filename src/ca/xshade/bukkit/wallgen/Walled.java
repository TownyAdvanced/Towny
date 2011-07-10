package ca.xshade.bukkit.wallgen;

import java.util.List;

public interface Walled {
	public Wall getWall();
	
	public List<WallSection> getWallSections();
	
	public void setWallSections(List<WallSection> wallSections);
	
	public boolean hasWallSection(WallSection wallSection);

	public void addWallSection(WallSection wallSection);

	public void removeWallSection(WallSection wallSection);
}
