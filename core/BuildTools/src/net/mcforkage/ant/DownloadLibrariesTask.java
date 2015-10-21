package net.mcforkage.ant;

import immibis.bon.com.immibis.json.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import bytecode.BaseStreamingZipProcessor;

public class DownloadLibrariesTask extends Task {
	
	private File jsonfile, libsdir, nativesdir;
	
	public void setJsonfile(File f) {
		jsonfile = f;
	}
	
	public void setLibsdir(File f) {
		libsdir = f;
	}
	
	public void setNativesdir(File f) {
		nativesdir = f;
	}
	
	private static class PendingDownload {
		URL url;
		File file;
		Object nativesJson;
		public PendingDownload(URL url, File file, Object nativesJson) {
			this.url = url;
			this.file = file;
			this.nativesJson = nativesJson;
		}
	}
	
	@Override
	public void execute() throws BuildException {
		if(jsonfile == null)
			throw new BuildException("jsonfile not specified");
		if(libsdir == null)
			throw new BuildException("libsdir not specified");
		if(nativesdir == null)
			throw new BuildException("nativesdir not specified");
		
		String osType = "unknown";
		{
			String osname = System.getProperty("os.name").toLowerCase();
			if(osname.contains("linux") || osname.contains("unix"))
				osType = "linux";
			if(osname.contains("win"))
				osType = "windows";
			if(osname.contains("mac"))
				osType = "mac";
		}
		
		String arch = System.getProperty("os.arch").contains("64") ? "64" : "32";
		
		Map json;
		try (FileReader fr = new FileReader(jsonfile)) {
			json = (Map)JsonReader.readJSON(fr);
		} catch(IOException e) {
			throw new BuildException("Failed to read or parse "+jsonfile, e);
		}
		
		
		
		List<PendingDownload> allDownloads = new ArrayList<DownloadLibrariesTask.PendingDownload>();
		
		for(Object libraryObject : (List<?>)json.get("libraries"))
			allDownloads.addAll(getDownloadsForLibrary(libraryObject, osType, arch));
		
		
		List<PendingDownload> toDownload = new ArrayList<PendingDownload>(allDownloads);
		
		for(Iterator<PendingDownload> it = toDownload.iterator(); it.hasNext();)
			if(it.next().file.exists())
				it.remove();
		
		
		
		
		for(int k = 0; k < toDownload.size(); k++) {
			PendingDownload download = toDownload.get(k);
			System.out.println("Downloading "+(k+1)+"/"+toDownload.size()+": "+download.file.getName());
			
			download(download);
		}
		
		for(PendingDownload download : allDownloads) {
			if(download.nativesJson != null) {
				extractNatives(download.file, nativesdir);
			}
		}
	}
	
	
	private List<PendingDownload> getDownloadsForLibrary(Object libraryObject, String osType, String arch) {
		
		Map<String, Object> library = (Map<String, Object>)libraryObject;
		
		Object nameObject = library.get("name");
		Object urlObject = library.get("url");
		Object childrenObject = library.get("children");
		Object rulesObject = library.get("rules");
		Object nativesObject = library.get("natives");
		
		String name = (String)nameObject;
		
		if(rulesObject != null) {
			if(!checkRules((List<?>)rulesObject, osType)) {
				return Collections.emptyList();
			}
		}
		
		List<PendingDownload> rv = new ArrayList<>();
		
		List<String> suffixes = new ArrayList<>();
		
		if(nativesObject == null)
			suffixes.add("");
		else {
			String nativeSuffix = (String)((Map<String, ?>)nativesObject).get(osType);
			if(nativeSuffix == null) throw new BuildException("natives library "+name+" has no native suffix specified");
			
			suffixes.add("-" + (String)nativeSuffix);
		}
		
		if(childrenObject != null)
			for(String o : (List<String>)childrenObject)
				suffixes.add("-" + o);
		
		String baseURL = (urlObject != null ? (String)urlObject + "/" : "https://libraries.minecraft.net/");
		
		String[] nameParts = name.split(":");
		if(nameParts.length != 3)
			throw new BuildException("malformed library name: "+name);
		
		for(String suffix : suffixes) {
			String fileName = nameParts[1] + "-" + nameParts[2] + suffix + ".jar";
			fileName = fileName.replace("${arch}", arch);
			File file = new File(libsdir, fileName);
			if(!file.getParentFile().getAbsolutePath().equals(libsdir.getAbsolutePath()))
				throw new SecurityException("Filename contains separator. Filename is: "+fileName);
			String url = baseURL + nameParts[0].replace(".", "/") + "/" + nameParts[1] + "/" + nameParts[2] + "/" + fileName;
			
			try {
				rv.add(new PendingDownload(new URL(url), file, (suffix.equals(suffixes.get(0)) ? nativesObject : null)));
			} catch (MalformedURLException e) {
				throw new BuildException(e);
			}
		}
		
		return rv;
	}
	
	

	private static void extractNatives(File zipFile, File nativesDir) throws BuildException {
		//System.out.println("extracting natives: "+zipFile.getName());
		try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry ze;
			while((ze = zin.getNextEntry()) != null) {
				if(ze.getName().endsWith("/") || ze.getName().startsWith("META-INF/")) {
					zin.closeEntry();
					continue;
				}
				
				File outFile = new File(nativesDir, ze.getName());
				if(!outFile.exists()) {
					if(!outFile.getParentFile().getAbsolutePath().equals(nativesDir.getAbsolutePath()))
						throw new SecurityException("filename contains separator: "+ze.getName());
					try (FileOutputStream fout = new FileOutputStream(outFile)) {
						BaseStreamingZipProcessor.copyResource(zin, fout);
					}
				}
				zin.closeEntry();
			}
		} catch(IOException e) {
			throw new BuildException("Failed to extract natives from "+zipFile+" to "+nativesDir, e);
		}
	}

	private static void download(PendingDownload info) throws BuildException {
		try {
			HttpURLConnection conn = (HttpURLConnection)info.url.openConnection();
			conn.setRequestProperty("User-Agent", "TotallyNotJavaMasqueradingAsRandomStuffBecauseForSomeReasonJavaUserAgentsAreBlacklistedButOnlyFromSomeRepositories/1.0");
			
			File tempfile = new File(info.file.getParentFile(), "temp-downloading");
			
			try (InputStream downloadStream = conn.getInputStream()) {
				try (OutputStream fileStream = new FileOutputStream(tempfile)) {
					BaseStreamingZipProcessor.copyResource(downloadStream, fileStream);
				}
			}
			
			// Thanks Oracle.
			for(int k = 0; k < 5; k++) {
				info.file.delete();
				if(info.file.exists())
					Thread.sleep(200);
				else
					break;
			}
			
			for(int k = 0; k < 5; k++) {
				if(tempfile.renameTo(info.file))
					break;
			}
			
		} catch(IOException | InterruptedException e) {
			throw new BuildException("Failed to download "+info.url, e);
		}
	}

	/** Returns true if this library is allowed. */
	@SuppressWarnings("unchecked")
	private static boolean checkRules(List<?> rules, String osType) throws BuildException {
		boolean allowed = false;
		for(Object ruleObject : rules) {
			if(!(ruleObject instanceof Map)) throw new BuildException("malformed dev.json");
			
			Map<String, ?> rule = (Map<String, ?>)ruleObject;
			boolean ruleAction;
			if("allow".equals(rule.get("action")))
				ruleAction = true;
			else if("disallow".equals(rule.get("action")))
				ruleAction = false;
			else
				throw new BuildException("malformed dev.json");
			
			Map.Entry<String, ?> condition = null;
			for(Map.Entry<String, ?> entry : rule.entrySet()) {
				if(entry.getKey().equals("action"))
					continue;
				else if(condition == null)
					condition = entry;
				else
					throw new BuildException("can't handle rule with more than one condition in dev.json: conditions are "+entry.getKey()+" and "+condition.getKey());
			}
			
			if(condition == null)
				allowed = ruleAction;
			
			else if(condition.getKey().equals("os")) {
				if(!(condition.getValue() instanceof Map)) throw new BuildException("malformed dev.json");
				Map<String, ?> attrs = (Map<String, ?>)condition.getValue();
				
				if(attrs.size() != 1 || !attrs.containsKey("name")) throw new BuildException("can't handle os condition: "+attrs);
				
				if(osType.equals(attrs.get("name")))
					allowed = ruleAction;
			
			} else
				throw new BuildException("can't handle unknown rule condition "+condition.getKey());
			
		}
		return allowed;
	}
}
