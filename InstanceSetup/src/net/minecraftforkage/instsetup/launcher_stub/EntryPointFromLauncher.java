package net.minecraftforkage.instsetup.launcher_stub;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import net.minecraftforkage.instsetup.InstallationArguments;
import net.minecraftforkage.instsetup.SetupEntryPoint;

class EntryPointFromLauncher {
	public static void main(String[] args) throws Exception {
		
		File gameDir = null;
		for(int k = 0; k < args.length - 1; k++)
			if(args[k].equals("--gameDir"))
				gameDir = new File(args[k+1]);
		
		if(gameDir == null)
			throw new RuntimeException("No --gameDir argument found! Arguments from launcher: " + Arrays.toString(args));
		
		System.out.println("Arguments from launcher: " + Arrays.toString(args));
		
		// Minecraft launcher has no way to specify additional program arguments, but we can specify JVM arguments, including -D
		if(!Boolean.getBoolean("minecraftforkage.launcherstub.noInstanceSetup")) {
			InstallationArguments instArgs = new InstallationArguments();
			instArgs.instanceBaseDir = gameDir;
			instArgs.isInstallerRunningFromLauncher = true;
			instArgs.patchedVanillaJarLocation = findPatchedVanillaJarLocation();
			SetupEntryPoint.setupInstance(instArgs);
		}
		SetupEntryPoint.runInstance(gameDir, args, SetupEntryPoint.findLibrariesFromClasspath());
	}

	private static URL findPatchedVanillaJarLocation() {
		URL launcherStubURL = null;
		for(URL url : ((URLClassLoader)EntryPointFromLauncher.class.getClassLoader()).getURLs()) {
			if(!url.getProtocol().equals("file")) {
				continue;
			}
			
			String[] path = url.getPath().split("/");
			if(path.length == 0) {
				continue;
			}
			
			String lastSegment = path[path.length - 1];
			if(lastSegment.startsWith("MCForkage-"))
				launcherStubURL = url;
		}
		System.out.println("Launcher stub was loaded from: " + launcherStubURL);
		
		String s = launcherStubURL.toString();
		s = s.substring(0, s.lastIndexOf('/'));
		s += "/patched-vanilla.jar";
		try {
			return new URL(s);
		} catch(MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
