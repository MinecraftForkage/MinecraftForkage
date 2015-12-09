package net.minecraftforkage.instsetup;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains random global information that lots of things need during the instance setup process.
 */
public final class InstanceEnvironmentData {
	/**
	 * The main Minecraft directory.
	 * For clients, this is the .minecraft directory.
	 * For servers, this is usually the current directory.
	 */
	public static File getMinecraftDir() {assert minecraftDir != null; return minecraftDir;}
	
	/**
	 * The mods directory.
	 */
	public static File getModsDir() {assert modsDir != null; return modsDir;}
	
	/**
	 * The config directory.
	 */
	public static File getConfigDir() {assert configDir != null; return configDir;}
	
	/**
	 * A temporary directory available for use during instance setup.
	 */
	public static File getSetupTempDir() {assert setupTempDir != null; return setupTempDir;}
	
	/**
	 * An unstructured "blackboard" for any mods that want to add their own data to this, for any reason.
	 * This map is accessible to setup plugins; it is not used directly by Minecraft Forkage.
	 * Keys are recommended (but not required) to be strings starting with your mod ID, to avoid conflicts.
	 */
	public static final Map<Object, Object> customData = new HashMap<Object, Object>();
	
	
	
	
	
	static File configDir;
	static File modsDir;
	static File minecraftDir;
	static File setupTempDir;
	
	
	public static Set<String> extraModContainers = new HashSet<>();
	
	
	private InstanceEnvironmentData() {}
}
