package net.minecraftforkage.instsetup;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class InstallationArguments {
	
	/**
	 * Must be set.
	 */
	public File instanceBaseDir;
	
	public boolean isInstallerRunningFromLauncher;
	
	/** 
	 * For custom arguments to setup plugins.
	 * This map is accessible to setup plugins; it is not used directly by Minecraft Forkage.
	 * Keys are recommended (but not required) to be strings starting with your mod ID, to avoid conflicts.
	 */
	public Map<Object, Object> customData = new HashMap<>();
	
	/**
	 * If null, a default location is used.
	 */
	public File outputLocation;
	
	/**
	 * Must be set.
	 */
	public URL coreLocation;

	/**
	 * If true, all required libraries will be packaged into the output JAR.
	 * If true, {@link #libraryDir} and {@link #nativesDir} must be set.
	 */
	public boolean standalone;
	
	/**
	 * Directory containing libraries.
	 * Required if {@link #standalone} is true.
	 */
	public File libraryDir;
	
	/**
	 * Directory containing native libraries.
	 * Required if {@link #standalone} is true.
	 */
	public File nativesDir;
}
