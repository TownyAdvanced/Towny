package com.palmergames.bukkit.towny.tasks;

import com.palmergames.util.FileMgmt;

import java.io.File;

public class PermanentDeleteFileTask implements Runnable {

	private final File file;

	public PermanentDeleteFileTask(File file) {
		this.file = file;
	}

	@Override
	public void run() {
		if (file.exists()) {
			file.delete();
		}
	}
}
