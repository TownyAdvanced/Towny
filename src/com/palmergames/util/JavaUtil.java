package com.palmergames.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.palmergames.util.JavaUtil;

public class JavaUtil {
	
	/**
	 * Recursively check if the interface inherits the super interface. Returns false if not an interface. Returns true if sup = sub.
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
		BufferedReader fin = new BufferedReader(
				new InputStreamReader(
						JavaUtil.class.getResourceAsStream(
								path)));
		String line;
		List<String> out = new ArrayList<String>();
		try {
			while ((line = fin.readLine()) != null)
				out.add(line);
		} catch (IOException e) {
			throw new IOException(e.getCause());
		} finally {
			fin.close();
		}
		return out;
	}
}
