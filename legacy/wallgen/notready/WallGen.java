package com.palmergames.bukkit.wallgen;

import com.palmergames.bukkit.util.BlockQueue;

public class WallGen {
	public static final int topSection = 4; //How many layers the top part of the wall is.

	/*
		WallBlock type:
			0 = Wall
			1 = Turret wall (3 enemy sides)
			2 = Turret wall (1 enemy sides)
	*/
	
	public static boolean deleteTownWall(TownyWorld world, Town town) {
		BlockQueue blockQueue = BlockQueue.getInstance();
		//Delete old wall
		//ArrayList<TownBlock> townblocks = world.getTownBlocks(town);
		for (WallSection wb : town.wall.sections) {
			for (int z = 0; z < 4; z++) {
				//for (int y = 0; y < town.wall.walkwayHeight+topSection-(int)wb.p.y; y++) {
				for (int y = 0; y < town.wall.height+topSection; y++) {
					for (int x = 0; x < 4; x++) {
						blockQueue.addWork(new Block(0, (int)wb.p.x+x, (int)wb.p.y+y, (int)wb.p.z+z));
					}
				}
			}
		}
		town.wall.sections.clear();
		
		return true;
	}
	
	public static boolean townGen(TownyWorld world, Town town) {
		ArrayList<TownBlock> townblocks = world.getTownBlocks(town);
		
		// Assign Turrets
		for (TownBlock tb : townblocks) {
			if (!isPartOfTown(tb.x-1, tb.z, town)) {
				for (int z = 0; z < TownyProperties.blockSize; z += 4)
					town.wall.sections.add(new WallSection(lowestInGrid(tb.x*TownyProperties.blockSize, z+tb.z*TownyProperties.blockSize, 4, 4), 0, 0));	
			}
			if (!isPartOfTown(tb.x, tb.z-1, town)) {
				for (int x = 0; x < TownyProperties.blockSize; x += 4)
					town.wall.sections.add(new WallSection(lowestInGrid(x+tb.x*TownyProperties.blockSize, tb.z*TownyProperties.blockSize, 4, 4), 3, 0));
			}
			if (!isPartOfTown(tb.x+1, tb.z, town)) {
				for (int z = 0; z < TownyProperties.blockSize; z += 4)
					town.wall.sections.add(new WallSection(lowestInGrid(tb.x*TownyProperties.blockSize+(TownyProperties.blockSize-4), z+tb.z*TownyProperties.blockSize, 4, 4), 2, 0));
			}
			if (!isPartOfTown(tb.x, tb.z+1, town)) {
				for (int x = 0; x < TownyProperties.blockSize; x += 4)
					town.wall.sections.add(new WallSection(lowestInGrid(x+tb.x*TownyProperties.blockSize, tb.z*TownyProperties.blockSize+(TownyProperties.blockSize-4), 4, 4), 1, 0));
			}
			
			// x = current townblock, f = town, e = else
			
			// ef    ee
			// fx or ex
			if (!isPartOfTown(tb.x-1, tb.z+1, town) &&
				(isPartOfTown(tb.x-1, tb.z, town) && isPartOfTown(tb.x, tb.z+1, town)) ||
				(!isPartOfTown(tb.x-1, tb.z, town) && !isPartOfTown(tb.x, tb.z+1, town)) )
				town.wall.sections.add(new WallSection(lowestInGrid(tb.x*TownyProperties.blockSize, tb.z*TownyProperties.blockSize+(TownyProperties.blockSize-4), 4, 4), 1, (isPartOfTown(tb.x-1, tb.z, town) && isPartOfTown(tb.x, tb.z+1, town)) ? 2 : 1));
			// fe    ee
			// xf or xe
			if (!isPartOfTown(tb.x+1, tb.z+1, town) &&
				(isPartOfTown(tb.x+1, tb.z, town) && isPartOfTown(tb.x, tb.z+1, town)) ||	
				(!isPartOfTown(tb.x+1, tb.z, town) && !isPartOfTown(tb.x, tb.z+1, town)) )
				town.wall.sections.add(new WallSection(lowestInGrid(tb.x*TownyProperties.blockSize+(TownyProperties.blockSize-4), tb.z*TownyProperties.blockSize+(TownyProperties.blockSize-4), 4, 4), 2, (isPartOfTown(tb.x+1, tb.z, town) && isPartOfTown(tb.x, tb.z+1, town)) ? 2 : 1));
			// xf    xe
			// fe or ee
			if (!isPartOfTown(tb.x+1, tb.z-1, town) &&
				(isPartOfTown(tb.x+1, tb.z, town) && isPartOfTown(tb.x, tb.z-1, town)) ||	
				(!isPartOfTown(tb.x+1, tb.z, town) && !isPartOfTown(tb.x, tb.z-1, town)) )
				town.wall.sections.add(new WallSection(lowestInGrid(tb.x*TownyProperties.blockSize+(TownyProperties.blockSize-4), tb.z*TownyProperties.blockSize, 4, 4), 3, (isPartOfTown(tb.x+1, tb.z, town) && isPartOfTown(tb.x, tb.z-1, town)) ? 2 : 1));
			// fx    ex
			// ef or ee
			if (!isPartOfTown(tb.x-1, tb.z-1, town) &&
				(isPartOfTown(tb.x-1, tb.z, town) && isPartOfTown(tb.x, tb.z-1, town)) ||
				(!isPartOfTown(tb.x-1, tb.z, town) && !isPartOfTown(tb.x, tb.z-1, town)) )
				town.wall.sections.add(new WallSection(lowestInGrid(tb.x*TownyProperties.blockSize, tb.z*TownyProperties.blockSize, 4, 4), 0, (isPartOfTown(tb.x-1, tb.z, town) && isPartOfTown(tb.x, tb.z-1, town)) ? 2 : 1));
			
		}
		
		/*
		for (WallBlock wb : town.wall.sections) {
			if ((int)wb.p.y > town.wall.walkwayHeight)
				town.wall.walkwayHeight = (int)wb.p.y+town.wall.height+3;
		}
		*/
				
		for (WallSection wb : town.wall.sections)
			drawWallBlock(town.wall, wb);
			
		return true;
	}
	
	public static void drawWallBlock(Wall wall, WallSection wb) {
		BlockQueue blockQueue = BlockQueue.getInstance();
		// Even wall height
		//int[][][] arr = getBuildArray(wall.blockType, wall.walkwayHeight-(int)wb.p.y, wb.t, wb.r);
		
		int[][][] arr = getBuildArray(wall.blockType, wall.height, wb.t, wb.r);
		ArrayList<Block> addAfter = new ArrayList<Block>();
		
		for (int z = 0; z < 4; z++) {
			for (int y = 0; y < arr.length; y++) { 
				for (int x = 0; x < 4; x++) {
					int bid = arr[y][x][z];

					if (bid == 65) {
						int ladderRot = 2;
						if (wb.r == 0) ladderRot = 3;
						else if (wb.r == 1) ladderRot = 4;
						else if (wb.r == 2) ladderRot = 2;
						else if (wb.r == 3) ladderRot = 5;
						addAfter.add(new Block(bid, (int)wb.p.x+x, (int)wb.p.y+y, (int)wb.p.z+z, ladderRot));
					} else {
						blockQueue.addWork(new Block(bid, (int)wb.p.x+x, (int)wb.p.y+y, (int)wb.p.z+z));
					}
				}
			}
		}
		
		for (Block block : addAfter)
			blockQueue.addWork(block);
	}
	
	public static Point lowestInGrid(long x, long z, int w, int h) {
		int y;
		int lowest = 127;
		for (long iz = z; iz < z+h; iz++) {
			for (long ix = x; ix < x+w; ix++) {
				y = etc.getServer().getHighestBlockY((int)ix, (int)iz);
				if (y < lowest)
					lowest = y;
			}
		}
		return new Point(x, lowest, z);
	}
	
	public static boolean isPartOfTown(long x, long z, Town town) {
		String key = x + "," + z;
		TownBlock tb = TownyWorld.getInstance().townblocks.get(key);
		if (tb != null && tb.town != null && tb.town == town)
			return true;
		return false;
	}
	
	public static int[][][] getBuildArray(int b, int h, int type, int rotation) {
		int s = 65; // Ladder
		
		int[][][] buildArray = {
			// Wall section
			{
				{ 0, 0, 0, 0 },
				{ b, b, b, b },
				{ b, b, b, b },
				{ 0, 0, 0, 0 }
			},
			// Wall walkway
			{
				{ b, b, b, b },
				{ b, b, b, b },
				{ b, b, b, b },
				{ b, b, b, b }
			},
			// Wall section walkway wall
			{
				{ b, b, b, b },
				{ 0, 0, 0, 0 },
				{ 0, 0, 0, 0 },
				{ b, b, b, b }
			},
			// Wall section walkway wall crenelation
			{
				{ 44, 0, 0, 44 },
				{ 0, 0, 0, 0 },
				{ 0, 0, 0, 0 },
				{ 44, 0, 0, 44 }
			},
			// Turret wall (3 enemy sides)
			{
				{ 0, 0, 0, 0 },
				{ 0, b, b, b },
				{ 0, b, s, 0 },
				{ 0, b, 0, 0 }
			},
			// Turret wall (1 enemy side)
			{
				{ 0, b, b, 0 },
				{ b, b, b, 0 },
				{ b, b, s, 0 },
				{ 0, 0, 0, 0 }
			},
			// Turret walkway
			{
				{ b, b, b, b },
				{ b, b, b, b },
				{ b, b, s, b },
				{ b, b, b, b }
			},
			// Turret wall (3 enemies) (is rotated twice to match (1 enemy)
			{
				{ b, b, b, b },
				{ b, 0, 0, 0 },
				{ b, 0, 0, 0 },
				{ b, 0, 0, b }
			},
			// Wall section Afv. crenels
			{
				{ b, 0, 0, b },
				{ b, b, b, b },
				{ b, b, b, b },
				{ 0, 0, 0, 0 }
			},
		};
		
		ArrayList<int[][]> out = new ArrayList<int[][]>();
		int[][] layer;
		// Walkway
		if (type == 0) {
			layer = rotate(buildArray[0], rotation);
			for (int i = 0; i < h; i++)
				out.add(layer);
				
			// Top Section
			out.add(rotate(buildArray[8], rotation));
			out.add(buildArray[1]); //No need to rotate the walkway
			out.add(rotate(buildArray[2], rotation));
			out.add(rotate(buildArray[3], rotation));
		}
		// Turret wall (3 enemy sides)
		else if (type == 1) {
			layer = rotate(buildArray[4], rotation);
			for (int i = 0; i < h; i++)
				out.add(layer);
				
			// Top Section
			layer = rotate(buildArray[6], rotation);
			for (int i=0;i<2;i++)
				out.add(layer);
			out.add(rotate(buildArray[7], rotation));
			out.add(rotate(buildArray[3], rotation));
		}
		// Turret wall (1 enemy sides)
		else if (type == 2) {
			layer = rotate(buildArray[5], rotation);
			for (int i = 0; i < h; i++)
				out.add(layer);
				
			// Top Section
			layer = rotate(buildArray[6], rotation);
			for (int i=0;i<2;i++)
				out.add(layer);
			out.add(rotate(buildArray[7], rotation+2)); //rotate wall to fit
			out.add(rotate(buildArray[3], rotation));
		}
		
		int[][][] toReturn = new int[out.size()][4][4];
		for (int z = 0; z < 4; z++) {
			for (int y = 0; y < out.size(); y++) {
				for (int x = 0; x < 4; x++) {
					toReturn[y][x][z] = out.get(y)[x][z];
				}
			}
		}
		
		return toReturn;
		//return (int[][][])out.toArray();
	}
	
	public static int[][] rotate(int[][] arr, int times) {
		int[][] temp = arr.clone();
		for (int i = 0; i < times; i++)
			temp = rotateMatrixRight(temp);
		return temp;
	}
	
	// http://stackoverflow.com/questions/42519/how-do-you-rotate-a-two-dimensional-array
	public static int[][] rotateMatrixRight(int[][] matrix) {
		/* W and H are already swapped */
		int w = matrix.length;
		int h = matrix[0].length;
		int[][] ret = new int[h][w];
		for (int i = 0; i < h; ++i) {
			for (int j = 0; j < w; ++j) {
				ret[i][j] = matrix[w - j - 1][i];
			}
		}
		return ret;
	}
}
