package net.minecraftforkage.instsetup;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
	 */
	public static final Map<Object, Object> customData = new HashMap<Object, Object>();
	
	
	
	
	
	static File configDir;
	static File modsDir;
	static File minecraftDir;
	static File setupTempDir;
	
	
	
	private InstanceEnvironmentData() {}
}
