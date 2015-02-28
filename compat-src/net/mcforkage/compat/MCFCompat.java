package net.mcforkage.compat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class MCFCompat {

	public static void init(LaunchClassLoader cl) {
		
		
		cl.registerTransformer("net.mcforkage.compat.HardcoreEnderExpansionTransformer");
		
		
		
		
		final LaunchClassLoader old = Launch.classLoader;
		Launch.classLoader = new LaunchClassLoader(new URL[0]) {
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				return old.loadClass(name);
			}
			@Override
			public void addClassLoaderExclusion(String toExclude) {
				old.addClassLoaderExclusion(toExclude);
			}
			@Override
			public void addTransformerExclusion(String toExclude) {
				old.addTransformerExclusion(toExclude);
			}
			@Override
			public void clearNegativeEntries(Set<String> entriesToClear) {
				old.clearNegativeEntries(entriesToClear);
			}
			@Override
			public void clearAssertionStatus() {
				old.clearAssertionStatus();
			}
			@Override
			public void close() throws IOException {
				old.close();
			}
			@Override
			public Class<?> findClass(String arg0) throws ClassNotFoundException {
				return old.findClass(arg0);
			}
			@Override
			public URL findResource(String name) {
				return old.findResource(name);
			}
			@Override
			public Enumeration<URL> findResources(String name) throws IOException {
				return old.findResources(name);
			}
			@Override
			public byte[] getClassBytes(String arg0) throws IOException {
				if(arg0.equals("net.minecraft.world.World")) {
					// prevent CodeChickenLib from deciding it's running in MCP
					StackTraceElement[] stackTrace = new Exception().getStackTrace();
					
					if(stackTrace[1].getClassName().equals("codechicken.lib.asm.ObfMapping") && stackTrace[1].getMethodName().equals("<clinit>"))
						return null;
				}
					
				return old.getClassBytes(arg0);
			}
			@Override
			public URL getResource(String name) {
				return old.getResource(name);
			}
			@Override
			public InputStream getResourceAsStream(String name) {
				return old.getResourceAsStream(name);
			}
			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				return old.getResources(name);
			}
			@Override
			public List<URL> getSources() {
				return old.getSources();
			}
			@Override
			public List<IClassTransformer> getTransformers() {
				return old.getTransformers();
			}
			@Override
			public URL[] getURLs() {
				return old.getURLs();
			}
			@Override
			public void registerTransformer(String arg0) {
				old.registerTransformer(arg0);
			}
			@Override
			public void setClassAssertionStatus(String className,
					boolean enabled) {
				old.setClassAssertionStatus(className, enabled);
			}
			@Override
			public void setDefaultAssertionStatus(boolean enabled) {
				old.setDefaultAssertionStatus(enabled);
			}
			@Override
			public void setPackageAssertionStatus(String packageName, boolean enabled) {
				old.setPackageAssertionStatus(packageName, enabled);
			}
		};
	}
	
	public static String replaceTransformer(String transformerClassName) {
		return transformerClassName;
	}
}





