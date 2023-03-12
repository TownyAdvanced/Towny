package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.object.ChangelogResult;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChangelogReader {
	private final String lastVersion;
	private final Path changelogPath;
	private final int limit;
	
	private ChangelogReader(String lastVersion, Path changelogPath, int limit) {
		this.lastVersion = lastVersion;
		this.changelogPath = changelogPath;
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
		int nextVersionIndex = 0;
		
		try (BufferedReader reader = Files.newBufferedReader(this.changelogPath, StandardCharsets.UTF_8)) {
			String line;
			boolean lastVersionFound = false;
			boolean nextVersionFound = false;
			
			while ((line = reader.readLine()) != null) {
				linesRead++;
				
				if (!lastVersionFound && line.startsWith(this.lastVersion)) {
					lastVersionFound = true;
					continue;
				} else if (lastVersionFound && !nextVersionFound && !line.startsWith("-")) {
					nextVersionFound = true;
					nextVersionIndex = linesRead;
					continue;
				}
				
				if (nextVersionFound && out.size() < limit)
					out.add(line);
			}
			
			if (!lastVersionFound)
				return new ChangelogResult(Collections.emptyList(), false, false, -1, linesRead);
		}
		
		return new ChangelogResult(out, true, linesRead >= limit, nextVersionIndex, linesRead);
	}
	
	public static ChangelogReader reader(@NotNull String lastVersion, @NotNull Path changelogPath) {
		return reader(lastVersion, changelogPath, -1);
	}
	
	public static ChangelogReader reader(@NotNull String lastVersion, @NotNull Path changelogPath, int limit) {
		return new ChangelogReader(lastVersion, changelogPath, limit);
	}
}
