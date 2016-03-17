package net.minecraft.launchwrapper;

import java.io.File;
import java.util.List;

/**
 * This class has been copied from Mojang's launchwrapper;
 * it will not function unless copied in its entirety, apart
 * from comments and parameter names.
 * 
 * Comments were not copied.
 */
public interface ITweaker {
	
	/**
	 * Called before Minecraft is loaded.
	 * @param args A modifiable empty list.
	 * @param gameDir The game directory.
	 * @param assetsDir null
	 * @param profile null
	 */
    void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile);
    
    /**
     * Called before Minecraft is loaded.
     * @param classLoader A reference to {@link Launch#classLoader}.
     */
    void injectIntoClassLoader(LaunchClassLoader classLoader);
    
    /**
     * Not used.
     */
    String getLaunchTarget();
    
    /**
     * Not used.
     */
    String[] getLaunchArguments();
}