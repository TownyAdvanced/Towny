package com.palmergames.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileMgmt {
	/**
	 * Checks a folderPath to see if it exists, if it doesn't it will attempt
	 * to create the folder at the designated path.
	 *
	 * @param folderPath {@link String} containing a path to a folder.
	 * @return True if the folder exists or if it was successfully created.
	 */
	public static boolean checkOrCreateFolder(String folderPath) {
		File file = new File(folderPath);
		return file.exists() || file.mkdirs() || file.isDirectory();
	}
	
	/**
	 * Checks an array of folderPaths to see if they exist, if they don't
	 * it will try to create the folder at the designated paths.
	 *
	 * @param folders array of {@link String} containing a path to a folder.
	 * @return true or false   
	 */
	public static boolean checkOrCreateFolders(String... folders) {
		for (String folder : folders) {
			if (!checkOrCreateFolder(folder)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks a filePath to see if it exists, if it doesn't it will attempt
	 * to create the file at the designated path.
	 *
	 * @param filePath {@link String} containing a path to a file.
	 * @return True if the folder exists or if it was successfully created.
	 */
	public static boolean checkOrCreateFile(String filePath) {
		File file = new File(filePath);
		if (!checkOrCreateFolder(file.getParentFile().getPath())) {
			return false;
		}
		try {
			return file.exists() || file.createNewFile();
		} catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * Checks an array of folderPaths to see if they exist, if they don't
	 * it will try to create the folder at the designated paths.
	 *
	 * @param files array of {@link String} containing a path to a file.
	 * @return true or false   
	 */
	public static boolean checkOrCreateFiles(String... files) {
		for (String file : files) {
			if (!checkOrCreateFile(file)) {
				return false;
			}
		}
		return true;
	}

	// http://www.java-tips.org/java-se-tips/java.io/how-to-copy-a-directory-from-one-location-to-another-loc.html
	public static void copyDirectory(File sourceLocation, File targetLocation) throws IOException {
		synchronized (sourceLocation) {
			if (sourceLocation.isDirectory()) {
				if (!targetLocation.exists())
					targetLocation.mkdir();

				String[] children = sourceLocation.list();
				for (String aChildren : children)
					copyDirectory(new File(sourceLocation, aChildren), new File(targetLocation, aChildren));
			} else {
				OutputStream out = new FileOutputStream(targetLocation);
				try {
					InputStream in = new FileInputStream(sourceLocation);
					// Copy the bits from in stream to out stream.
					byte[] buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) > 0)
						out.write(buf, 0, len);
					in.close();
					out.close();
				} catch (IOException ex) {
					// failed to access file.
					System.out.println("Error: Could not access: " + sourceLocation);
				}
				out.close();
			}
		}
	}

	public static File unpackResourceFile(String filePath, String resource, String defaultRes) {

		// open a handle to yml file
		File file = new File(filePath);

		if ((file.exists())/* && (!filePath.contains(FileMgmt.fileSeparator() + defaultRes))*/)
			return file;

		String resString;

		/*
		 * create the file as it doesn't exist,
		 * or it's the default file
		 * so refresh just in case.
		 */
		checkOrCreateFile(filePath);

		// Populate a new file
		try {
			resString = convertStreamToString("/" + resource);
			FileMgmt.stringToFile(resString, filePath);

		} catch (IOException e) {
			// No resource file found
			try {
				resString = convertStreamToString("/" + defaultRes);
				FileMgmt.stringToFile(resString, filePath);
			} catch (IOException e1) {
				// Default resource not found
				e1.printStackTrace();
			}
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
				Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} catch (IOException e) {
				System.out.println("Exception ");
			} finally {
				try {
					is.close();
				} catch (NullPointerException e) {
					//Failed to open a stream
					throw new IOException();
				}
			}
			return writer.toString();
		} else {
			return "";
		}
	}

	/**
	 * Pass a file and it will return it's contents as a string.
	 *
	 * @param file File to read.
	 *
	 * @return Contents of file. String will be empty in case of any errors.
	 */
	public static String convertFileToString(File file) {

		if (file != null && file.exists() && file.canRead() && !file.isDirectory()) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try (InputStream is = new FileInputStream(file)) {
				Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
				reader.close();
			} catch (IOException e) {
				System.out.println("Exception ");
			}
			return writer.toString();
		} else {
			return "";
		}
	}

	//writes a string to a file making all newline codes platform specific
	public static void stringToFile(String source, String FileName) {

		if (source != null) {
			// Save the string to file (*.yml)
			stringToFile(source, new File(FileName));
		}

	}

	/**
	 * Writes the contents of a string to a file.
	 *
	 * @param source String to write.
	 * @param file   File to write to.
	 */
	public static void stringToFile(String source, File file) {

		try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
			 BufferedWriter bufferedWriter = new BufferedWriter(osw)) {
			
			bufferedWriter.write(source);
			
		} catch (IOException e) {
			System.out.println("Exception ");
		}
	}

	/**
	 * Write a list to a file, terminating each line with a system specific new line.
	 * 
	 * @param source - Data source
	 * @param targetLocation - Target location on Filesystem
	 * @return true on success, false on IOException
	 */
	public static boolean listToFile(List<String> source, String targetLocation) {
		File file = new File(targetLocation);
		try(OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
			BufferedWriter bufferedWriter = new BufferedWriter(osw)) {
			
			for (String aSource : source) {
				bufferedWriter.write(aSource + System.getProperty("line.separator"));
			}

			return true;
		} catch (IOException e) {
			System.out.println("Exception ");
			return false;
		}
	}

	// move a file to a sub directory
	public static void moveFile(File sourceFile, String targetLocation) {

		synchronized (sourceFile) {
			if (sourceFile.isFile()) {
				// check for an already existing file of that name
				File f = new File((sourceFile.getParent() + File.separator + targetLocation + File.separator + sourceFile.getName()));
				if ((f.exists() && f.isFile()))
					f.delete();
				// Move file to new directory
				sourceFile.renameTo(new File((sourceFile.getParent() + File.separator + targetLocation), sourceFile.getName()));
				
			}
		}
	}
	
	public static void moveTownBlockFile(File sourceFile, String targetLocation, String townDir) {

		synchronized (sourceFile) {
			if (sourceFile.isFile()) {
				if (!townDir.isEmpty())
					checkOrCreateFolder(sourceFile.getParent() + File.separator + "deleted" + File.separator + townDir);
				else 
					checkOrCreateFolder(sourceFile.getParent() + File.separator + "deleted");
				// check for an already existing file of that name
				File f = new File((sourceFile.getParent() + File.separator + targetLocation + File.separator + townDir + File.separator + sourceFile.getName()));
				if ((f.exists() && f.isFile()))
					f.delete();
				// Move file to new directory
				sourceFile.renameTo(new File((sourceFile.getParent() + File.separator + targetLocation + File.separator + townDir), sourceFile.getName()));

			}
		}
	}

	public static void zipDirectories(File destination, File... sourceFolders) throws IOException {

		synchronized (sourceFolders) {
			ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destination), StandardCharsets.UTF_8);
			for (File sourceFolder : sourceFolders)
				recursiveZipDirectory(sourceFolder, output);
			output.close();
		}
	}

	public static void recursiveZipDirectory(File sourceFolder, ZipOutputStream zipStream) throws IOException {

		synchronized (sourceFolder) {

			String[] dirList = sourceFolder.list();
			byte[] readBuffer = new byte[2156];
			int bytesIn;
			for (String aDirList : dirList) {
				File f = new File(sourceFolder, aDirList);
				if (f.isDirectory()) {
					recursiveZipDirectory(f, zipStream);
				} else if (f.isFile() && f.canRead()) {
					FileInputStream input = new FileInputStream(f);
					ZipEntry anEntry = new ZipEntry(f.getPath());
					zipStream.putNextEntry(anEntry);
					while ((bytesIn = input.read(readBuffer)) != -1)
						zipStream.write(readBuffer, 0, bytesIn);
					input.close();
				}
			}
		}
	}

	/**
	 * Delete file, or if path represents a directory, recursively
	 * delete it's contents beforehand.
	 * 
	 * @param file - {@link File} to delete
	 */
	public static void deleteFile(File file) {

		synchronized (file) {

			if (file.isDirectory()) {
				File[] children = file.listFiles();
				if (children != null) {
					for (File child : children)
						deleteFile(child);
				}
				children = file.listFiles();
				if (children == null || children.length == 0) {
					if (!file.delete())
						System.out.println("Error: Could not delete folder: " + file.getPath());
				}
			} else if (file.isFile()) {
				if (!file.delete())
					System.out.println("Error: Could not delete file: " + file.getPath());
			}
		}
	}

	/**
	 * Delete child files/folders of backupsDir with a filename ending
	 * in milliseconds that is older than deleteAfter milliseconds in age.
	 * 
	 * @param backupsDir - {@link File} path to backupsDir
	 * @param deleteAfter - Maximum age of files, in milliseconds
	 */
	public static void deleteOldBackups(File backupsDir, long deleteAfter) {

		synchronized (backupsDir) {

			TreeSet<Long> deleted = new TreeSet<>();
			if (backupsDir.isDirectory()) {
				File[] children = backupsDir.listFiles();
				if (children != null) {
					for (File child : children) {
						try {
							String filename = child.getName();
							if (child.isFile()) {
								if (filename.contains("."))
									filename = filename.split("\\.")[0];
							}
							String[] tokens = filename.split(" ");
							String lastToken = tokens[tokens.length - 1];
							long timeMade = Long.parseLong(lastToken);

							if (timeMade >= 0) {
								long age = System.currentTimeMillis() - timeMade;
								if (age >= deleteAfter) {
									deleteFile(child);
									deleted.add(age);
								}
							}
						} catch (Exception e) {
							// Ignore file as it doesn't follow the backup format.
						}
					}
				}
			}

			if (deleted.size() > 0) {
				System.out.println(String.format("[Towny] Deleting %d Old Backups (%s).", deleted.size(), (deleted.size() > 1 ? String.format("%d-%d days old", TimeUnit.MILLISECONDS.toDays(deleted.first()), TimeUnit.MILLISECONDS.toDays(deleted.last())) : String.format("%d days old", TimeUnit.MILLISECONDS.toDays(deleted.first())))));
			}
		}
	}

	public synchronized static void deleteUnusedFiles(File residentDir, Set<String> fileNames) {

		synchronized (residentDir) {

			int count = 0;

			if (residentDir.isDirectory()) {
				File[] children = residentDir.listFiles();
				if (children != null) {
					for (File child : children) {
						try {
							String filename = child.getName();
							if (child.isFile()) {
								if (filename.contains(".txt"))
									filename = filename.split("\\.txt")[0];

								// Delete the file if there is no matching resident.
								if (!fileNames.contains(filename.toLowerCase())) {
									deleteFile(child);
									count++;
								}
							}

						} catch (Exception ignored) {}
					}

					if (count > 0) {
						System.out.println(String.format("[Towny] Deleted %d old files.", count));
					}
				}
			}
		}

	}
	
	@Deprecated
	public static String fileSeparator() {

		return System.getProperty("file.separator");
	}
}
