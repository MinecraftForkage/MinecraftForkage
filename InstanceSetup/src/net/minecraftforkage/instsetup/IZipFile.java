package net.minecraftforkage.instsetup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IZipFile {
	/**
	 * Returns a collection of the paths of files and directories in this zip file.
	 * Directory paths end in a slash ('/'). File paths do not.
	 */
	public Iterable<String> getFileNames() throws IOException;
	
	/**
	 * Checks whether the zip file contains an entry with the given path.
	 * To test for a directory, the path must end in a slash ('/').
	 */
	public boolean doesPathExist(String path) throws IOException;
	
	/**
	 * Returns an input stream reading from the ZIP entry with the specified path.
	 * Throws an IOException if the path does not exist or is a directory.
	 */
	public InputStream read(String path) throws IOException;
	
	/**
	 * Returns an output stream writing to the ZIP entry with the specified path.
	 * If an entry is simultaneously read and written to, the result is unspecified.
	 * Throws an IOException if the path is a directory path.
	 * Parent directories of the given path are automatically created
	 * (at an unspecified time before or during the closure of the returned stream).
	 */
	public OutputStream write(String path) throws IOException;
	
	/**
	 * Deletes the entry with the given path.
	 * To delete a directory, the path must end with a slash ('/').
	 * Throws IOException if the path does not exist.
	 */
	public void delete(String path) throws IOException;
	
	/**
	 * Creates a directory entry if it does not already exist.
	 * The path must end with a slash ('/').
	 * Parent directories, if any, are automatically created if they
	 * do not already exist.
	 */
	public void createDirectory(String path) throws IOException;
}
