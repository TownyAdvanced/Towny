package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.object.ChangelogResult;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChangelogReader {
	private final String lastVersion;
	private final InputStream changelogStream;
	private final int limit;
	
	private ChangelogReader(String lastVersion, InputStream inputStream, int limit) {
		this.lastVersion = lastVersion;
		this.changelogStream = inputStream;
		this.limit = limit;
	}

	/**
	 * Attempts to read the changelog at the provided path.
	 * @return The changelog result.
	 * @throws IOException If an IOException occurs while reading the file.
	 */
	@NotNull
	public ChangelogResult read() throws IOException {
		List<String> out = new ArrayList<>();
		int linesRead = 0;
		boolean limitReached = false;
		int nextVersionIndex = 0;
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.changelogStream, StandardCharsets.UTF_8))) {
			String line;
			boolean lastVersionFound = false;
			boolean nextVersionFound = false;
			
			while ((line = reader.readLine()) != null) {
				linesRead++;
				
				if (!lastVersionFound && line.startsWith(this.lastVersion)) {
					lastVersionFound = true;
					continue;
				} else if (lastVersionFound && !nextVersionFound && !line.trim().startsWith("-")) {
					nextVersionFound = true;
					nextVersionIndex = linesRead;
				}
				
				if (nextVersionFound) {
					if (limit < 0 || out.size() < limit) {
						out.add(line);
					} else {
						limitReached = true;
					}
				}
			}
			
			if (!lastVersionFound)
				return new ChangelogResult(Collections.emptyList(), false, false, -1, linesRead);
		}
		
		return new ChangelogResult(out, true, limitReached, nextVersionIndex, linesRead);
	}
	
	public static ChangelogReader reader(@NotNull String lastVersion, @NotNull InputStream changelogStream) {
		return reader(lastVersion, changelogStream, -1);
	}
	
	public static ChangelogReader reader(@NotNull String lastVersion, @NotNull InputStream changelogStream, int limit) {
		return new ChangelogReader(lastVersion, changelogStream, limit);
	}
}
