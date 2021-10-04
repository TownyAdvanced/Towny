package com.palmergames.bukkit.config;

import com.palmergames.bukkit.towny.Towny;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author dumptruckman
 * @author Articdive
 */
public class CommentedConfiguration extends YamlConfiguration {
	private final HashMap<String, String> comments = new HashMap<>();
	private final Path path;

	private final DumperOptions yamlOptions = new DumperOptions();
	private final Representer yamlRepresenter = new YamlRepresenter();
	private final Yaml yaml = new Yaml(new YamlConstructor(), yamlRepresenter, yamlOptions);

	public CommentedConfiguration(Path path) {
		super();
		this.path = path;
	}

	public boolean load() {

		try {
			this.load(path.toFile());
		} catch (InvalidConfigurationException | IOException e) {
			Towny.getPlugin().getLogger().warning(String.format("Loading error: Failed to load file %s (does it pass a yaml parser?).", path));
			Towny.getPlugin().getLogger().warning("https://jsonformatter.org/yaml-parser");
			Towny.getPlugin().getLogger().warning(e.getMessage());
			return false;
		}

		return true;
	}

	public void save() {

		boolean saved = true;

		// Save the config just like normal
		try {
			this.save(path.toFile());

		} catch (Exception e) {
			saved = false;
		}

		// if there's comments to add and it saved fine, we need to add comments
		if (!comments.isEmpty() && saved) {

			// This will hold the newly formatted line
			StringBuilder newContents = new StringBuilder();
			// This holds the current path the lines are at in the config
			String currentPath = "";
			// This flags if the line is a node or unknown text.
			boolean node;
			// The depth of the path. (number of words separated by periods - 1)
			int depth = 0;

			// String list of each line in the config file
			List<String> yamlContents;
			try {
				yamlContents = Files.readAllLines(path, StandardCharsets.UTF_8);
			} catch (IOException e) {
				Towny.getPlugin().getLogger().warning(String.format("Saving error: Failed to save file %s.", path));
				Towny.getPlugin().getLogger().warning(e.getMessage());
				// This is how Towny would have handled such an error in the past
				// It would return an empty String and the for loop would basically not run.
				// We can not return here since it still has to rewrite.
				yamlContents = new ArrayList<>();
			}

			// Loop through the config lines
			for (String line : yamlContents) {
				// If the line is a node (and not something like a list value)
				if (line.contains(": ") || (line.length() > 1 && line.charAt(line.length() - 1) == ':')) {

					// This is a node so flag it as one
					node = true;

					// Grab the index of the end of the node name
					int index;
					index = line.indexOf(": ");
					if (index < 0) {
						index = line.length() - 1;
					}
					// If currentPath is empty, store the node name as the currentPath. (this is only on the first iteration, i think)
					if (currentPath.isEmpty()) {
						currentPath = line.substring(0, index);
					} else {
						// Calculate the whitespace preceding the node name
						int whiteSpace = 0;
						for (int n = 0; n < line.length(); n++) {
							if (line.charAt(n) == ' ') {
								whiteSpace++;
							} else {
								break;
							}
						}
						// Find out if the current depth (whitespace * 2) is greater/lesser/equal to the previous depth
						if (whiteSpace / 2 > depth) {
							// Path is deeper.  Add a . and the node name
							currentPath += "." + line.substring(whiteSpace, index);
							depth++;
						} else if (whiteSpace / 2 < depth) {
							// Path is shallower, calculate current depth from whitespace (whitespace / 2) and subtract that many levels from the currentPath
							int newDepth = whiteSpace / 2;
							for (int i = 0; i < depth - newDepth; i++) {
								currentPath = currentPath.replace(currentPath.substring(currentPath.lastIndexOf(".")), "");
							}
							// Grab the index of the final period
							int lastIndex = currentPath.lastIndexOf(".");
							if (lastIndex < 0) {
								// if there isn't a final period, set the current path to nothing because we're at root
								currentPath = "";
							} else {
								// If there is a final period, replace everything after it with nothing
								currentPath = currentPath.replace(currentPath.substring(currentPath.lastIndexOf(".")), "");
								currentPath += ".";
							}
							// Add the new node name to the path
							currentPath += line.substring(whiteSpace, index);
							// Reset the depth
							depth = newDepth;
						} else {
							// Path is same depth, replace the last path node name to the current node name
							int lastIndex = currentPath.lastIndexOf(".");
							if (lastIndex < 0) {
								// if there isn't a final period, set the current path to nothing because we're at root
								currentPath = "";
							} else {
								// If there is a final period, replace everything after it with nothing
								currentPath = currentPath.replace(currentPath.substring(currentPath.lastIndexOf(".")), "");
								currentPath += ".";
							}
							//currentPath = currentPath.replace(currentPath.substring(currentPath.lastIndexOf(".")), "");
							currentPath += line.substring(whiteSpace, index);

						}

					}

				} else {
					node = false;
				}

				if (node) {
					// If there's a comment for the current path, retrieve it and flag that path as already commented
					String comment = comments.get(currentPath);

					if (comment != null) {
						// Add the comment to the beginning of the current line
						line = comment + System.getProperty("line.separator") + line + System.getProperty("line.separator");
					} else {
						// Add a new line as it is a node, but has no comment
						line += System.getProperty("line.separator");
					}
				}
				// Add the (modified) line to the total config String
				if (!node) {
					newContents.append(line).append(System.getProperty("line.separator"));
				} else {
					newContents.append(line);
				}
			}

			/*
			 * Due to a Bukkit Bug with the Configuration
			 * we just need to remove any extra comments at the start of a file.
			 */
			while (newContents.toString().startsWith(" " + System.getProperty("line.separator"))) {
				newContents = new StringBuilder(newContents.toString().replaceFirst(" " + System.getProperty("line.separator"), ""));
			}
			
			// Write to file
			try {
				// Whatever IntelliJ tells you, Jabel doesn't have the writeString method for whatever reason.
				// Keep this the way it is.
				Files.write(path, newContents.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
			} catch (IOException e) {
				Towny.getPlugin().getLogger().warning(String.format("Saving error: Failed to write to file %s.", path));
				Towny.getPlugin().getLogger().warning(e.getMessage());
			}
		}
	}

	/**
	 * Adds a comment just before the specified path. The comment can be
	 * multiple lines. An empty string will indicate a blank line.
	 *
	 * @param path         Configuration path to add comment.
	 * @param commentLines Comments to add. One String per line.
	 */
	public void addComment(String path, String... commentLines) {

		StringBuilder commentstring = new StringBuilder();
		StringBuilder leadingSpaces = new StringBuilder();
		for (int n = 0; n < path.length(); n++) {
			if (path.charAt(n) == '.') {
				leadingSpaces.append("  ");
			}
		}
		for (String line : commentLines) {
			if (!line.isEmpty()) {
				line = leadingSpaces + line;
			} else {
				line = " ";
			}
			if (commentstring.length() > 0) {
				commentstring.append(System.getProperty("line.separator"));
			}
			commentstring.append(line);
		}
		comments.put(path, commentstring.toString());
	}

	@Override
	public String saveToString() {
		yamlOptions.setIndent(options().indent());
		yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		yamlOptions.setWidth(10000);
		yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);


		String dump = yaml.dump(getValues(false));


		if (dump.equals(BLANK_CONFIG)) {
			dump = "";
		}

		return dump;
	}
}
