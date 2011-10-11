package com.palmergames.bukkit.config;

import com.palmergames.util.FileMgmt;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author dumptruckman
 */
public class CommentedConfiguration extends YamlConfiguration {

    private HashMap<String, String> comments;
    private File file;
   

    public CommentedConfiguration(File file) {
        super();
        //this.load(file);
        comments = new HashMap<String, String>();
        this.file = file;
    }
    
    public boolean load() {
    	
    	boolean loaded = true;
    	
    	try {
			this.load(file);
		} catch (FileNotFoundException e) {
			loaded = false;
		} catch (IOException e) {
			loaded = false;
		} catch (InvalidConfigurationException e) {
			loaded = false;
		}
    	
    	return loaded;
    }

    public boolean save() {
    	
    	boolean saved = true;
    	
        // Save the config just like normal
    	try {
    		super.save(file);
    	} catch (Exception e) {
    		saved = false;
    	}
       

        // if there's comments to add and it saved fine, we need to add comments
        if (!comments.isEmpty() && saved) {
            // String array of each line in the config file
            String[] yamlContents =
                    FileMgmt.convertFileToString(file).split("[" + System.getProperty("line.separator") + "]");

            // This will hold the newly formatted line
            String newContents = "";
            // This holds the current path the lines are at in the config
            String currentPath = "";
            // This tells if the specified path has already been commented
            boolean commentedPath = false;
            // The depth of the path. (number of words separated by periods - 1)
            int depth = 0;

            // Loop through the config lines
            for (String line : yamlContents) {
                // If the line is a node (and not something like a list value)
                if (line.contains(": ") || (line.length() > 1 && line.charAt(line.length() - 1) == ':')) {
                    // Grab the index of the end of the node name
                    int index = 0;
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
                        // This is a new node so we need to mark it for commenting (if there are comments)
                        commentedPath = false;
                    }
                }

                String comment = "";
                if (!commentedPath) {
                    // If there's a comment for the current path, retrieve it and flag that path as already commented
                    comment = comments.get(currentPath);
                    commentedPath = true;
                }
                if (comment != null) {
                    // Add the comment to the beginning of the current line
                    line = comment + System.getProperty("line.separator") + line;
                }
                // Add the (modified) line to the total config String
                newContents += line + System.getProperty("line.separator");
            }
            try {
                // Write the string to the config file
            	System.out.print("Saving Commented Config...");
                FileMgmt.stringToFile(newContents, file);
            } catch (IOException e) {
                saved = false;
                System.out.print("Failed to save Commented Config!");
            }
        } else
        	System.out.print("Not updating Config.");

        return saved;
    }

    /**
     * Adds a comment just before the specified path.  The comment can be multiple lines.  An empty string will indicate a blank line.
     * @param path Configuration path to add comment.
     * @param commentLines Comments to add.  One String per line.
     */
    public void addComment(String path, String...commentLines) {
        StringBuilder commentstring = new StringBuilder();
        String leadingSpaces = "";
        for (int n = 0; n < path.length(); n++) {
            if (path.charAt(n) == '.') {
                leadingSpaces += "  ";
            }
        }
        for (String line : commentLines) {
            if (!line.isEmpty()) {
                line = leadingSpaces + line;
            } else {
                line = " ";
            }
            if (commentstring.length() > 0) {
                commentstring.append("\r\n");
            }
            commentstring.append(line);
        }
        comments.put(path, commentstring.toString());
    }
}
