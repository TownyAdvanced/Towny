package ca.xshade.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileMgmt {
	public static void checkFolders(String[] folders) {
		for (String folder : folders) {
			File f = new File(folder);
			if (!(f.exists() && f.isDirectory()))
				f.mkdir();
		}
	}
	
	public static void checkFiles(String[] files) throws IOException {
		for (String file : files) {
			File f = new File(file);
			if (!(f.exists() && f.isFile()))
				f.createNewFile();
		}
	}
	
	public static String fileSeparator() {
		return System.getProperty("file.separator");
	}
	
	
	// http://www.java-tips.org/java-se-tips/java.io/how-to-copy-a-directory-from-one-location-to-another-loc.html
	public static void copyDirectory(File sourceLocation , File targetLocation) throws IOException {
		if (sourceLocation.isDirectory()) {
			if (!targetLocation.exists())
				targetLocation.mkdir();
			            
			String[] children = sourceLocation.list();
			for (int i=0; i<children.length; i++)
				copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
		} else {      
			InputStream in = new FileInputStream(sourceLocation);
			OutputStream out = new FileOutputStream(targetLocation);
			// Copy the bits from in stream to out stream.
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
				out.write(buf, 0, len);
			in.close();
			out.close();
		}
	}
	
	public static File CheckYMLexists(String filePath, String defaultRes) {
		
		// open a handle to yml file
		File file = new File(filePath);
		if(file.exists())
			return file;
		
		String resString;
		
		// create the file as it doesn't exist
		try {
			checkFiles(new String[]{
					filePath});
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// file didn't exist so we need to populate a new one
		try {
			resString = convertStreamToString(defaultRes);
			if (resString != null) {
	    		// Save the string to file (*.yml)
	    		try {
		    		FileMgmt.stringToFile(resString, filePath);
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}
	    	}
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		return file;
	}
	
	// pass a resource name and it will return it's contents as a string
	public static String convertStreamToString(String name) throws IOException {
	    if (name != null) {
	        Writer writer = new StringWriter();
	        InputStream is = FileMgmt.class.getResourceAsStream(name);

	        char[] buffer = new char[1024];
	        try {
	            Reader reader = new BufferedReader(
	                    new InputStreamReader(is, "UTF-8"));
	            int n;
	            while ((n = reader.read(buffer)) != -1) {
	                writer.write(buffer, 0, n);
	            }
	        } catch (IOException e)
			{
			    System.out.println("Exception ");
			} finally {
	            is.close();
	        }
	        return writer.toString();
	    } else {        
	        return "";
	    }
	}
	
	//writes a string to a file making all newline codes platform specific
	public static boolean stringToFile(String source, String FileName) throws IOException {
		
		try {

		    BufferedWriter out = new BufferedWriter(new FileWriter(FileName));

		    source.replaceAll("\n", System.getProperty("line.separator"));

		    out.write(source);
		    out.close();
		    return true;

		}
		catch (IOException e)
		{
		    System.out.println("Exception ");
		    return false;
		}
	}


	// move a file to a sub directory
	public static void moveFile(File sourceFile , String targetLocation) throws IOException {
		if (sourceFile.isFile()) {
			// check for an already existing file of that name
			File f = new File((sourceFile.getParent() + fileSeparator() + targetLocation));
			if ((f.exists() && f.isFile()))
				f.delete();
			// Move file to new directory
		    boolean success = sourceFile.renameTo(new File((sourceFile.getParent() + fileSeparator() + targetLocation), sourceFile.getName()));
		    if (!success) {
		        // File was not successfully moved
		    }
		} 
	}
	
	public static void zipDirectory(File sourceFolder, File destination) throws IOException {
		ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destination));
		recursiveZipDirectory(sourceFolder, output);
		output.close();
	}

	public static void zipDirectories(File[] sourceFolders, File destination) throws IOException {
		ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destination));
		for (File sourceFolder : sourceFolders)
			recursiveZipDirectory(sourceFolder, output);
		output.close();
	}
	
	public static void recursiveZipDirectory(File sourceFolder, ZipOutputStream zipStream) throws IOException {
		String[] dirList = sourceFolder.list();
		byte[] readBuffer = new byte[2156];
		int bytesIn = 0;
		for (int i = 0; i < dirList.length; i++) {
			File f = new File(sourceFolder, dirList[i]);
			if (f.isDirectory()) {
				recursiveZipDirectory(f, zipStream);
				continue;
			} else {
				FileInputStream input = new FileInputStream(f);
				ZipEntry anEntry = new ZipEntry(f.getPath());
				zipStream.putNextEntry(anEntry);
				while((bytesIn = input.read(readBuffer)) != -1)
					zipStream.write(readBuffer, 0, bytesIn);
				input.close();
			}
		}
	}

}
