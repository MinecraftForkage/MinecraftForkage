package net.minecraftforkage.instsetup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.minecraftforkage.instsetup.depsort.DependencySortedObject;

/**
 * Allows mods to transform the baked JAR before it loads.
 * 
 * JarTransformer classes are located through {@link java.util.ServiceLoader}.
 * JarTransformer classes will not be located in mods that do not identify themselves
 * as containing instance setup components. See the documentation for this package for more details.
 */
public abstract class JarTransformer implements DependencySortedObject {
	
	@Override
	public String getDependencies() {
		return "";
	}
	
	@Override
	public abstract String getID();
	
	/**
	 * Main entry point for a JAR transformer.
	 */
	public void transform(IZipFile zipFile) throws Exception {
	}
	
	/**
	 * Alternate main entry point for a JAR transformer.
	 * 
	 * You are recommended to override {@link #transform(IZipFile)} instead, unless
	 * you specifically require Zip4J, as it is possible that future versions of Minecraft
	 * Forkage will not use Zip4J.
	 */
	public void transform(ZipFile zipFile) throws Exception {
		transform(new ZipFileAdapter(zipFile));
	}
	
	static class ZipFileAdapter implements IZipFile {

		private final ZipFile wraps;
		private Set<String> filenames;
		
		public ZipFileAdapter(ZipFile zipFile) {
			this.wraps = zipFile;
		}
		
		@Override
		public synchronized Iterable<String> getFileNames() throws IOException {
			if(filenames == null) {
				filenames = new HashSet<>();
				try {
					for(Object header : wraps.getFileHeaders())
						filenames.add(((FileHeader)header).getFileName());
				} catch(ZipException e) {
					throw new IOException(e);
				}
				filenames = Collections.unmodifiableSet(filenames);
			}
			return filenames;
		}
		
		@Override
		public synchronized InputStream read(String path) throws IOException {
			if(filenames == null)
				getFileNames();
			if(!filenames.contains(path))
				throw new IOException("No such path in zip file: '" + path + "'");
			try {
				return wraps.getInputStream(wraps.getFileHeader(path));
			} catch (ZipException e) {
				throw new IOException("Failed to open '"+path+"'", e);
			}
		}
		
		@Override
		public OutputStream write(final String path) throws IOException {
			if(path.endsWith("/") || path.endsWith(""))
				throw new IOException("Invalid path: '"+path+"'");
			OutputStream stream = new ByteArrayOutputStream() {
				@Override
				public void close() throws IOException {
					super.close();
					
					synchronized(ZipFileAdapter.this) {
						// create parent directory
						{
							int i = path.lastIndexOf('/');
							if(i >= 0)
								createDirectory(path.substring(0, i+1));
						}
						
						ZipParameters params = new ZipParameters();
						params.setFileNameInZip(path);
						params.setSourceExternalStream(true);
						try {
							wraps.addStream(new ByteArrayInputStream(buf, 0, count), params);
						} catch(ZipException e) {
							throw new IOException("While writing '"+path+"'", e);
						}
					}
				}
			};
			return stream;
		}

		@Override
		public synchronized boolean doesPathExist(String path) throws IOException {
			if(filenames == null)
				getFileNames();
			return filenames.contains(path);
		}

		@Override
		public synchronized void delete(String path) throws IOException {
			if(!doesPathExist(path))
				throw new IOException("Path doesn't exist: '" + path + "'");
			
			try {
				wraps.removeFile(path);
			} catch(ZipException e) {
				throw new IOException(e);
			}
		}

		@Override
		public synchronized void createDirectory(String path) throws IOException {
			if(!path.endsWith("/"))
				throw new IOException("Not a directory path: '" + path + "'");
			
			if(doesPathExist(path))
				return;
			
			// create parent
			{
				int i = path.lastIndexOf('/', path.length() - 2);
				if(i > 0)
					createDirectory(path.substring(0, i+1));
			}
			
			ZipParameters parameters = new ZipParameters();
			parameters.setFileNameInZip(path);
			try {
				wraps.addStream(new NullStream(), parameters);
			} catch(ZipException e) {
				throw new IOException(e);
			}
		}
		
		private static class NullStream extends InputStream {
			@Override
			public int read() throws IOException {
				return -1;
			}
		}
		
	}
}
