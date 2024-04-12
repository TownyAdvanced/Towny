package com.palmergames.util;

public class Pair<L, R> {
	private final L left;
	private final R right;
	
	private Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}
	
	public static <L, R> Pair<L, R> pair(L left, R right) {
		return new Pair<>(left, right);
	}
	
	public L left() {
		return this.left;
	}
	
	public R right() {
		return this.right;
	}
	
	public L key() {
		return this.left;
	}
	
	public R value() {
		return this.right;
	}

	@Override
	public String toString() {
		return "Pair{" +
			"left=" + left +
			", right=" + right +
			'}';
	}
}
