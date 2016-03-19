package net.minecraftforkage.instsetup.cmdline;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraftforkage.instsetup.InstallationArguments;
import net.minecraftforkage.instsetup.SetupEntryPoint;

public class CommandLineInstanceSetup {
	
	public static void main(String[] args) throws Exception {
		InstallationArguments instArgs = new InstallationArguments();
		
		String[] runArgs = null;
		
		argloop: for(int k = 0; k < args.length;) {
			switch(args[k++]) {
			
			case "--instanceBaseDir":
				if(k == args.length) throw new Exception("--instanceBaseDir requires argument");
				instArgs.instanceBaseDir = new File(args[k++]);
				break;
			
			case "--outputLocation":
				if(k == args.length) throw new Exception("--outputLocation requires argument");
				instArgs.outputLocation = new File(args[k++]);
				break;
			
			case "--coreLocation":
				if(k == args.length) throw new Exception("--coreLocation requires argument");
				instArgs.coreLocation = new File(args[k++]).toURI().toURL();
				break;
			
			case "--standalone":
				instArgs.standalone = true;
				break;
			
			case "--libraryDir":
				if(k == args.length) throw new Exception("--libraryDir requires argument");
				instArgs.libraryDir = new File(args[k++]);
				break;
				
			case "--nativesDir":
				if(k == args.length) throw new Exception("--nativesDir requires argument");
				instArgs.nativesDir = new File(args[k++]);
				break;
				
			case "--run":
				// All arguments after --run are passed to Minecraft
				runArgs = Arrays.copyOfRange(args, k, args.length);
				break argloop;
				
			default:
				throw new Exception("Unknown argument: " + args[k-1]);
			}
		}
		
		if(runArgs != null && instArgs.libraryDir == null)
			throw new Exception("--run requires --libraryDir");
		
		SetupEntryPoint.setupInstance(instArgs);
		
		if(runArgs != null) {
			
			List<URL> urls = new ArrayList<URL>();
			
			// If the modpack JAR is standalone, then the libraries
			// are included in it and don't need to be added to the classpath.
			if(!instArgs.standalone) {
				JsonObject json;
				try (Reader in = new InputStreamReader(getLauncherJsonURL(instArgs.coreLocation).openStream(), StandardCharsets.UTF_8)) {
					json = new GsonBuilder().create().fromJson(in, JsonObject.class);
				}
				
				for(JsonElement library : json.get("libraries").getAsJsonArray()) {
					String name = library.getAsJsonObject().get("name").getAsString();
					String[] parts = name.split(":");
					
					File libfile = new File(instArgs.libraryDir, parts[1]+"-"+parts[2]+".jar");
					if(libfile.exists()) {
						urls.add(libfile.toURI().toURL());
						continue;
					}
					
					libfile = new File(instArgs.libraryDir, parts[0].replace(".",File.separator)+File.separator+parts[1]+File.separator+parts[2]+File.separator+parts[1]+"-"+parts[2]+".jar");
					if(libfile.exists()) {
						urls.add(libfile.toURI().toURL());
						continue;
					}
					
					System.err.println("Couldn't find library "+name+" in "+instArgs.libraryDir);
				}
			}
			
			urls.add(instArgs.outputLocation.toURI().toURL());
			
			@SuppressWarnings("resource")
			URLClassLoader mcLoader = new URLClassLoader(urls.toArray(new URL[0]));

			List<String> allRunArgs = new ArrayList<>();
			allRunArgs.addAll(Arrays.asList(runArgs));
			allRunArgs.add("--gameDir");
			allRunArgs.add(instArgs.instanceBaseDir.getAbsolutePath());
			
			Class<?> mcMain = mcLoader.loadClass("net.minecraft.launchwrapper.Launch");
			Method mainMethod = mcMain.getMethod("main", String[].class);
			//System.in.read();
			mainMethod.invoke(null, (Object)allRunArgs.toArray(new String[0]));
		}
	}
	
	private static URL getLauncherJsonURL(URL coreLoc) throws MalformedURLException {
		return new URL("jar:" + coreLoc.toString() + "!/mcforkage-launcher-info.json");
	}
}
