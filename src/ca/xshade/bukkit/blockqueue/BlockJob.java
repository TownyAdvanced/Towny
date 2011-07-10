package ca.xshade.bukkit.blockqueue;

import org.bukkit.World;

class BlockJob {
	private String boss;
	private boolean notify;

	public BlockJob(String boss, World world) {
		this(boss, true);
	}

	public BlockJob(String boss, boolean notify) {
		this.setBoss(boss);
		this.setNotify(notify);
	}

	public void setBoss(String boss) {
		this.boss = boss;
	}

	public String getBoss() {
		return boss;
	}

	public void setNotify(boolean notify) {
		this.notify = notify;
	}

	public boolean isNotify() {
		return notify;
	}
}
