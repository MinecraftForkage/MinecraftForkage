package net.minecraftforkage.instsetup;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.lingala.zip4j.core.ZipFile;
import net.minecraftforkage.instsetup.depsort.DependencySortedObject;
import net.minecraftforkage.instsetup.depsort.DependencySorter;
import net.minecraftforkage.instsetup.depsort.DependencySortingException;

public class SetupEntryPoint {
	
	public static void setupInstance(InstallationArguments args) throws Exception {
		
		if(args.instanceBaseDir == null)
			throw new IllegalArgumentException("args.instanceBaseDir must be set");
		
		InstanceEnvironmentData.minecraftDir = args.instanceBaseDir;
		InstanceEnvironmentData.modsDir = new File(args.instanceBaseDir, "mods");
		InstanceEnvironmentData.configDir = new File(args.instanceBaseDir, "config");
		InstanceEnvironmentData.setupTempDir = new File(args.instanceBaseDir, "setup-temp");
		
		if(args.bakedJarLocation == null)
			args.bakedJarLocation = new File(args.instanceBaseDir, "mcforkage-baked.jar");
		
		deleteRecursive(InstanceEnvironmentData.setupTempDir);
		if(!InstanceEnvironmentData.setupTempDir.mkdir()) {
			try {Thread.sleep(500);} catch(Exception e) {}
			if(!InstanceEnvironmentData.setupTempDir.mkdir()) {
				try {Thread.sleep(500);} catch(Exception e) {}
				if(!InstanceEnvironmentData.setupTempDir.mkdir()) {
					throw new IOException("Failed to create directory: "+InstanceEnvironmentData.setupTempDir.getAbsolutePath());
				}
			}
		}
		
		
		List<File> mods = new ArrayList<>();
		for(File modFile : InstanceEnvironmentData.getModsDir().listFiles())
			if(modFile.getName().endsWith(".zip") || modFile.getName().endsWith(".jar") || modFile.isDirectory())
				mods.add(modFile);
		
		
		System.out.println("Mods:");
		if(mods.size() == 0)
			System.out.println("  <none>");
		else
			for(File f : mods)
				System.out.println("  " + f.getAbsolutePath());
		
		List<String> coremodClassList = new ArrayList<>();
		
		createInitialBakedJar(mods, args.patchedVanillaJarLocation, args.bakedJarLocation, coremodClassList);
		
		System.out.println("Baked JAR: " + args.bakedJarLocation.getAbsolutePath());
		
		
		
		List<URL> setupMods = Installer.findSetupClasspathJars(InstanceEnvironmentData.modsDir);
		System.out.println("Mods involved in instance setup:");
		if(setupMods.size() == 0)
			System.out.println("  <none>");
		else
			for(URL url : setupMods)
				System.out.println("  " + url);
		
		IZipFile bakedJarIZF;
		
		if(false) {
			ZipFile bakedJarZF = new ZipFile(args.bakedJarLocation);
			bakedJarZF.setFileNameCharset("UTF-8");
			bakedJarIZF = new ZipFileAdapter(bakedJarZF);
		} else {
			FileSystem fs = FileSystems.newFileSystem(Paths.get(args.bakedJarLocation.toURI()), null);
			bakedJarIZF = new ZipFileSystemAdapter(fs);
		}
		
		
		
		
		
		URLClassLoader setupModClassLoader = new URLClassLoader(setupMods.toArray(new URL[0]), SetupEntryPoint.class.getClassLoader());
		
		List<JarTransformer> jarTransformers = loadAndDependencySort(JarTransformer.class, setupModClassLoader);
		for(JarTransformer jt : jarTransformers)
			jt.transform(bakedJarIZF);
		
		if(bakedJarIZF instanceof Closeable)
			((Closeable)bakedJarIZF).close();
	}
	
	public static void runInstance(File minecraftDir, String[] args, List<URL> libraryURLs) throws Exception {
		File bakedJar = new File(minecraftDir, "mcforkage-baked.jar");
		
		List<URL> classpath = new ArrayList<>(libraryURLs);
		classpath.add(bakedJar.toURI().toURL());
		URLClassLoader minecraftClassLoader = new URLClassLoader(classpath.toArray(new URL[0]), SetupEntryPoint.class.getClassLoader().getParent());
		
		System.setProperty("minecraftforkage.loadingFromBakedJAR", "true");
		
		List<String> newArgs = new ArrayList<>(Arrays.asList(args));
		newArgs.add("--tweakClass");
		newArgs.add("cpw.mods.fml.common.launcher.FMLTweaker");
		minecraftClassLoader.loadClass("net.minecraft.launchwrapper.Launch").getMethod("main", String[].class).invoke(null, new Object[] {newArgs.toArray(new String[0])});
	}
	
	/** Takes a URL found on the classpath, and checks whether it is a library
	 * (whether it should be used on the Minecraft classpath) */
	private static boolean isClasspathEntryLibrary(URL url) {
		if(!url.getProtocol().equals("file"))
			return true;
		
		String[] path = url.getPath().split("/");
		if(path.length == 0)
			return true;
		
		String lastSegment = path[path.length - 1];
		return !lastSegment.startsWith("MCForkage-") && !lastSegment.contains("zip4j");
	}
	
	public static List<URL> findLibrariesFromClasspath() {
		List<URL> result = new ArrayList<>();
		for(URL url : ((URLClassLoader)SetupEntryPoint.class.getClassLoader()).getURLs()) {
			if(isClasspathEntryLibrary(url)) {
				result.add(url);
				System.out.println("On classpath: " + url + " (is library)");
			} else {
				System.out.println("On classpath: " + url + " (not library)");
			}
		}
		return result;
	}
	
	private static <T extends DependencySortedObject> List<T> loadAndDependencySort(Class<T> what, ClassLoader classLoader) throws DependencySortingException {
		List<T> result = new ArrayList<>();
		for(T t : ServiceLoader.load(what, classLoader))
			result.add(t);
		return DependencySorter.sort(result);
	}


	private static void createInitialBakedJar(List<File> mods, URL patchedVanillaJarURL, File bakedJarFile, List<String> coremodClassList) throws IOException {
		List<URL> inputURLs = new ArrayList<>();
		inputURLs.add(patchedVanillaJarURL);
		for(File modFile : mods)
			inputURLs.add(modFile.toURI().toURL());
		
		List<byte[]> mcmodInfoFiles = new ArrayList<>();
		List<byte[]> versionPropertiesFiles = new ArrayList<>();
		Properties classToSourceMap = new Properties();
		
		try (ZipOutputStream z_out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(bakedJarFile)))) {
			
			Set<String> seenEntries = new HashSet<>();
			
			// Minecraft's LaunchClassLoader *requires* a manifest or it won't call definePackage (WTH?)
			// TODO: Stop using LaunchClassLoader (and ModClassLoader) since we don't need it
			z_out.putNextEntry(new ZipEntry("META-INF/"));
			z_out.closeEntry();
			seenEntries.add("META-INF/");
			z_out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
			z_out.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
			z_out.closeEntry();
			seenEntries.add("META-INF/MANIFEST.MF");
			
			for(URL inputURL : inputURLs) {
				try (ZipInputStream z_in = new ZipInputStream(inputURL.openStream())) {
					ZipEntry ze_in;
					while((ze_in = z_in.getNextEntry()) != null) {
						
						if(ze_in.getName().endsWith("/")) {
							z_in.closeEntry();
							
							// don't warn about duplicate directories; just only add them once
							if(seenEntries.add(ze_in.getName())) {
								z_out.putNextEntry(new ZipEntry(ze_in.getName()));
								z_out.closeEntry();
							}
							
							continue;
						}
						
						if(ze_in.getName().equals("META-INF/MANIFEST.MF")) {
							Manifest mf_in = new Manifest(z_in);
							String coreModClass = mf_in.getMainAttributes().getValue("FMLCorePlugin");
							if(coreModClass != null)
								coremodClassList.add(coreModClass);
							z_in.closeEntry();
							continue;
						}
						
						if(ze_in.getName().startsWith("META-INF/") && (ze_in.getName().endsWith(".SF") || ze_in.getName().endsWith(".RSA") || ze_in.getName().endsWith(".DSA") || ze_in.getName().endsWith(".EC"))) {
							z_in.closeEntry();
							continue;
						}
						
						if(ze_in.getName().equals("mcmod.info")) {
							mcmodInfoFiles.add(Utils.readStream(z_in));
							z_in.closeEntry();
							continue;
						}
						
						if(ze_in.getName().equals("version.properties")) {
							versionPropertiesFiles.add(Utils.readStream(z_in));
							z_in.closeEntry();
							continue;
						}
						
						if(ze_in.getName().endsWith(".class")) {
							String className = ze_in.getName();
							className = className.substring(0, className.length() - 6).replace('/', '.');
							classToSourceMap.put(className, inputURL.toString());
						}
						
						
						if(!seenEntries.add(ze_in.getName()))
							System.err.println("Duplicate entry: "+ze_in.getName());
						else {
							z_out.putNextEntry(new ZipEntry(ze_in.getName()));
							copyStream(z_in, z_out);
							z_in.closeEntry();
							z_out.closeEntry();
						}
					}
				}
			}
			
			
			z_out.putNextEntry(new ZipEntry("mcmod.info"));
			z_out.write(mergeJsonArrays(mcmodInfoFiles));
			z_out.closeEntry();
			
			if(versionPropertiesFiles.size() > 0) {
				z_out.putNextEntry(new ZipEntry("version.properties"));
				z_out.write(mergePropertiesFiles(versionPropertiesFiles));
				z_out.closeEntry();
			}
			
			z_out.putNextEntry(new ZipEntry("mcforkage-class-to-source-map.properties"));
			classToSourceMap.store(z_out, "");
			z_out.closeEntry();
		}
	}


	private static byte[] mergePropertiesFiles(List<byte[]> inputs) {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		for(byte[] input : inputs) {
			result.write(input, 0, input.length);
			result.write('\n');
		}
		return result.toByteArray();
	}

	private static byte[] mergeJsonArrays(List<byte[]> inputs) {
		
		List<Object> mods = new ArrayList<>();
		
		for(byte[] input : inputs) {
			
			Object inputParsed;
			
			try {
				inputParsed = JsonReader.readJSON(new StringReader(new String(input, StandardCharsets.UTF_8)));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			if(inputParsed instanceof List<?>)
				mods.addAll((List<?>)inputParsed);
			else if(inputParsed instanceof Map<?,?> && ((Map<?,?>)inputParsed).containsKey("modList"))
				mods.addAll((List<?>)((Map<?,?>)inputParsed).get("modList"));
			else
				throw new RuntimeException("unrecognized mcmod.info format");
		}
		
		return JsonWriter.toString(mods).getBytes(StandardCharsets.UTF_8);
	}

	private static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[32768];
		while(true) {
			int read = in.read(buffer);
			if(read < 0)
				break;
			out.write(buffer, 0, read);
		}
	}


	private static void deleteRecursive(File dir) throws IOException {
		if(!dir.exists())
			return;
		
		if(dir.isDirectory())
			for(File child : dir.listFiles())
				deleteRecursive(child);
		if(!dir.delete())
			throw new IOException("Failed to delete "+dir.getAbsolutePath());
	}


	private SetupEntryPoint() {}
}
