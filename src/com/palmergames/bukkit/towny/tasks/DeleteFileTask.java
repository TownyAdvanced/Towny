package com.palmergames.bukkit.towny.tasks;

import com.palmergames.util.FileMgmt;

import java.io.File;

public class DeleteFileTask implements Runnable {
	
	private final File file;
	
	public DeleteFileTask(File file) {
		this.file = file;
	}

	@Override
	public void run() {
		if (file.exists()) {
			FileMgmt.moveFile(file, ("deleted"));
		}
	}
}
