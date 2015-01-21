package net.minecraftforkage.instsetup;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public abstract class AbstractZipFile {
	/**
	 * Returns a collection of the paths of files and directories in this zip file.
	 * Directory paths end in a slash ('/'). File paths do not.
	 */
	public abstract Iterable<String> getFileNames() throws IOException;
	
	/**
	 * Checks whether the zip file contains an entry with the given path.
	 * To test for a directory, the path must end in a slash ('/').
	 */
	public abstract boolean doesPathExist(String path) throws IOException;
	
	/**
	 * Returns an input stream reading from the ZIP entry with the specified path.
	 * Throws an IOException if the path does not exist or is a directory.
	 */
	public abstract InputStream read(String path) throws IOException;
	
	/**
	 * Returns an output stream writing to the ZIP entry with the specified path.
	 * If an entry is simultaneously read and written to, the result is unspecified.
	 * Throws an IOException if the path is a directory path.
	 * Parent directories of the given path are automatically created
	 * (at an unspecified time before or during the closure of the returned stream).
	 */
	public abstract OutputStream write(String path) throws IOException;
	
	/**
	 * Deletes the entry with the given path.
	 * To delete a directory, the path must end with a slash ('/').
	 * Throws IOException if the path does not exist.
	 */
	public abstract void delete(String path) throws IOException;
	
	/**
	 * Creates a directory entry if it does not already exist.
	 * The path must end with a slash ('/').
	 * Parent directories, if any, are automatically created if they
	 * do not already exist.
	 */
	public abstract void createDirectory(String path) throws IOException;
	
	
	
	
	/* ===== UTF-8 UTILITY METHODS ===== */
	
	/**
	 * Similar to {@link #read(String)}, but returns a Reader.
	 * Characters are decoded as UTF-8.
	 */
	public Reader readUTF8(String path) throws IOException {
		return new InputStreamReader(read(path), StandardCharsets.UTF_8);
	}
	
	/**
	 * Similar to {@link #write(String)}, but returns a Writer.
	 * Characters are encoded as UTF-8.
	 */
	public Writer writeUTF8(String path) throws IOException {
		return new OutputStreamWriter(write(path), StandardCharsets.UTF_8);
	}
	
	
	
	
	/* ===== JSON UTILITY METHODS ===== */
	
	private static final Gson DEFAULT_GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
	
	/**
	 * Reads a JSON file into an object tree, using {@link Gson}
	 * with the default settings.
	 */
	public <T> T readGSON(String path, Class<T> classOfT) throws IOException {
		return readGSON(path, classOfT, DEFAULT_GSON);
	}
	
	/**
	 * Reads a JSON file into an object tree, using {@link Gson}
	 * with the default settings.
	 */
	public <T> T readGSON(String path, Type typeOfT) throws IOException {
		return readGSON(path, typeOfT, DEFAULT_GSON);
	}
	
	/**
	 * Reads a JSON file into an object tree, using a customized {@link Gson} instance.
	 */
	public <T> T readGSON(String path, Class<T> classOfT, Gson gsonInstance) throws IOException {
		try (Reader in = readUTF8(path)) {
			return gsonInstance.fromJson(in, classOfT);
		} 
	}
	
	/**
	 * Reads a JSON file into an object tree, using a customized {@link Gson} instance.
	 */
	public <T> T readGSON(String path, Type typeOfT, Gson gsonInstance) throws IOException {
		try (Reader in = readUTF8(path)) {
			return gsonInstance.fromJson(in, typeOfT);
		}
	}
	
	/**
	 * Writes an object tree into a JSON file, using {@link Gson}
	 * with the default settings.
	 */
	public void writeGSON(String path, Object object) throws IOException {
		writeGSON(path, object, DEFAULT_GSON);
	}
	
	/**
	 * Writes an object tree into a JSON file, using a customized {@link Gson} instance.
	 */
	public void writeGSON(String path, Object object, Gson gsonInstance) throws IOException {
		try (Writer out = writeUTF8(path)) {
			gsonInstance.toJson(object, out);
		}
	}

	/**
	 * If the file does not exist, write the given list to it as a JSON array.
	 * If the file does exist, it must contain a JSON array. The elements in the given list
	 * will be appended to the array.
	 */
	public synchronized void appendGSONArray(String path, List<?> list) throws IOException {

		List<Object> objects = new ArrayList<Object>();
		
		if(doesPathExist(path))
			for(JsonElement existingElement : readGSON(path, JsonArray.class))
				objects.add(existingElement);
		
		objects.addAll(list);
		
		writeGSON(path, objects);
	}
	
	
	
	/* ===== PROPERTIES FILE UTILITY METHODS ===== */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, String> readProperties(String path) throws IOException {
		Properties p = new Properties();
		try (InputStream in = read(path)) {
			p.load(in);
		}
		return (Map<String, String>)(Map)p;
	}
	
}
