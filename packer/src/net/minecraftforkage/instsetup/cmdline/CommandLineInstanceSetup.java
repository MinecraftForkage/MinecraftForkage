package net.minecraftforkage.instsetup.cmdline;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
		File runJsonLoc = null;
		File runLibsDir = null;
		
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
				
			case "--run":
				// First argument after --run is the location of the JSON launcher file.
				// Next argument is the library dir.
				// All arguments after --that are passed to Minecraft
				runJsonLoc = new File(args[k++]);
				runLibsDir = new File(args[k++]);
				runArgs = Arrays.copyOfRange(args, k, args.length);
				break argloop;
				
			default:
				throw new Exception("Unknown argument: " + args[k-1]);
			}
		}
		
		SetupEntryPoint.setupInstance(instArgs);
		
		if(runArgs != null) {
			
			JsonObject json;
			try (Reader in = new InputStreamReader(new FileInputStream(runJsonLoc), StandardCharsets.UTF_8)) {
				json = new GsonBuilder().create().fromJson(in, JsonObject.class);
			}
			
			List<URL> urls = new ArrayList<URL>();
			
			for(JsonElement library : json.get("libraries").getAsJsonArray()) {
				String name = library.getAsJsonObject().get("name").getAsString();
				String[] parts = name.split(":");
				
				File libfile = new File(runLibsDir, parts[1]+"-"+parts[2]+".jar");
				if(libfile.exists()) {
					urls.add(libfile.toURI().toURL());
					continue;
				}
				
				libfile = new File(runLibsDir, parts[0].replace(".",File.separator)+File.separator+parts[1]+File.separator+parts[2]+File.separator+parts[1]+"-"+parts[2]+".jar");
				if(libfile.exists()) {
					urls.add(libfile.toURI().toURL());
					continue;
				}
				
				System.err.println("Couldn't find library "+name+" in "+runLibsDir);
			}
			
			urls.add(instArgs.outputLocation.toURI().toURL());
			
			@SuppressWarnings("resource")
			URLClassLoader mcLoader = new URLClassLoader(urls.toArray(new URL[0]));

			// Required for Log4J. Otherwise Log4J uses a SimpleLoggerContextFactory
			// instead of the usual Log4jContextFactory, which leads to creating
			// SimpleLoggers instead of core.Loggers, which leads to INpureCore
			// crashing when it can't cast the loggers to core.Loggers.
			Thread.currentThread().setContextClassLoader(mcLoader);
			
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
}
