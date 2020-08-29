package com.palmergames.bukkit.towny.tasks;

import com.palmergames.util.FileMgmt;

import java.io.File;

public class DeleteFileTask implements Runnable {
	
	private final File file;
	private final boolean permanent;
	
	public DeleteFileTask(File file, boolean permanent) {
		this.file = file;
		this.permanent = permanent;
	}

	@Override
	public void run() {
		if (!file.exists()) {
			return;
		}
		
		if (permanent) {
			file.delete();
		} else {
			FileMgmt.moveFile(file, ("deleted"));
		}
	}
}
