package net.minecraftforkage.setup_plugin;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.minecraftforkage.instsetup.AbstractZipFile;
import net.minecraftforkage.instsetup.JarTransformer;
import net.minecraftforkage.instsetup.PackerContext;

public class CoremodSearchTransformer extends JarTransformer {
	@Override
	public String getID() {
		return "MinecraftForkage|CoremodSearchTransformer";
	}
	
	class CascadingTweaker {
		String className;
		int sortOrder;
		
		CascadingTweaker(String className, String sortOrder) {
			this.className = className;
			
			this.sortOrder = 0;
			if(sortOrder != null) {
				try {
					this.sortOrder = Integer.parseInt(sortOrder);
				} catch(NumberFormatException ex) {
				}
			}
		}
	}
	
	@Override
	public void transform(AbstractZipFile zipFile, PackerContext context) throws Exception {
		List<String> coremodClasses = new ArrayList<String>();
		List<String> accessTransformers = new ArrayList<String>();
		List<CascadingTweaker> cascadingTweakers = new ArrayList<CascadingTweaker>();
		for(URL modURL : context.getModURLs()) {
			Manifest mf = null;
			try (ZipInputStream zin = new ZipInputStream(modURL.openStream())) {
				ZipEntry ze;
				while((ze = zin.getNextEntry()) != null) {
					if(ze.getName().equals("META-INF/MANIFEST.MF")) {
						mf = new Manifest();
						mf.read(zin);
						zin.closeEntry();
						break;
					}
					zin.closeEntry();
				}
			}
			
			if(mf != null) {
				Attributes attr = mf.getMainAttributes();
				String value;
				
				value = attr.getValue("FMLCorePlugin");
				if(value != null)
					coremodClasses.add(value);
				
				value = attr.getValue("FMLAT");
				if(value != null)
					accessTransformers.add(value);
				
				value = attr.getValue("TweakClass");
				if(value != null)
					cascadingTweakers.add(new CascadingTweaker(value, attr.getValue("TweakOrder")));
				
				value = attr.getValue("ModType");
				if(value != null)
					// TODO: use these somehow
					// FML's behaviour is to ignore the mod file if value.split(",") (no trimming) does not contain "FML"
					System.out.println("Unhandled: Found a ModType annotation in "+modURL+" with value "+value);
				
				value = attr.getValue("ModSide");
				if(value != null)
					// TODO: use these somehow
					// FML's behaviour is to ignore the mod file if !FMLLaunchHandler.side.name().equals(modSide)
					System.out.println("Unhandled: Found a ModSide annotation in "+modURL+" with value "+value);
			}
		}
		
		zipFile.appendGSONArray("mcforkage-coremods.json", coremodClasses);
		zipFile.appendGSONArray("mcforkage-FMLAT.json", accessTransformers);
		zipFile.appendGSONArray("mcforkage-cascading-tweakers.json", cascadingTweakers);
	}
	
	@Override
	public Stage getStage() {
		return Stage.MOD_IDENTIFICATION_STAGE;
	}
}
