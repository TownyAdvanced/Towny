package com.palmergames.bukkit.util;

import java.util.Objects;

public class Version implements Comparable<Version> {

	private final String version;

	public final String get() {
		return this.version;
	}

	public Version(String version) {
		if(version == null)
			throw new IllegalArgumentException("Version can not be null");
		if(!version.matches("[0-9]+(\\.[0-9]+)*"))
			throw new IllegalArgumentException("Invalid version format: " + version);
		this.version = version;
	}

	@Override public int compareTo(Version that) {
		if(that == null)
			return 1;
		String[] thisParts = this.get().split("\\.");
		String[] thatParts = that.get().split("\\.");
		int length = Math.max(thisParts.length, thatParts.length);
		for(int i = 0; i < length; i++) {
			int thisPart = i < thisParts.length ?
				Integer.parseInt(thisParts[i]) : 0;
			int thatPart = i < thatParts.length ?
				Integer.parseInt(thatParts[i]) : 0;
			if(thisPart < thatPart)
				return -1;
			if(thisPart > thatPart)
				return 1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Version)) return false;
		Version version1 = (Version) o;
		return Objects.equals(version, version1.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(version);
	}

	@Override
	public String toString() {
		return "Version{" +
			"version='" + version + '\'' +
			'}';
	}
}
