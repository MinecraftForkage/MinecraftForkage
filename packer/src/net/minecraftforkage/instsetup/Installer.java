package net.minecraftforkage.instsetup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

class Installer {
	
	private static boolean isSetupClasspathJar(Attributes manifestSection) {
		String value = manifestSection.getValue("MCF-InstanceSetupClasspath");
		if(value == null)
			return false;
		
		return value.equals("true")
			|| value.startsWith("true; "); // future compatibility?
	}
	
	/**
	 * Returns all URLs
	 * 
	 * Note: this may return nested JAR URLs such as <tt>jar:jar:file:somefile.jar!/somepath.jar!/somepath</tt>.
	 * Java is not capable of handling these natively.
	 * 
	 * @param modsDir The main mods folder.
	 */
	static List<URL> findSetupClasspathJars(File modsDir) throws IOException {
		List<URL> result = new ArrayList<>();
		
		for(File modFileOrDir : modsDir.listFiles()) {
			if(modFileOrDir.isFile())
				findSetupClasspathJarsInJar(modFileOrDir, result);
			else if(modFileOrDir.isDirectory())
				findSetupClasspathJarsInDirectory(modFileOrDir, result);
		}
		
		return result;
	}
	
	private static void findSetupClasspathJarsInDirectory(File modDir, List<URL> result) throws IOException {
		File manifestFile = new File(new File(modDir, "META-INF"), "MANIFEST.MF");
		if(!manifestFile.exists())
			return;
		
		// Read manifest data from manifest file
		Manifest manifest = new Manifest();
		try (InputStream in = new FileInputStream(manifestFile)) {
			manifest.read(in);
		}
		
		if(isSetupClasspathJar(manifest.getMainAttributes())) {
			result.add(modDir.toURI().toURL());
		}
		
		for(Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
			if(isSetupClasspathJar(entry.getValue())) {
				findSetupClasspathJarsInJar(new File(modDir, entry.getKey()), result);
			}
		}
	}
	
	private static void findSetupClasspathJarsInJar(File modJar, List<URL> result) throws IOException {
		try (JarInputStream j_in = new JarInputStream(new FileInputStream(modJar))) {
			findSetupClasspathJarsInJar(j_in, result, modJar.toURI().toURL());
		}
	}
	
	private static void findSetupClasspathJarsInJar(JarInputStream jarIn, List<URL> result, URL jarURL) throws IOException {
		Manifest manifest = jarIn.getManifest();
		if(manifest == null)
			return;
		
		if(isSetupClasspathJar(manifest.getMainAttributes())) {
			result.add(jarURL);
		}
		
		Set<String> subJarNames = new HashSet<>();
		
		for(Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
			if(isSetupClasspathJar(entry.getValue())) {
				subJarNames.add(entry.getKey());
			}
		}
		
		if(subJarNames.size() == 0)
			return;
		
		JarEntry je;
		while((je = jarIn.getNextJarEntry()) != null) {
			if(subJarNames.contains(je.getName())) {
				findSetupClasspathJarsInJar(new JarInputStream(jarIn), result, new URL("jar:" + jarURL.toString() + "!/" + je.getName()));
				jarIn.closeEntry();
			}
		}
	}
}
