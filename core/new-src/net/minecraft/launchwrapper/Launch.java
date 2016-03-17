package net.minecraft.launchwrapper;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.launcher.FMLInjectionAndSortingTweaker;
import cpw.mods.fml.common.launcher.FMLTweaker;

/**
 * This class is specified by Mojang's launchwrapper.
 */
public class Launch {
	/**
	 * The {@link LaunchClassLoader} that Minecraft is loaded with.
	 * This field is specified by Mojang's launchwrapper.
	 */
	public static LaunchClassLoader classLoader;
	
	/**
	 * A globally-accessible Map containing unspecified objects.
	 * This field is specified by Mojang's launchwrapper.
	 */
	public static Map<String, Object> blackboard = new HashMap<String, Object>();
	
	/**
	 * This method is specified by Mojang's launchwrapper.
	 */
	public static void main(String[] args) throws Exception {
		blackboard.put("Tweaks", Collections.emptyList());
		blackboard.put("TweakClasses", Collections.emptyList());
		
		URL[] urls = ((URLClassLoader)Launch.class.getClassLoader()).getURLs();
		classLoader = new LaunchClassLoader(urls);
		injectCascadingTweak(new FMLTweaker());
		classLoader.loadClass("net.minecraft.client.main.Main").getMethod("main", String[].class).invoke(null, (Object)args);
	}

	public static void injectCascadingTweak(ITweaker tweaker) {
		tweaker.acceptOptions(new ArrayList<String>(), new File("."), null, null);
		tweaker.injectIntoClassLoader(classLoader);
	}
}
