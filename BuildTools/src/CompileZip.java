import installer.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;


public class CompileZip {
	
	static ZipOutputStream zipOut;
	
	static class ZipFileManager implements JavaFileManager {
		
		ZipFile zf;
		Collection<ZipFile> others = new ArrayList<>();
		List<ZipEntry> entries = new ArrayList<>();
		List<URL> otherURLs = new ArrayList<>();
		List<File> otherFiles = new ArrayList<>();
		JavaFileManager parent;
		
		ZipFileManager(File file, File libsDir, JavaFileManager parent) throws IOException {
			zf = new ZipFile(file);
			
			for(Enumeration<? extends ZipEntry> entries_enum = zf.entries(); entries_enum.hasMoreElements();)
				entries.add(entries_enum.nextElement());
			
			this.parent = parent;
			
			walkLibs(libsDir);
			
			String s="";
			for(File f : otherFiles)
				s += ";" + f.getAbsolutePath();
			if(!parent.handleOption("-cp", Arrays.asList(s.substring(1)).iterator()))
				throw new RuntimeException(s);
		}
		
		private void walkLibs(File dir) throws IOException {
			if(dir.isFile()) {
				if(!dir.getName().endsWith(".jar"))
					return;
				//System.err.println(dir);
				others.add(new ZipFile(dir));
				otherURLs.add(dir.toURI().toURL());
				otherFiles.add(dir);
			} else
				for(File sub : dir.listFiles())
					walkLibs(sub);
		}
		
		static class OutputClassJFO implements JavaFileObject {
			String className;
			public OutputClassJFO(String name) {
				this.className = name;
			}
			@Override
			public URI toUri() {
				try {
					return new URI("CompileZipOutput:"+className);
				} catch (URISyntaxException e) {
					throw new AssertionError(e);
				}
			}
			@Override
			public String getName() {
				throw new RuntimeException("getName "+className);
			}
			@Override
			public InputStream openInputStream() throws IOException {
				throw new RuntimeException("openInputStream "+className);
			}
			@Override
			public OutputStream openOutputStream() throws IOException {
				return new ByteArrayOutputStream() {
					@Override
					public void close() throws IOException {
						super.close();
						
						zipOut.putNextEntry(new ZipEntry(className.replace(".","/")+".class"));
						zipOut.write(toByteArray());
						zipOut.closeEntry();
					}
				};
			}
			@Override
			public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
				throw new RuntimeException("openReader "+className);
			}
			@Override
			public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
				throw new RuntimeException("getCharContent "+className);
			}
			@Override
			public Writer openWriter() throws IOException {
				throw new RuntimeException("openWriter "+className);
			}
			@Override
			public long getLastModified() {
				throw new RuntimeException("getLastModified "+className);
			}
			@Override
			public boolean delete() {
				return false;
			}
			@Override
			public Kind getKind() {
				return Kind.CLASS;
			}
			@Override
			public boolean isNameCompatible(String simpleName, Kind kind) {
				return true;
			}
			@Override
			public NestingKind getNestingKind() {
				throw new RuntimeException("getNestingKind "+className);
			}
			@Override
			public Modifier getAccessLevel() {
				throw new RuntimeException("getAccessLevel "+className);
			}
		}
		
		static class ZipFileObject implements JavaFileObject, FileObject {

			ZipFile zf;
			ZipEntry ze;
			Kind kind;
			public ZipFileObject(ZipFile zf, String path, Kind kind) {
				this.zf = zf;
				this.kind = kind;
				ze = zf.getEntry(path);
				if(ze == null)
					throw new RuntimeException(path+" not found");
			}

			@Override
			public URI toUri() {
				try {
					return new URI("ZipFileObject:///"+ze.getName());
				} catch(URISyntaxException e) {
					throw new AssertionError(e);
				}
			}

			@Override
			public String getName() {
				return ze.getName();
			}

			@Override
			public InputStream openInputStream() throws IOException {
				return zf.getInputStream(ze);
			}

			@Override
			public OutputStream openOutputStream() throws IOException {
				throw new RuntimeException("openOutputStream");
			}

			@Override
			public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
				// ignoreEncodingErrors ignored
				return new InputStreamReader(openInputStream(), StandardCharsets.UTF_8);
			}

			@Override
			public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
				StringBuilder rv = new StringBuilder();
				char[] buffer = new char[32768];
				try (Reader r = openReader(ignoreEncodingErrors)) {
					while(true) {
						int read = r.read(buffer);
						if(read < 0)
							break;
						rv.append(buffer, 0, read);
					}
				}
				return rv;
			}

			@Override
			public Writer openWriter() throws IOException {
				throw new RuntimeException("openWriter");
			}

			@Override
			public long getLastModified() {
				throw new RuntimeException("getLastModified");
			}

			@Override
			public boolean delete() {
				throw new RuntimeException("delete");
			}

			@Override
			public Kind getKind() {
				return kind;
			}

			@Override
			public boolean isNameCompatible(String simpleName, Kind kind) {
				if(kind != this.kind)
					return false;
				String name = ze.getName();
				if(name.contains("/")) name = name.substring(name.lastIndexOf('/')+1);
				if(name.contains(".")) name = name.substring(0, name.lastIndexOf('.'));
				return name.equals(simpleName);
			}

			@Override
			public NestingKind getNestingKind() {
				throw new RuntimeException("getNestingKind");
			}

			@Override
			public Modifier getAccessLevel() {
				throw new RuntimeException("getAccessLevel");
			}
			
			@Override
			public String toString() {
				return ze.getName();
			}
			
		}

		@Override
		public int isSupportedOption(String option) {
			return parent.isSupportedOption(option);
		}

		@Override
		public ClassLoader getClassLoader(Location location) {
			//System.err.println("getClassLoader "+location);
			return new URLClassLoader(new URL[0], parent.getClassLoader(location)) {{
				for(URL url : otherURLs)
					addURL(url);
			}};
		}

		@Override
		public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
			//System.err.println("list "+location+" "+packageName+" "+kinds+" "+recurse);
			
			List<JavaFileObject> result = new ArrayList<>();
			for(JavaFileObject pJFO : parent.list(location, packageName, kinds, recurse))
				result.add(pJFO);
			
			packageName = packageName.replace(".", "/");
			
			if(kinds.contains(Kind.CLASS)) {
				for(ZipFile zf : others) {
					for(Enumeration<? extends ZipEntry> entries_enum = zf.entries(); entries_enum.hasMoreElements();) {
						ZipEntry ze = entries_enum.nextElement();
						
						if(ze.getName().startsWith(packageName) && ze.getName().endsWith(".class"))
							if(recurse || ze.getName().indexOf('/', packageName.length()+1) < 0)
								result.add(new ZipFileObject(zf, ze.getName(), Kind.CLASS));
					}
				}
			}
			
			if(kinds.contains(Kind.SOURCE)) {
				for(Enumeration<? extends ZipEntry> entries_enum = zf.entries(); entries_enum.hasMoreElements();) {
					ZipEntry ze = entries_enum.nextElement();
					
					if(ze.getName().startsWith(packageName) && ze.getName().endsWith(".java"))
						if(recurse || ze.getName().indexOf('/', packageName.length()+1) < 0)
							result.add(new ZipFileObject(zf, ze.getName(), Kind.SOURCE));
				}
			}
			
			//if(packageName.equals("java"))
				//throw new RuntimeException(result.toString());
			
			return result;
		}

		@Override
		public String inferBinaryName(Location location, JavaFileObject file) {
			if(file instanceof ZipFileObject) {
				String result = ((ZipFileObject)file).ze.getName().replaceFirst("\\.class$", "").replaceFirst("\\.java$", "");
				//if(result.contains("java"))
				//	throw new RuntimeException(result);
				return result;
			}
			//throw new RuntimeException("inferBinaryName");
			return parent.inferBinaryName(location, file);
		}

		@Override
		public boolean isSameFile(FileObject a, FileObject b) {
			if(a instanceof ZipFileObject)
				if(b instanceof ZipFileObject)
					return ((ZipFileObject) a).ze == ((ZipFileObject) b).ze;
				else
					return false;
			if(b instanceof ZipFileObject)
				return false;
			return parent.isSameFile(a, b);
		}

		@Override
		public boolean handleOption(String current, Iterator<String> remaining) {
			//throw new RuntimeException("handleOption "+current);
			return parent.handleOption(current, remaining);
		}

		@Override
		public boolean hasLocation(Location location) {
			return location == StandardLocation.SOURCE_PATH || parent.hasLocation(location);
		}

		@Override
		public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
			if(kind != Kind.SOURCE)
				return parent.getJavaFileForInput(location, className, kind);
			return (JavaFileObject)getFileForInput(location, "", className.replace(".", "/")+".java");
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
			if(kind != Kind.CLASS || location != StandardLocation.CLASS_OUTPUT)
				return null;
			return new OutputClassJFO(className);
		}

		@Override
		public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
			if(location != StandardLocation.SOURCE_PATH)
				return null;
			return new ZipFileObject(zf, packageName.equals("") ? relativeName : packageName.replace(".","/")+"/"+relativeName, Kind.SOURCE);
		}

		@Override
		public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
			throw new RuntimeException("gffo");
		}

		@Override
		public void flush() throws IOException {
			parent.flush();
		}

		@Override
		public void close() throws IOException {
			parent.close();
			zf.close();
		}
		
	}
	
	public static void main(String[] args) {
		if(args.length != 2) {
			System.err.println("Usage: java CompileDirTree sources.zip libsDir > output.jar");
			System.exit(1);
		}
		
		try {
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			
			JavaFileManager standardFM = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
			
			ZipFileManager fileManager = new ZipFileManager(new File(args[0]), new File(args[1]), standardFM);
			
			List<JavaFileObject> compilationFileObjects = new ArrayList<>();
			for(ZipEntry ze : fileManager.entries) {
				if(ze.getName().endsWith(".java")) {
					String name = ze.getName();
					name = name.substring(0, name.length() - 5);
					name = name.replace("/", ".");
					compilationFileObjects.add(fileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, name, Kind.SOURCE));
				}
			}
			
			try (ZipOutputStream zipOut = new ZipOutputStream(System.out)) {
				CompileZip.zipOut = zipOut;
				
				for(ZipEntry ze : fileManager.entries) {
					if(!ze.getName().endsWith(".java")) {
						try (InputStream in = fileManager.zf.getInputStream(ze)) {
							zipOut.putNextEntry(new ZipEntry(ze.getName()));
							Utils.copyStream(in, zipOut);
							zipOut.closeEntry();
						}
					}
				}
				
				CompilationTask task = compiler.getTask(null, fileManager, null, null, null, compilationFileObjects);
			
				if(!task.call()) {
					System.err.println("Compilation failed");
					System.exit(1);
				}
			}
			
		} catch(Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
}
