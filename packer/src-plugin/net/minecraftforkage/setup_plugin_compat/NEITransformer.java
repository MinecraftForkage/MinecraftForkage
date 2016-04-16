package net.minecraftforkage.setup_plugin_compat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.minecraftforkage.instsetup.AbstractZipFile;
import net.minecraftforkage.instsetup.JarTransformer;
import net.minecraftforkage.instsetup.PackerContext;

public class NEITransformer extends JarTransformer {

	@Override
	public String getID() {
		return "MinecraftForkage|Compat|NotEnoughItems";
	}
	
	@Override
	public Stage getStage() {
		return Stage.MOD_IDENTIFICATION_STAGE;
	}

	@Override
	public void transform(AbstractZipFile zipFile, PackerContext context) throws Exception {
		if(zipFile.doesPathExist("neimod.info")) {
			// BuildcraftCompat has @Optional based on NotEnoughItems
			// NotEnoughItems is a coremod so it needs custom discovery code.
			Map<String, String> modEntry = new HashMap<String, String>();
			modEntry.put("modid", "NotEnoughItems");
			modEntry.put("modtype", "dummy");
			zipFile.appendGSONArray("mcforkage-installed-mods.json", Arrays.asList(modEntry));
		}
	}

}
