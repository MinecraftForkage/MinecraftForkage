package net.minecraftforkage.instsetup;

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
}
