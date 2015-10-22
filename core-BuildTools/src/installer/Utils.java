package installer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;

public class Utils {
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[32768];
		while(true) {
			int read = in.read(buffer);
			if(read <= 0)
				break;
			out.write(buffer, 0, read);
		}
	}
	
	public static byte[] readStream(InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copyStream(in, baos);
		return baos.toByteArray();
	}
	
	public static Map<String, byte[]> readZip(InputStream in) throws IOException {
		ZipInputStream zin = new ZipInputStream(in);
		Map<String, byte[]> rv = new HashMap<>();
		ZipEntry ze;
		while((ze = zin.getNextEntry()) != null) {
			if(!ze.getName().endsWith("/")) {
				rv.put(ze.getName(), readStream(zin));
			}
			zin.closeEntry();
		}
		return rv;
	}

	public static byte[] download(ProgressDialog dlg, String urlString, File cacheDir, String cacheFileName, String overrideFileName) throws MalformedURLException {
		
		File overrideFile = new File(overrideFileName);
		if(overrideFile.exists()) {
			try (FileInputStream in = new FileInputStream(overrideFile)) {
				return readStream(in);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		File cacheFile = new File(cacheDir, cacheFileName);
		if(cacheFile.exists()) {
			try (FileInputStream in = new FileInputStream(cacheFile)) {
				return readStream(in);
			} catch(IOException e) {
				e.printStackTrace();
				cacheFile.delete();
			}
		}
		
		URL url = new URL(urlString);
		try {
			URLConnection conn = url.openConnection();
			try (InputStream stream = conn.getInputStream()) {
				int length = conn.getContentLength();
				if(length != -1) {
					dlg.bar.setValue(0);
					dlg.bar.setMinimum(0);
					dlg.bar.setMaximum(length);
					dlg.bar.setIndeterminate(false);
				}
				
				int nRead = 0;
				ByteArrayOutputStream tempBAOS = new ByteArrayOutputStream();
				byte[] buffer = new byte[32768];
				
				while(true)
				{
					int nReadThis = stream.read(buffer);
					if(nReadThis <= 0)
						break;
					tempBAOS.write(buffer, 0, nReadThis);
					
					nRead += nReadThis;
					if(length != -1)
						dlg.bar.setValue(nRead);
				}
				
				byte[] fileContents = tempBAOS.toByteArray();
				tempBAOS = null;
				
				if(cacheDir != null) {
					// create cache file, by writing to a temp file then atomically moving it
					File cacheTempFile = File.createTempFile("mcf-installer-download-", ".tmp");
					try (FileOutputStream out = new FileOutputStream(cacheTempFile)) {
						out.write(fileContents);
					}
					cacheTempFile.renameTo(cacheFile);
				}
				
				return fileContents;
			}
		} catch(IOException e) {
			JOptionPane.showMessageDialog(dlg, "Download failed: "+e, "MCF Installer Failure", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
	}
	
	public static File getMinecraftDirectory() {
		// same algorithm as Minecraft launcher
		String osname = System.getProperty("os.name").toLowerCase();
		String userHome = System.getProperty("user.home", ".");
		
		if(osname.contains("linux") || osname.contains("unix")) {
			// OS.LINUX
			return new File(userHome, ".minecraft/");
			
		} else if(osname.contains("win")) {
			// OS.WINDOWS
			String appdata = System.getenv("APPDATA");
			return new File(appdata != null ? appdata : userHome, ".minecraft/");
			
		} else if(osname.contains("mac")) {
			// OS.MAC
			return new File(userHome, "Library/Application Support/minecraft");
			
		} else {
			// OS.UNKNOWN
			return new File(userHome, "minecraft/");
		}
	}
}
