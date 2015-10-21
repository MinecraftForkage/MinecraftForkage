package installer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;

import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;

/**
 * This gets packaged together with install-data.zip.lzma,
 * and run as the main class for the installer distributed to users.
 */
public class InstallerMain {
	static class AlreadyHandledException extends Exception {
		/* not serializable */
		private static final long serialVersionUID = new Random().nextLong();
	}
	
	public static void main(String[] args) {
		
		File tempDir = null;
		
		ProgressDialog dlg = ProgressDialog.openModal(null, "MCF Installer");
		
		File globalMCFCacheDir = new File(Utils.getMinecraftDirectory(), "minecraft-forkage-install-cache");
		if(!globalMCFCacheDir.exists()) {
			if(!globalMCFCacheDir.mkdirs())
				globalMCFCacheDir = null;
		}
		
		try {
			Map<String, byte[]> installData;
			
			dlg.startIndeterminate("Unpacking installer");
			
			{
				InputStream embeddedInstallDataStream = InstallerMain.class.getResourceAsStream("/install-data.zip.lzma");
				if(embeddedInstallDataStream != null)
					embeddedInstallDataStream = new LzmaInputStream(embeddedInstallDataStream, new Decoder());
				
				else {
					embeddedInstallDataStream = InstallerMain.class.getResourceAsStream("/install-data.zip.gz");
					if(embeddedInstallDataStream != null)
						embeddedInstallDataStream = new GZIPInputStream(embeddedInstallDataStream);
					else {
						File devInstallDataFile = new File("../../build/install-data.zip.lzma");
						if(devInstallDataFile.exists())
							embeddedInstallDataStream = new LzmaInputStream(new FileInputStream(devInstallDataFile), new Decoder());
						else {
							devInstallDataFile = new File("../../build/install-data.zip.gz");
							
							if(devInstallDataFile.exists()) {
								embeddedInstallDataStream = new GZIPInputStream(new FileInputStream(devInstallDataFile));
							} else {
								JOptionPane.showMessageDialog(null, "The install data file is missing. Whoever created this installer screwed something up.", "MCF Installer Failure", JOptionPane.ERROR_MESSAGE);
								throw new AlreadyHandledException();
							}
						}
					}
				}
				
				try (InputStream in = embeddedInstallDataStream) {
					installData = Utils.readZip(in);
				}
			}

			Properties installProperties = new Properties();
			if(!installData.containsKey("install.properties")) {
				JOptionPane.showMessageDialog(null, "The install properties file is missing. Whoever created this installer screwed something up.", "MCF Installer Failure", JOptionPane.ERROR_MESSAGE);
				throw new AlreadyHandledException();
			}
			
			try (Reader reader = new InputStreamReader(new ByteArrayInputStream(installData.get("install.properties")))) {
				installProperties.load(reader);
			}
			
			String mcver = installProperties.getProperty("mcver");
			String launcherVersionName = installProperties.getProperty("launcherVersionName");
			
			if(mcver == null || launcherVersionName == null) {
				JOptionPane.showMessageDialog(null, "The install properties file is corrupted. Whoever created this installer screwed something up.", "MCF Installer Failure", JOptionPane.ERROR_MESSAGE);
				throw new AlreadyHandledException();
			}
			
			tempDir = File.createTempFile("MCF-INSTALLER-", ".tmp");
			if(!tempDir.delete() || !tempDir.mkdirs())
				throw new Exception("Failed to create directory "+tempDir.getAbsolutePath());
			
			// create a README file in our temporary directory in case
			// something stops us from deleting it when done
			try (FileOutputStream out = new FileOutputStream(new File(tempDir, "AAA TEMP FOLDER README.TXT"))) {
				out.write("This directory was created by the Minecraft Forkage installer. Unless the installer is still running, you can safely delete it at any time.".getBytes(StandardCharsets.UTF_8));
			}
			
			System.out.println("Temp dir: "+tempDir);
			
			
			dlg.startIndeterminate("Downloading client");
			byte[] mcClient = Utils.download(dlg, "http://s3.amazonaws.com/Minecraft.Download/versions/"+mcver+"/"+mcver+".jar", globalMCFCacheDir, "minecraft."+mcver+".jar", "minecraft.jar");
			if(mcClient == null)
				throw new AlreadyHandledException(); // Utils.download already displayed an error message
			
			File clientFile = new File(tempDir, "client.jar");
			try (FileOutputStream out = new FileOutputStream(clientFile)) {
				out.write(mcClient);
			}
			
			dlg.startIndeterminate("Downloading server");
			byte[] mcServer = Utils.download(dlg, "http://s3.amazonaws.com/Minecraft.Download/versions/"+mcver+"/minecraft_server."+mcver+".jar", globalMCFCacheDir, "minecraft_server."+mcver+".jar", "minecraft_server.jar");
			if(mcServer == null)
				throw new AlreadyHandledException();
			
			File serverFile = new File(tempDir, "server.jar");
			try (FileOutputStream out = new FileOutputStream(serverFile)) {
				out.write(mcServer);
			}
			
			mcClient = null;
			mcServer = null;
			
			dlg.startIndeterminate("Installing");
			
			File mainJarFile = Installer.install(clientFile, serverFile, tempDir, installData, dlg);
			
			
			
			///////// INSTALL VERSION IN MINECRAFT LAUNCHER /////////
			
			dlg.startIndeterminate("Installing in launcher");
			
			File versionDir = new File(new File(Utils.getMinecraftDirectory(), "versions"), launcherVersionName);
			if(versionDir.exists()) {
				System.out.println(versionDir+" already exists, deleting...");
				deleteRecursive(versionDir);
			}
			
			if(!versionDir.isDirectory() && !versionDir.mkdirs()) {
				try {Thread.sleep(1000);} catch(InterruptedException e) {}
				if(!versionDir.isDirectory() && !versionDir.mkdirs())
					throw new Exception("Failed to create directory: "+versionDir);
			}
			
			// Install JSON file (by copying from install data)
			try (FileOutputStream versionJsonOut = new FileOutputStream(new File(versionDir, launcherVersionName+".json"))) {
				versionJsonOut.write(installData.get("install.json"));
			}
			
			// Install JAR file (by copying from temp dir)
			try (FileOutputStream jarOut = new FileOutputStream(new File(versionDir, launcherVersionName+".jar"))) {
				try (FileInputStream jarIn = new FileInputStream(mainJarFile)) {
					Utils.copyStream(jarIn, jarOut);
				}
			}
			
			
			
			
			// Done!
			if(!Boolean.getBoolean("minecraftforkage.installer.skipDoneMessage"))
				JOptionPane.showMessageDialog(dlg, "Minecraft Forkage was successfully installed.\nVersion name in launcher: "+launcherVersionName, "MCF Installer - Done!", JOptionPane.INFORMATION_MESSAGE);
			
			
		} catch(AlreadyHandledException e) {
			// do nothing
		
		} catch(Throwable e) {
			e.printStackTrace(); // in case someone is running this in a console
			
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				e.printStackTrace(pw);
			}
			JOptionPane.showMessageDialog(null, sw.toString(), "MCF Installer - unexpected error", JOptionPane.ERROR_MESSAGE);
			
		} finally {
			dlg.setVisible(false);
		}
		
		if(tempDir != null) {
			// Some antivirus programs (MSSE) seem to stop us deleting our temporary files?
			// Therefore causing this process to stay in the background indefinitely.
			// Avoid this by exiting after a fixed amount of time even if we're not done.
			new Thread() {
				{setName("Temporary folder deletion watchdog timer");}
				public void run() {
					try {Thread.sleep(10000);} catch(Exception e) {}
					Runtime.getRuntime().halt(0);
				};
			}.start();
			
			deleteRecursive(tempDir);
		}
		
		System.exit(0);
	}

	private static void deleteRecursive(File f) {
		if(f.isDirectory())
			for(File child : f.listFiles())
				deleteRecursive(child);
		f.delete();
	}
}
