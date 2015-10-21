package net.mcforkage.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import bytecode.BaseStreamingZipProcessor;

public class DownloadIfNotExistsTask extends Task {
	
	private File file;
	private String url;
	
	public void setFile(File file) {this.file = file;}
	public void setUrl(String url) {this.url = url;}
	
	@Override
	public void execute() throws BuildException {
		if(file == null) throw new BuildException("File not specified");
		if(url == null) throw new BuildException("URL not specified");
		
		if(file.exists())
			return;
		
		File tempFile = new File(file.getAbsolutePath() + ".temp");
		if(tempFile.exists() && !tempFile.delete())
			throw new BuildException("Failed to delete "+tempFile);
		
		System.out.println("Downloading "+file.getName());
		
		try {
			HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
			conn.setRequestProperty("User-Agent", "TotallyNotJavaMasqueradingAsRandomStuffBecauseForSomeReasonJavaUserAgentsAreBlacklistedButOnlyFromSomeRepositories/1.0");
			try (InputStream downloadStream = conn.getInputStream()) {
				try (OutputStream fileStream = new FileOutputStream(tempFile)) {
					BaseStreamingZipProcessor.copyResource(downloadStream, fileStream);
				}
			}
		} catch(IOException e) {
			throw new BuildException("Failed to download "+file.getName(), e);
		}
		
		if(!tempFile.renameTo(file))
			throw new BuildException("Failed to rename "+tempFile+" to "+file);
	}
	
}
