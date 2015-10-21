package net.mcforkage.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import bytecode.SortZipEntries;

public class SortZipEntriesTask extends Task {
	private File input, output, filter;
	private boolean invertFilter;
	
	public void setInput(File f) {input = f;}
	public void setOutput(File f) {output = f;}
	public void setFilter(File f) {filter = f;}
	public void setInvertfilter(boolean b) {invertFilter = b;}
	
	@Override
	public void execute() throws BuildException {
		if(input == null) throw new BuildException("Input file not specified");
		if(output == null) throw new BuildException("Output file not specified");
		
		try {
			
			Set<String> filterEntries = new HashSet<>();
			
			if(filter != null) {
				try (ZipFile filterZF = new ZipFile(filter)) {
					Enumeration<? extends ZipEntry> entries = filterZF.entries();
					while(entries.hasMoreElements())
						filterEntries.add(entries.nextElement().getName());
				}
			}
			
			try (OutputStream outputStream = new FileOutputStream(output)) {
				SortZipEntries.sort(input, filter == null ? null : filterEntries, invertFilter, outputStream);
			}
		} catch(Exception e) {
			throw new BuildException(e);
		}
	}
}
