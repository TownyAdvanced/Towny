package com.palmergames.util;

public class MemMgmt {

	public static String getMemoryBar(int size, Runtime run) {

		StringBuilder line = new StringBuilder();
		double percentUsed = (run.totalMemory() - run.freeMemory()) / (double)run.maxMemory();
		int pivot = (int) Math.floor(size * percentUsed);
		for (int i = 0; i < pivot - 1; i++)
			line.append("=");
		if (pivot < size - 1)
			line.append("+");
		for (int i = pivot + 1; i < size; i++)
			line.append("-");
		return line.toString();
	}

	public static String getMemSize(long num) {

		String[] s = { "By", "Kb", "Mb", "Gb", "Tb" };
		double n = num;
		int w = 0;
		while (n > 1024 && w < s.length - 1) {
			n /= 1024;
			w += 1;
		}
		return String.format("%.2f %s", n, s[w]);
	}
}
