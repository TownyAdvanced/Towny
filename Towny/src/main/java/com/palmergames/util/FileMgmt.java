package com.palmergames.util;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.db.TownyDatabaseHandler;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class FileMgmt {
	
	private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private static final Lock readLock = readWriteLock.readLock();
	private static final Lock writeLock = readWriteLock.writeLock();
	
	/**
	 * Checks a folderPath to see if it exists, if it doesn't it will attempt
	 * to create the folder at the designated path.
	 *
	 * @param folderPath {@link String} containing a path to a folder.
	 * @return True if the folder exists or if it was successfully created.
	 */
	public static boolean checkOrCreateFolder(String folderPath) {
		File file = new File(folderPath);
		
		if (file.exists() || file.isDirectory()) {
			return true;
		}
		
		return newDir(file);
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

		if (file.exists()) {
			return true;
		}

		return newFile(file);
	}
	
	private static boolean newDir(File dir) {
		try {
			writeLock.lock();
			return dir.mkdirs();
		} finally {
			writeLock.unlock();
		}
	}
	
	private static boolean newFile(File file) {
		try {
			writeLock.lock();
			return file.createNewFile();
		} catch (IOException e) {
			return false;
		} finally {
			writeLock.unlock();
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
		try {
			writeLock.lock();
			if (sourceLocation.isDirectory()) {
				if (!targetLocation.exists())
					targetLocation.mkdir();

				String[] children = sourceLocation.list();
				for (String aChildren : children)
					copyDirectory(new File(sourceLocation, aChildren), new File(targetLocation, aChildren));
			} else {
				Files.copy(sourceLocation.toPath(), targetLocation.toPath());
			}
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Write a list to a file, terminating each line with a system specific new line.
	 * 
	 * @param source - Data source
	 * @param targetLocation - Target location on Filesystem
	 * @return true on success, false on IOException
	 */
	public static boolean listToFile(Collection<String> source, String targetLocation) {
		try {
			writeLock.lock();
			try {
				Files.write(Path.of(targetLocation), source);
				return true;
			} catch (IOException e) {
				Towny.getPlugin().getLogger().log(Level.WARNING, "An exception occurred while writing to " + targetLocation, e);
				return false;
			}
		} finally {
			writeLock.unlock();
		}
	}

	// move a file to a sub directory
	public static void moveFile(File sourceFile, String targetLocation) {
		try {
			writeLock.lock();
			if (sourceFile.isFile()) {
				// check for an already existing file of that name
				File f = new File((sourceFile.getParent() + File.separator + targetLocation + File.separator + sourceFile.getName()));
				if ((f.exists() && f.isFile()))
					f.delete();
				// Move file to new directory
				sourceFile.renameTo(new File((sourceFile.getParent() + File.separator + targetLocation), sourceFile.getName()));
			}
		} finally {
			writeLock.unlock();
		}
	}
	
	public static void moveTownBlockFile(File sourceFile, String targetLocation, String townDir) {
		try {
			writeLock.lock();
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
		} finally {
			writeLock.unlock();
		}
	}
	
	public static String getFileTimeStamp() {
		long t = System.currentTimeMillis();
		return new SimpleDateFormat("yyyy-MM-dd HH-mm").format(t);
	}
	
	public static void tar(File destination, File... sources) throws IOException {
		try {
			readLock.lock();
			try (TarArchiveOutputStream archive = new TarArchiveOutputStream(new GzipCompressorOutputStream(new FileOutputStream(destination)))) {
				archive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
				for (File sourceFile : sources) {
					Path source = sourceFile.toPath();
					try (Stream<Path> files = Files.walk(source)) {
						for (Path path : files.toList()) {
							if (Files.isDirectory(path))
								continue;
							
							try (InputStream fis = Files.newInputStream(path)) {
								TarArchiveEntry entry_1 = new TarArchiveEntry(path, source.getParent().relativize(path).toString());
								
								archive.putArchiveEntry(entry_1);
								IOUtils.copy(fis, archive);
								archive.closeArchiveEntry();
							}
						}
					}
				}
			}
		} finally {
			readLock.unlock();
		}
	}


	/**
	 * Zip a given file into the given path.
	 * 
	 * @param file - File to zip.
	 * @param path - Path to put file.
	 */
	public static void zipFile(File file, String path) {
		
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(path), StandardCharsets.UTF_8)) {
			writeLock.lock();
			byte[] buffer = new byte[2056];  // Buffer with which to write the bytes of the zip file.
			zos.putNextEntry(new ZipEntry(file.getName())); // Place file into zip.
			try (FileInputStream in = new FileInputStream(file)) { 
                int len;
                while ((len = in.read(buffer)) > 0) { // While there is data to write, write up to the buffer.
                    zos.write(buffer, 0, len);
                }
            }			
		} catch (IOException e) {
			Towny.getPlugin().getLogger().log(Level.WARNING, "An exception occurred while zipping up file " + file.getName(), e);
		} finally {
			writeLock.unlock();
		}
	}
	
	public static void zipDirectories(File destination, File... sourceFolders) throws IOException {
		try {
			readLock.lock();
			ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destination), StandardCharsets.UTF_8);
			for (File sourceFolder : sourceFolders)
				recursiveZipDirectory(sourceFolder, output);
			output.close();
		} finally {
			readLock.unlock();
		}
	}

	public static void recursiveZipDirectory(File sourceFolder, ZipOutputStream zipStream) throws IOException {
		try {
			readLock.lock();
			String[] dirList = sourceFolder.list();
			byte[] readBuffer = new byte[2156];
			int bytesIn;
			for (String aDirList : dirList) {
				File f = new File(sourceFolder, aDirList);
				if (f.isDirectory()) {
					recursiveZipDirectory(f, zipStream);
				} else if (f.isFile() && f.canRead()) {
					try (FileInputStream input = new FileInputStream(f)) {
						ZipEntry anEntry = new ZipEntry(f.getPath());
						zipStream.putNextEntry(anEntry);
						while ((bytesIn = input.read(readBuffer)) != -1)
							zipStream.write(readBuffer, 0, bytesIn);
					}
				}
			}
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Delete file, or if path represents a directory, recursively
	 * delete it's contents beforehand.
	 * 
	 * @param file - {@link File} to delete
	 */
	public static void deleteFile(File file) {
		try {
			writeLock.lock();
			if (file.isDirectory()) {
				File[] children = file.listFiles();
				if (children != null) {
					for (File child : children)
						deleteFile(child);
				}
				children = file.listFiles();
				if (children == null || children.length == 0) {
					if (!file.delete())
						Towny.getPlugin().getLogger().warning("Error: Could not delete folder: " + file.getPath());
				}
			} else if (file.isFile()) {
				if (!file.delete())
					Towny.getPlugin().getLogger().warning("Error: Could not delete file: " + file.getPath());
			}
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Delete child files/folders of backupsDir with a filename ending
	 * in milliseconds that is older than deleteAfter milliseconds in age.
	 * 
	 * @param backupsDir - {@link File} path to backupsDir
	 * @param deleteAfter - Maximum age of files, in milliseconds
	 * @return Whether old backups were successfully deleted.   
	 */
	public static boolean deleteOldBackups(File backupsDir, long deleteAfter) {
		try {
			writeLock.lock();

			TreeSet<Long> deleted = new TreeSet<>();
			if (!backupsDir.isDirectory())
				return false;
			
			File[] children = backupsDir.listFiles();
			if (children == null)
				return true;
			
			for (File child : children) {
				if (child.isDirectory())
					continue;
				
				String fileName = child.getName();
							
				long timeMade;
							
				try {
					timeMade = TownyDatabaseHandler.BACKUP_DATE_FORMAT.parse(fileName).getTime();
				} catch (ParseException pe) {
					try {
						// The file name does not match the new date format, attempt legacy format
						String[] tokens = fileName.split("\\.")[0].split(" ");
						String lastToken = tokens[tokens.length - 1];
						timeMade = Long.parseLong(lastToken);
					} catch (Exception e) {
						// Ignore file as it doesn't follow the backup format.
						Towny.getPlugin().getLogger().warning("File '" + fileName + "' in the backup folder does not match any format recognized by Towny, it will not be automatically deleted.");
						continue;
					}
				}

				if (timeMade >= 0) {
					long age = System.currentTimeMillis() - timeMade;
					if (age >= deleteAfter) {
						deleteFile(child);
						deleted.add(age);
					}
				}
			}

			if (!deleted.isEmpty()) {
				Towny.getPlugin().getLogger().info(String.format("Deleting %d Old Backups (%s).", deleted.size(), (deleted.size() > 1 ? String.format("%d-%d days old", TimeUnit.MILLISECONDS.toDays(deleted.first()), TimeUnit.MILLISECONDS.toDays(deleted.last())) : String.format("%d days old", TimeUnit.MILLISECONDS.toDays(deleted.first())))));
			}

			return true;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Function which reads from a resident, town, nation, townyobject file, returning a hashmap. 
	 *
	 * @param file - File from which the HashMap will be made.
	 * @return HashMap - Used for loading keys and values from object files. 
	 */
	public static HashMap<String, String> loadFileIntoHashMap(File file) {
		
		try {
			readLock.lock();
			HashMap<String, String> keys = new HashMap<>();
			try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					if (line.isEmpty() || line.startsWith("#")) continue;
					int eq = line.indexOf('=');
					if (eq < 0) continue;
					String key = line.substring(0, eq).trim();
					String value = line.substring(eq + 1);
					keys.put(key, value);
				}
			} catch (IOException e) {
				Towny.getPlugin().getLogger().log(Level.WARNING, "An exception occurred while reading file " + file.getName(), e);
			}
			return keys;
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * Method to save a PlotBlockData object to disk.
	 * 
	 * @param data PlotBlockData object containing a plot snapshot used for the unclaim-on-revert feature.
	 * @param file Directory to save the PlotBlockData to.
	 * @param path Zip file location to save to.
	 */
	public static void savePlotData(PlotBlockData data, File file, String path) {
		checkOrCreateFolder(file.getPath()); // Make the folder if it doesn't exist.
		try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(path), StandardCharsets.UTF_8)) {
			writeLock.lock();
			output.putNextEntry(new ZipEntry(data.getX() + "_" + data.getZ() + "_" + data.getSize() + ".data")); // Create x_z_size.data file inside of .zip
			try (DataOutputStream fout = new DataOutputStream(output)) {
				// Data version goes first.
				fout.write("VER".getBytes(StandardCharsets.UTF_8));
				fout.write(data.getVersion());
				// Write the plot height (who knows Mojang might change it a fourth time.)
				fout.writeInt(data.getHeight());
				// Write the plot min height (who knows Mojang might change it a second time.)
				fout.writeInt(data.getMinHeight());
				// Write the actual blocks with their BlockData included.
				for (String block : new ArrayList<>(data.getBlockList()))
					fout.writeUTF(block);
			}
		} catch (IOException e1) {
			Towny.getPlugin().getLogger().log(Level.WARNING, "An exception occurred while saving plot data to " + file.getName(), e1);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * @param path The file path
	 * @return The file name for the given path (minus file extension)
	 */
	@NotNull
	public static String getFileName(final @NotNull Path path) {
		String fileName = path.getFileName().toString();
		if (fileName.contains("."))
			fileName = fileName.substring(0, fileName.lastIndexOf("."));

		return fileName;
	}
	
	@NotNull
	public static String getExtension(final @NotNull Path path) {
		String fileName = path.getFileName().toString();
		
		final int index = fileName.lastIndexOf(".");
		if (index == -1)
			return "";
		
		return fileName.substring(index + 1);
	}
}
