package com.palmergames.bukkit.blockqueue;

import java.util.LinkedList;

import org.bukkit.Server;

public class BlockQueue {

	private LinkedList<Object> queue = new LinkedList<Object>();
	private static volatile BlockQueue instance;
	private BlockWorker worker;

	public synchronized void addWork(Object obj) {

		queue.addLast(obj);
		notify();
	}

	public synchronized Object getWork() throws InterruptedException {

		while (queue.isEmpty())
			wait();
		return queue.removeFirst();
	}

	public static BlockQueue getInstance() throws Exception {

		if (instance == null)
			throw new Exception("BlockQueue has not been initialized yet");

		return instance;
	}

	public static BlockQueue newInstance(Server server) {

		instance = new BlockQueue();
		instance.worker = new BlockWorker(server, instance);
		
		server.getScheduler().runTaskLaterAsynchronously(null, instance.getWorker(),0);

		return instance;
	}

	public BlockWorker getWorker() {

		return worker;
	}
}
