package net.minecraftforkage.instsetup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class ZipFileSystemAdapter extends AbstractZipFile implements Closeable {

	final FileSystem fs;
	final Path root;
	
	public ZipFileSystemAdapter(FileSystem fs) throws IOException {
		this.fs = fs;
		
		try {
			Iterator<Path> roots = fs.getRootDirectories().iterator();
			if(!roots.hasNext()) throw new RuntimeException("filesystem has no root directories");
			this.root = roots.next();
			if(roots.hasNext()) throw new RuntimeException("filesystem has more than one root directory");
		
			filenames = new HashSet<>();
			
			Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					filenames.add(nioPathToString(dir)+"/");
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					filenames.add(nioPathToString(file));
					return FileVisitResult.CONTINUE;
				}
			});
			
			filenamesUnmod = Collections.unmodifiableSet(filenames);
		
		} catch(Throwable e) {
			try {
				fs.close();
			} catch(Throwable e2) {
				e.addSuppressed(e2);
			}
			throw e;
		}
	}

	@Override
	public synchronized void close() throws IOException {
		fs.close();
	}
	
	Set<String> filenames;
	Set<String> filenamesUnmod;
	
	private String nioPathToString(Path path) {
		return root.relativize(path).toString().replace(File.separator, "/");
	}
	
	private Path getNioPath(String path) {
		Path p = fs.getPath(File.separator + path.replace("/", File.separator));
		String canonical = nioPathToString(p);
		if(!canonical.equals(path) && !(canonical+"/").equals(path))
			throw new IllegalArgumentException("not canonical path format: " + path + " (should be " + canonical + ")");
		
		return p;
	}

	@Override
	public Iterable<String> getFileNames() throws IOException {
		return filenamesUnmod;
	}

	@Override
	public synchronized boolean doesPathExist(String path) throws IOException {
		return filenames.contains(path);
	}

	@Override
	public synchronized InputStream read(String path) throws IOException {
		if(path.endsWith("/"))
			throw new IllegalArgumentException("can't read a directory: " + path);
		return new BufferedInputStream(Files.newInputStream(getNioPath(path), StandardOpenOption.READ));
	}

	@Override
	public synchronized OutputStream write(String path) throws IOException {
		if(path.endsWith("/"))
			throw new IllegalArgumentException("can't write a directory: " + path);
		
		Path path2 = getNioPath(path);
		filenames.add(nioPathToString(path2));
		return new BufferedOutputStream(Files.newOutputStream(path2, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
	}

	@Override
	public synchronized void delete(String path) throws IOException {
		Path path2 = getNioPath(path);
		if(path.endsWith("/")) {
			if(!Files.isDirectory(path2))
				throw new IllegalArgumentException("Not a directory: " + path);
		} else {
			if(Files.isDirectory(path2))
				throw new IllegalArgumentException("Not a file: " + path);
		}
		Files.delete(path2);
		
		if(path.endsWith("/"))
			filenames.remove(nioPathToString(path2) + "/");
		else
			filenames.remove(nioPathToString(path2));
	}

	@Override
	public synchronized void createDirectory(String path) throws IOException {
		if(!path.endsWith("/"))
			throw new IllegalArgumentException("Not a directory path: " + path);
		
		createDirectory(getNioPath(path));
	}
	
	private void createDirectory(Path path) throws IOException {
		if(Files.isDirectory(path))
			return;
		
		Path parent = path.getParent();
		if(parent != null)
			createDirectory(parent);
		
		Files.createDirectory(path);
		filenames.add(nioPathToString(path) + "/");
	}
	

}
