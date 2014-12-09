import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import immibis.bon.com.immibis.json.JsonReader;
import bytecode.BaseStreamingZipProcessor;


public class GetLibsFromJson {
	public static void main(String[] args) throws Exception {
		
		if(args.length != 3) {
			System.err.println("Usage: java GetLibsFromJson in.json libsdir/ nativesdir/");
			System.exit(1);
		}
		
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
		try (FileReader fr = new FileReader(new File(args[0]))) {
			json = (Map)JsonReader.readJSON(fr);
		}
		List<Map> libraries = (List<Map>)json.get("libraries");
		
		String libsDir = args[1];
		File nativesDir = new File(args[2]);
		
		for(Map libraryObject : (List<Map>)libraries) {
			Map<String, Object> library = (Map<String, Object>)libraryObject;
			
			Object nameObject = library.get("name");
			Object urlObject = library.get("url");
			Object childrenObject = library.get("children");
			Object rulesObject = library.get("rules");
			Object nativesObject = library.get("natives");
			
			String name = (String)nameObject;
			
			if(rulesObject != null) {
				if(!checkRules((List<?>)rulesObject, osType)) {
					System.out.println("not applicable: "+name);
					continue;
				}
			}
			
			List<String> suffixes = new ArrayList<>();
			
			if(nativesObject == null)
				suffixes.add("");
			else {
				String nativeSuffixObject = (String)((Map<String, ?>)nativesObject).get(osType);
				if(nativeSuffixObject == null) throw new Exception("natives library "+name+" is allowed on platform "+osType+" but has no native suffix specified");
				
				suffixes.add("-" + (String)nativeSuffixObject);
			}
			
			if(childrenObject != null)
				for(String o : (List<String>)childrenObject)
					suffixes.add("-" + o);
			
			String baseURL = (urlObject != null ? (String)urlObject + "/" : "https://libraries.minecraft.net/");
			
			List<File> files = download(libsDir, name, baseURL, suffixes, arch);
			
			if(nativesObject != null) {
				extractNatives(files.get(0), nativesDir);
			}
		}
	}
	

	private static void extractNatives(File zipFile, File nativesDir) throws IOException {
		//System.out.println("extracting natives: "+zipFile.getName());
		try (ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry ze;
			while((ze = zin.getNextEntry()) != null) {
				if(ze.getName().endsWith("/") || ze.getName().startsWith("META-INF/")) {
					zin.closeEntry();
					continue;
				}
				
				if(ze.getName().contains("/") || ze.getName().contains(File.separator))
					throw new SecurityException("filename contains separator: "+ze.getName());
				
				File outFile = new File(nativesDir, ze.getName());
				if(!outFile.exists()) {
					try (FileOutputStream fout = new FileOutputStream(outFile)) {
						BaseStreamingZipProcessor.copyResource(zin, fout);
					}
				}
				zin.closeEntry();
			}
		}
	}

	private static List<File> download(String libsDir, String name, String baseURL, List<String> suffixes, String arch) throws IOException {
		String[] nameParts = name.split(":");
		if(nameParts.length != 3)
			throw new IOException("malformed library name: "+name);
		
		List<File> files = new ArrayList<>();
		
		for(String suffix : suffixes) {
			String fileName = nameParts[1] + "-" + nameParts[2] + suffix + ".jar";
			fileName = fileName.replace("${arch}", arch);
			if(fileName.contains("/") || fileName.contains(File.separator))
				throw new SecurityException("Filename contains separator. Filename is: "+fileName);
			String url = baseURL + nameParts[0].replace(".", "/") + "/" + nameParts[1] + "/" + nameParts[2] + "/" + fileName;
			
			files.add(new File(libsDir, fileName));
			
			
			System.out.println(libsDir+"/"+fileName);
			
			if(new File(libsDir, fileName).exists()) {
				continue;
			}
			
			System.err.println(url);
			
			HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
			conn.setRequestProperty("User-Agent", "TotallyNotJavaMasqueradingAsRandomStuffBecauseForSomeReasonJavaUserAgentsAreBlacklistedButOnlyFromSomeRepositories/1.0");
			try (InputStream downloadStream = conn.getInputStream()) {
				try (OutputStream fileStream = new FileOutputStream(new File(libsDir, fileName))) {
					BaseStreamingZipProcessor.copyResource(downloadStream, fileStream);
				}
			}
		}
		
		return files;
	}

	/** Returns true if this library is allowed. */
	@SuppressWarnings("unchecked")
	private static boolean checkRules(List<?> rules, String osType) throws IOException {
		boolean allowed = false;
		for(Object ruleObject : rules) {
			if(!(ruleObject instanceof Map)) throw new IOException("malformed dev.json");
			
			Map<String, ?> rule = (Map<String, ?>)ruleObject;
			boolean ruleAction;
			if("allow".equals(rule.get("action")))
				ruleAction = true;
			else if("disallow".equals(rule.get("action")))
				ruleAction = false;
			else
				throw new IOException("malformed dev.json");
			
			Map.Entry<String, ?> condition = null;
			for(Map.Entry<String, ?> entry : rule.entrySet()) {
				if(entry.getKey().equals("action"))
					continue;
				else if(condition == null)
					condition = entry;
				else
					throw new IOException("can't handle rule with more than one condition in dev.json: conditions are "+entry.getKey()+" and "+condition.getKey());
			}
			
			if(condition == null)
				allowed = ruleAction;
			
			else if(condition.getKey().equals("os")) {
				if(!(condition.getValue() instanceof Map)) throw new IOException("malformed dev.json");
				Map<String, ?> attrs = (Map<String, ?>)condition.getValue();
				
				if(attrs.size() != 1 || !attrs.containsKey("name")) throw new IOException("can't handle os condition: "+attrs);
				
				if(osType.equals(attrs.get("name")))
					allowed = ruleAction;
			
			} else
				throw new IOException("can't handle unknown rule condition "+condition.getKey());
			
		}
		return allowed;
	}
}
