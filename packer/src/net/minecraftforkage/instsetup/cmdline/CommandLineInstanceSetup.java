package net.minecraftforkage.instsetup.cmdline;

import java.io.File;

import net.minecraftforkage.instsetup.InstallationArguments;
import net.minecraftforkage.instsetup.SetupEntryPoint;

public class CommandLineInstanceSetup {
	
	public static void main(String[] args) throws Exception {
		InstallationArguments instArgs = new InstallationArguments();
		for(int k = 0; k < args.length;) {
			switch(args[k++]) {
			
			case "--instanceBaseDir":
				if(k == args.length) throw new Exception("--instanceBaseDir requires argument");
				instArgs.instanceBaseDir = new File(args[k++]);
				break;
			
			case "--bakedJarLocation":
				if(k == args.length) throw new Exception("--bakedJarLocation requires argument");
				instArgs.bakedJarLocation = new File(args[k++]);
				break;
			
			case "--patchedVanillaJarLocation":
				if(k == args.length) throw new Exception("--patchedVanillaJarLocation requires argument");
				instArgs.patchedVanillaJarLocation = new File(args[k++]).toURI().toURL();
				break;
				
			default:
				throw new Exception("Unknown argument: " + args[k-1]);
			}
		}
		
		SetupEntryPoint.setupInstance(instArgs);
	}
}
