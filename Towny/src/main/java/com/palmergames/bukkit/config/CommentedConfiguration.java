package com.palmergames.bukkit.config;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.util.StringMgmt;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author dumptruckman
 * @author Articdive
 * @author LlmDl
 */
public class CommentedConfiguration extends YamlConfiguration {
	private final HashMap<String, String> comments = new HashMap<>();
	private final Path path;
	private final Logger logger;
	private final String newLine = System.getProperty("line.separator");
	private int depth;

	/**
	 * Create a new CommentedConfiguration using the file at the given path, using
	 * Towny's Logger for any error messages.
	 */
	public CommentedConfiguration(Path path) {
		super();
		this.path = path;
		logger = Towny.getPlugin().getLogger();
		setWidth();
	}

	/**
	 * Create a new CommentedConfiguration using the file at the given path, for the
	 * given plugin. Plugin's own Logger will be used for any error messages.
	 */
	public CommentedConfiguration(Path path, Plugin plugin) {
		super();
		this.path = path;
		logger = plugin.getLogger();
		setWidth();
	}

	/**
	 * Create a new CommentedConfiguration using the given file, using Towny's
	 * Logger for any error messages.
	 */
	public CommentedConfiguration(File file) {
		super();
		this.path = file.toPath();
		logger = Towny.getPlugin().getLogger();
		setWidth();
	}

	
	/**
	 * Load the yaml configuration file into memory.
	 * @return true if file is able to load.
	 */
	public boolean load() {
		return loadFile();
	}

	private boolean loadFile() {
		try {
			this.load(path.toFile());
			return true;
		} catch (InvalidConfigurationException | IOException e) {
			logger.warning(String.format("Loading error: Failed to load file %s (does it pass a yaml parser?).", path));
			logger.warning("https://jsonformatter.org/yaml-parser");
			logger.warning(e.getMessage());
			return false;
		}
	}

	/**
	 * Save the yaml configuration file from memory to file.
	 */
	public void save() {

		// Save the config like normal.
		boolean saved = saveFile();

		// If there's comments to add and it saved fine, we need to add comments.
		if (!comments.isEmpty() && saved) {

			// String list of each line in the config file.
			List<String> yamlContents = readYaml();

			// Generate new config strings, ignoring existing comments and parsing in our
			// up-to-date comments from the ConfigNodes enum.
			StringBuilder newContents = readConfigToString(yamlContents);

			// Write newContents to file.
			writeYaml(newContents);
		}
	}

	/**
	 * Read the file found at the path into a list of Strings.
	 * @return yamlContents a List of Strings which represent the raw lines of the file at the given path.
	 */
	private List<String> readYaml() {

		List<String> yamlContents = new ArrayList<>();
		try {
			yamlContents = Files.readAllLines(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.warning(String.format("Failed to read file %s.", path));
			logger.warning(e.getMessage());
		}
		return yamlContents;
	}

	/**
	 * Write the file to the given path, in valid yaml format, with the comments added in.
	 */
	private void writeYaml(StringBuilder newContents) {
		try {
			Files.write(path, newContents.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			logger.warning(String.format("Saving error: Failed to write to file %s.", path));
			logger.warning(e.getMessage());
		}
	}

	/**
	 * Attempts to save the file via YamlConfiguration's usual methods.
	 * 
	 * @return true if the file was able to be saved.
	 */
	private boolean saveFile() {
		try {
			this.save(path.toFile());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Read through the contents of the old config and return an up to date new
	 * config, complete with comments generated from the ConfigNodes enum.
	 * 
	 * @param oldContents List of Strings which represent the config being loaded
	 *                    from the server.
	 * @return newContents StringBuilder.
	 */
	private StringBuilder readConfigToString(List<String> oldContents) {
		// This will hold the newly formatted line
		StringBuilder newContents = new StringBuilder();
		// This holds the current path the lines are at in the config
		String currentPath = "";
		// The depth of the path. (number of words separated by periods - 1)
		depth = 0;

		// Loop through the old config lines.
		for (String line : oldContents) {
			// Spigot's addition of native SnakeYAML comment support in MC 1.18.1, requires
			// us to ignore the comments in our own file, which will be replaced later on
			// with up-to-date comments from the ConfigNodes enum.
			if (line.trim().startsWith("#") || line.isEmpty() || line.trim().isEmpty())
				continue;
			
			// If the line is a node (and not something like a list value)
			if (line.contains(": ") || (line.length() > 1 && line.charAt(line.length() - 1) == ':')) {

				// Build the new line, allowing us to get the comments made in the ConfigNodes enum.
				// ie: new_world_settings.pvp.force_pvp_on
				currentPath = getCurrentPath(line, currentPath);

				// Grab any available comments for the current path.
				String comment = comments.get(currentPath);

				// If there are comments, add them to the beginning of the current line.
				if (comment != null)
					line = comment + newLine + line;
			}

			// Add the line to what will be written in the new config.
			newContents.append(line).append(newLine);
		}

		return newContents;
	}

	/**
	 * Creates a Configuration path from a raw yaml file's lines.
	 * 
	 * @param line String line from the old configuration file.
	 * @param currentPath What has been built thus far as a Configuration path.
	 * @return currentPath The next Configuration path to save into the new config.
	 */
	private String getCurrentPath(String line, String currentPath) {
		// Grab the index of the end of the node name
		int index;
		index = line.indexOf(": ");
		if (index < 0) {
			index = line.length() - 1;
		}
		// The first line of the file, store the node name as the currentPath.
		if (currentPath.isEmpty()) {
			currentPath = line.substring(0, index);
		} else {
			// Calculate the whitespace preceding the node name, allowing us to determine depth.
			int whiteSpace = getWhiteSpaceFromLine(line);
			// Get the current node we're adding.
			String nodeName = line.substring(whiteSpace, index);
			// Find out if the current depth (whitespace * 2) is greater/lesser/equal to the previous depth.
			if (whiteSpace / 2 > depth) {
				// Path is deeper.  Add a . and the node name.
				currentPath += "." + nodeName;
				depth++;
			} else if (whiteSpace / 2 < depth) {
				// Path is shallower, calculate current depth from whitespace (whitespace / 2) and subtract that many levels from the currentPath
				int newDepth = whiteSpace / 2;
				// Shrink the currentPath, removing nodes with no more children.
				currentPath = shrinkCurrentPath(currentPath, newDepth);
				// Add the nodeName to the currentPath.
				currentPath = addNodeNameToCurrentPath(currentPath, nodeName);
				// Reset the depth
				depth = newDepth;
			} else {
				// Path is same depth, replace the last path node name to the current node name.
				// Add the nodeName to the currentPath.
				currentPath = addNodeNameToCurrentPath(currentPath, nodeName);
			}
		}

		return currentPath;
	}

	/**
	 * Get the whitespace in front of the current line's text.
	 * 
	 * @param line String line from the old config.
	 * @return number of empty spaces preceding the current line's text.
	 */
	private int getWhiteSpaceFromLine(String line) {
		int whiteSpace = 0;
		for (int n = 0; n < line.length(); n++)
			if (line.charAt(n) == ' ')
				whiteSpace++;
			else
				break;
		return whiteSpace;
	}

	/**
	 * Shrink the currentPath, scaling the path back, removing completed child nodes.
	 * 
	 * @param currentPath String representing the Configuration path. 
	 * @param newDepth int representing the depth we're going to get back to.
	 * @return currentPath with finished child nodes removed.
	 */
	private String shrinkCurrentPath(String currentPath, int newDepth) {
		for (int i = 0; i < depth - newDepth; i++)
			currentPath = currentPath.replace(currentPath.substring(currentPath.lastIndexOf(".")), "");
		return currentPath;
	}

	/**
	 * Add the nodeName to the currentPath, either at the same depth or shallower.
	 * 
	 * @param currentPath String representing the Configuration path being added.
	 * @param nodeName String representing the new node being added to the currentPath.
	 * @return currentPath after it has been adjusted and had the new nodeName added.
	 */
	private String addNodeNameToCurrentPath(String currentPath, String nodeName) {
		// Grab the index of the final period
		int lastIndex = currentPath.lastIndexOf(".");
		if (lastIndex < 0) {
			// If there isn't a final period, set the current path to nothing because we're at root
			currentPath = "";
		} else {
			// If there is a final period, replace everything after it with nothing
			currentPath = currentPath.replace(currentPath.substring(lastIndex), "");
			currentPath += ".";
		}
		return currentPath += nodeName;
	}

	/**
	 * Stores a comment for the specified Configuration path. The comment can be
	 * multiple lines. An empty string will indicate a blank line.
	 *
	 * @param path         Configuration path to add comment.
	 * @param commentLines Comments to add. One String per line.
	 */
	public void addComment(String path, String... commentLines) {

		StringBuilder commentBlock = new StringBuilder();
		// Get the preceding spaces based on how many .'s are in the path.
		String leadingSpaces = StringMgmt.repeat("  ", (int) path.chars().filter(ch -> ch == '.').count()); 
		// Parse over all of the comment lines.
		for (String commentLine : commentLines) {
			// Add the leading spaces if commentLine isn't empty.
			commentLine = !commentLine.isEmpty() ? leadingSpaces + commentLine : "  "; 
			// Add a new line if this comment block already has more than one line.
			if (commentBlock.length() > 0)
				commentBlock.append(newLine);
			// Add the line to the commentBlock.
			commentBlock.append(commentLine);
		}
		// Put the comment block into the comments HashMap to be parsed into the config later.
		comments.put(path, commentBlock.toString());
	}

	/**
	 * Width became an option with MC 1.18.1. Setting it wider will allow
	 * configurations to not break things into multi-lines.
	 */
	private void setWidth() {
		try {
			this.options().width(10000);
		} catch (NoSuchMethodError ignored) {}
	}
}