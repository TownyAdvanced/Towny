package com.palmergames.util;

public class MemMgmt {
	public static String getMemoryBar(int size, Runtime run) {
		String line = "";
		double percentUsed = (run.totalMemory() - run.freeMemory())
				/ run.maxMemory();
		int pivot = (int) Math.floor(size * percentUsed);
		for (int i = 0; i < pivot - 1; i++)
			line += "=";
		if (pivot < size - 1)
			line += "+";
		for (int i = pivot + 1; i < size; i++)
			line += "-";
		return line;
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
