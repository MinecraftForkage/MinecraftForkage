package net.minecraft.launchwrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * This class is specified by Mojang's launchwrapper.
 * 
 * Note: This ClassLoader <b>must</b> define packages and code sources, or some mods
 * will crash.
 * Also note: packages must be defined before transformers are called.
 * The CodeSource URL is the URL to the actual class file, not the JAR or directory.
 * The URL for packages is the JAR or directory.
 */
public class LaunchClassLoader extends URLClassLoader {
	private ArrayList<URL> sources;
	private List<URL> sources_unmod;
	private List<String> classloaderExclusions = new ArrayList<String>(20);
	private List<String> transformerExclusions = new ArrayList<String>(20);
	private ArrayList<IClassTransformer> transformers = new ArrayList<IClassTransformer>(0);
	private ClassLoader parent;
	
	private File debugDir = null;/*new File("CLASSLOADER_DEBUG");
	{
		debugDir.mkdirs();
	}*/
	
	// XXX BACKCOMPAT with logisticspipes.asm.LogisticsPipesClassInjector
	private Map<String, byte[]> resourceCache = new HashMap<String, byte[]>();
	
	// XXX BACKCOMPAT with codechicken.core.asm.DelegatedTransformer
	@SuppressWarnings("unused")
	private Map<String, Class<?>> cachedClasses = new AbstractMap<String, Class<?>>() {
		public Class<?> put(String key, Class<?> value) {
			return null;
		}
		@Override
		public Set<Map.Entry<String, Class<?>>> entrySet() {
			return Collections.emptySet();
		}
	};
	
	/**
	 * Constructs a LaunchClassLoader which will initially load from the specified URLs.
	 * (See {@link URLClassLoader})
	 * 
	 * @param urls An array of the URLs to load classes from.
	 */
	public LaunchClassLoader(URL[] urls) {
		super(urls, null);
		
		this.parent = LaunchClassLoader.class.getClassLoader();
		
		sources = new ArrayList<URL>(urls.length);
		for(URL url : urls)
			sources.add(url);
		sources.trimToSize();
		
		sources_unmod = Collections.unmodifiableList(sources);
		
		addClassLoaderExclusion("net.minecraft.launchwrapper.");
		addClassLoaderExclusion("java.");
		addTransformerExclusion("org.objectweb.asm.");
	}
	
	/**
	 * Creates a new instance of the class with the given name,
	 * transformer with the given class name.
	 * This method is specified by Mojang's launchwrapper.
	 */
	public void registerTransformer(String className) {
		addTransformerExclusion(className);
		IClassTransformer transformer;
		try {
			transformer = loadClass(className).asSubclass(IClassTransformer.class).newInstance();
		} catch(Exception e) {
			throw new RuntimeException("Failed to register transformer "+className, e);
		}
		transformers.add(transformer);
		transformers.trimToSize();
	}
	
	/**
	 * Adds a URL to load from. (See {@link URLClassLoader})
	 * 
	 * This method is specified by Mojang's launchwrapper.
	 * 
	 * @param url The URL to add.
	 */
	public void addURL(URL url) {
		super.addURL(url);
		sources.add(url);
		sources.trimToSize();
	}
	
	/**
	 * Returns an unmodifiable list of the URLs being loaded from.
	 * 
	 * This method is specified by Mojang's launchwrapper.
	 * 
	 * @return A list of the URLs classes are being loaded from.
	 */
	public List<URL> getSources() {
		return sources_unmod;
	}
	
	/**
	 * This method is specified by Mojang's launchwrapper.
	 */
	public List<IClassTransformer> getTransformers() {
		throw new UnsupportedOperationException();
	}
	/**
	 * Adds the given prefix to the classloader exclusion list.
	 * Any class that begins with a classloader exclusion prefix
	 * will not be loaded through this classloader - they will
	 * always be delegated to the parent.
	 * 
	 * This method is specified by Mojang's launchwrapper.
	 */
	public void addClassLoaderExclusion(String prefix) {
		classloaderExclusions.add(prefix);
	}
	
	/**
	 * Adds the given prefix to the transformer exclusion list.
	 * Any class that begins with a transformer exclusion prefix
	 * will not be transformed.
	 * 
	 * This method is specified by Mojang's launchwrapper.
	 */
	public void addTransformerExclusion(String prefix) {
		transformerExclusions.add(prefix);
	}
	
	private static String getClassFileName(String className) {
		return className.replace('.','/').concat(".class");
	}
	
	/**
	 * This method is specified by Mojang's launchwrapper.
	 * @throws IOException 
	 */
	public byte[] getClassBytes(String className) throws IOException {
		String resname = getClassFileName(className);
		InputStream stream = getResourceAsStream(resname);
		if(stream == null)
			throw new IOException("Not found: "+resname);
		try {
			ByteArrayOutputStream temp = new ByteArrayOutputStream(stream.available());
			byte[] buffer = new byte[4096];
			while(true) {
				int nread = stream.read(buffer);
				if(nread <= 0)
					break;
				temp.write(buffer, 0, nread);
			}
			return temp.toByteArray();
		} finally {
			stream.close();
		}
	}
	
	/**
	 * This method is specified by Mojang's launchwrapper.
	 */
	public void clearNegativeEntries(Set<String> entries) {
		//throw new UnsupportedOperationException();
	}
	
	/**
	 * This method is specified by Mojang's launchwrapper.
	 */
	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			if(isClassLoaderExcluded(name))
				return parent.loadClass(name);
			
			try {
				byte[] bytes;
				bytes = getClassBytes(name);
				
				String packageName;
				if(name.contains("."))
					packageName = name.substring(0, name.lastIndexOf('.'));
				else
					packageName = "";
				
				URL classURL = getResource(getClassFileName(name));
				String classURLString = classURL.toString();
				
				URL jarURL;
				if(classURLString.startsWith("jar:"))
					jarURL = new URL(classURLString.substring(4, classURLString.lastIndexOf('!')));
				else
					jarURL = classURL;
				
				if(getPackage(packageName) == null) {
					definePackage(packageName, new Manifest(), jarURL);
				}
				
				if(!isTransformerExcluded(name))
					bytes = getTransformedBytes(name, bytes);
				
				CodeSource codeSource = new CodeSource(jarURL, new CodeSigner[0]);
				
				return defineClass(name, bytes, 0, bytes.length, codeSource);
			} catch(Exception e) {
				throw new ClassNotFoundException(name, e);
			} catch(AssertionError e) {
				throw new ClassNotFoundException(name, e);
			}
		} catch(ClassNotFoundException e) {
			//e.printStackTrace();
			throw e;
		}
	}
	
	private byte[] getTransformedBytes(String name, byte[] bytes) {
		if(debugDir != null) {
			int count = 0;
			debugDumpClass(count++, name, bytes, null);
			byte[] old = Arrays.copyOf(bytes, bytes.length);
			for(IClassTransformer ct : transformers) {
				bytes = ct.transform(name, name, bytes);
				if(!Arrays.equals(bytes, old)) {
					debugDumpClass(count, name, bytes, ct);
					old = Arrays.copyOf(bytes, bytes.length);
				}
				count++;
			}
			
		} else {
			for(IClassTransformer ct : transformers)
				bytes = ct.transform(name, name, bytes);
		}
		
		// return bytes;
		
		// XXX BACKCOMPAT: e.g. CoFH transforms net.minecraft.enchantment.Enchantment.func_92089_a
		// and exceeds the max stack size but doesn't update it.
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		new ClassReader(bytes).accept(cw, 0);
		return cw.toByteArray();
	}
	
	// XXX BACKCOMPAT with codechicken.multipart.asm.ASMMixinCompiler$
	@SuppressWarnings("unused")
	private byte[] runTransformers(String a, String b, byte[] bytes) {
		for(IClassTransformer ct : transformers)
			bytes = ct.transform(a, b, bytes);
		return bytes;
	}

	// XXX BACKCOMPAT with codechicken.multipart.asm.ASMMixinCompiler$
	@SuppressWarnings("unused")
	private Set<String> transformerExceptions = new AbstractSet<String>() {
		@Override
		public boolean add(String e) {
			return !contains(e) && transformerExclusions.add(e);
		}
		@Override
		public boolean remove(Object o) {
			if(!contains(o))
				return false;
			while(transformerExclusions.remove(o));
			return true;
		}
		@Override
		public Iterator<String> iterator() {
			return transformerExclusions.iterator();
		}
		@Override
		public int size() {
			return transformerExclusions.size();
		}
	};
	
	private void debugDumpClass(int stageIndex, String className, byte[] bytes, IClassTransformer lastTransformer) {
		File dumpDir = new File(debugDir, className);
		if(!dumpDir.exists())
			if(!dumpDir.mkdirs()) {
				new Exception("Failed to create "+dumpDir).printStackTrace();
				return;
			}
		
		File dumpFile = new File(dumpDir, stageIndex+"-"+(lastTransformer == null ? "null" : lastTransformer.getClass().getName()));
		try {
			FileOutputStream out = new FileOutputStream(dumpFile);
			try {
				out.write(bytes);
			} finally {
				out.close();
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private boolean isClassLoaderExcluded(String name) {
		for(String s : classloaderExclusions)
			if(name.startsWith(s))
				return true;
		return false;
	}
	
	private boolean isTransformerExcluded(String name) {
		for(String s : transformerExclusions)
			if(name.startsWith(s))
				return true;
		return false;
	}
}
