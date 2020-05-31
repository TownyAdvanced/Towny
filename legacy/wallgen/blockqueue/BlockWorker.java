package com.palmergames.bukkit.blockqueue;

import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.util.BukkitTools;

public class BlockWorker implements Runnable {

	private BlockQueue blockQueue;
	private Server server;
	public static final Object NO_MORE_WORK = new Object();
	public static final Object END_JOB = new Object();

	private boolean running;

	private BlockJob currentJob;
	private int blocks, skipped;

	public BlockWorker(Server server, BlockQueue blockQueue) {

		this.blockQueue = blockQueue;
		this.setServer(server);
		setRunning(true);
	}

	public synchronized void setRunning(boolean running) {

		this.running = running;
	}

	@Override
	public void run() {

		blocks = 0;
		skipped = 0;

		try {
			while (running) {
				Object obj = blockQueue.getWork();

				if (obj == NO_MORE_WORK)
					break;

				if (obj == END_JOB)
					onJobFinish(currentJob);

				if (obj instanceof BlockWork) {
					try {
						buildBlock((BlockWork) obj);
					} catch (Exception e) {
						skipped++;
					}
					blocks++;
				}

				if (obj instanceof BlockJob) {
					currentJob = (BlockJob) obj;
					blocks = 0;
					skipped = 0;
				}
			}
		} catch (InterruptedException e) {
		}

		System.out.println("[Blocker] BlockQueue Thread stopped.");
		blockQueue = null;
	}

	public void buildBlock(BlockWork blockWork) {

		Block block = blockWork.getWorld().getBlockAt(blockWork.getX(), blockWork.getY(), blockWork.getZ());

		if (blockWork.getId() == BukkitTools.getTypeId(block))
			return;
		
		BukkitTools.setTypeIdAndData(block, blockWork.getId(), blockWork.getData(), true);

	}

	public void setServer(Server server) {

		this.server = server;
	}

	public Server getServer() {

		return server;
	}

	public void onJobFinish(BlockJob job) {

		if (job.isNotify()) {
			Player player = BukkitTools.getPlayer(job.getBoss());
			player.sendMessage("Generated: " + blocks + " Blocks");
			if (skipped > 0)
				player.sendMessage("Skipped: " + skipped + " Blocks");
		}
	}
}
