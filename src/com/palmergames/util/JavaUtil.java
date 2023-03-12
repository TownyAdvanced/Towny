package com.palmergames.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class JavaUtil {

	/**
	 * Recursively check if the interface inherits the super interface. Returns
	 * false if not an interface. Returns true if sup = sub.
	 * 
	 * @param sup The class of the interface you think it is a subinterface of.
	 * @param sub The possible subinterface of the super interface.
	 * @return true if it is a subinterface.
	 */

	public static boolean isSubInterface(Class<?> sup, Class<?> sub) {

		if (sup.isInterface() && sub.isInterface()) {
			if (sup.equals(sub))
				return true;
			for (Class<?> c : sub.getInterfaces())
				if (isSubInterface(sup, c))
					return true;
		}
		return false;
	}

	public static List<String> readTextFromJar(String path) throws IOException {
		try (InputStream is = JavaUtil.class.getResourceAsStream(path)) {
			if (is == null)
				throw new FileNotFoundException("Could not find '" + path + "' inside the jar as a resource.");
			
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
				return reader.lines().collect(Collectors.toList());
			}
		}
	}
	
	public static void saveResource(String resource, Path destination, CopyOption... options) throws IOException {
		try (InputStream is = JavaUtil.class.getResourceAsStream(resource)) {
			if (is == null)
				throw new FileNotFoundException("Could not find '" + resource + "' inside the jar as a resource.");

			Files.copy(is, destination, options);
		}
	}
}
