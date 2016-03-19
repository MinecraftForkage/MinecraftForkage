package net.minecraft.launchwrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Used as the main class for standalone modpack JARs.
 */
public class DirectLaunch {
	public static void main(String[] args) throws Exception {
		File gameDir = new File(".");
		
		File nativesDir = new File(gameDir, "mcforkage-standalone-natives");
		if(!nativesDir.isDirectory()) {
			if(!nativesDir.mkdir())
				throw new RuntimeException("Failed to create directory "+nativesDir.getAbsolutePath());
		}
		
		InputStream nativesZipIn = DirectLaunch.class.getResourceAsStream("/mcforkage-standalone-natives.zip");
		ZipInputStream zin = new ZipInputStream(nativesZipIn);
		byte[] buf = new byte[65536];
		try {
			ZipEntry ze;
			while((ze = zin.getNextEntry()) != null) {
				File outfile = new File(nativesDir, ze.getName());
				FileOutputStream out = new FileOutputStream(outfile);
				try {
					while(true) {
						int read = zin.read(buf);
						if(read <= 0)
							break;
						out.write(buf, 0, read);
					}
				} finally {
					out.close();
				}
				zin.closeEntry();
			}
		} finally {
			zin.close();
		}
		
		System.setProperty("org.lwjgl.librarypath", nativesDir.getAbsolutePath());
		
		Launch.main(new String[] {
			"--login",
			"--gameDir",
			gameDir.toString(),
			"--userProperties",
			"{}",
			"--version",
			"1.7.10"
		});
	}
}
