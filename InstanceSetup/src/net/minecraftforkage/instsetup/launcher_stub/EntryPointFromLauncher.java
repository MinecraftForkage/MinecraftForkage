package net.minecraftforkage.instsetup.launcher_stub;

import java.io.File;
import java.util.Arrays;

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
		
		SetupEntryPoint.setupInstance(gameDir);
		SetupEntryPoint.runInstance(gameDir, args);
	}
}
