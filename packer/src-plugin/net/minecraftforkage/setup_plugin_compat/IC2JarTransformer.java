package net.minecraftforkage.setup_plugin_compat;

import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.minecraftforkage.instsetup.AbstractZipFile;
import net.minecraftforkage.instsetup.InstanceEnvironmentData;
import net.minecraftforkage.instsetup.JarTransformer;
import net.minecraftforkage.instsetup.PackerContext;

public class IC2JarTransformer extends JarTransformer {

	@Override
	public String getID() {
		return "MinecraftForkage|Compat|IC2";
	}
	
	private void extractFile(AbstractZipFile zipFile, String path) throws Exception {
		if(!zipFile.doesPathExist(path))
			return;
		
		byte[] buffer = new byte[32768];
		System.out.println("[IC2] Extracting "+path);
		try (ZipInputStream ejml_in = new ZipInputStream(zipFile.read(path))) {
			for(ZipEntry ze; (ze = ejml_in.getNextEntry()) != null;) {
				if(ze.getName().startsWith("META-INF/")) {
					ejml_in.closeEntry();
					continue;
				}
				
				if(ze.getName().endsWith("/")) {
					zipFile.createDirectory(ze.getName());
					ejml_in.closeEntry();
					continue;
				}
				
				try (OutputStream out = zipFile.write(ze.getName())) {
					int nRead;
					while((nRead = ejml_in.read(buffer)) >= 0)
						out.write(buffer, 0, nRead);
				}
				
				ejml_in.closeEntry();
			}
		}
		
		zipFile.delete(path);
	}
	
	@Override
	public void transform(AbstractZipFile zipFile, PackerContext context) throws Exception {
		
		extractFile(zipFile, "lib/ejml-0.23.jar");
		extractFile(zipFile, "lib/EJML-core-0.26.jar");
		
		//InstanceEnvironmentData.coremodsToIgnore.add("ic2.core.coremod.IC2core");
	}

}
