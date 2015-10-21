package net.mcforkage.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import decompsource.RemapSources;

public class RemapSourcesTask extends Task {
	private File methods, fields, params, input, output;
	private boolean noJavadocs;
	
	
	public void setMethods(File f) {methods = f;}
	public void setFields(File f) {fields = f;}
	public void setParams(File f) {params = f;}
	public void setInput(File f) {input = f;}
	public void setOutput(File f) {output = f;}
	public void setNojavadocs(boolean b) {noJavadocs = b;}
	
	@Override
	public void execute() throws BuildException {
		if(methods == null || fields == null || params == null || input == null || output == null)
			throw new BuildException("A required parameter is missing");
		
		try {
			RemapSources r = new RemapSources();
			r.noJavadocs = noJavadocs;
			r.readConfigs(methods.getAbsolutePath(), fields.getAbsolutePath(), params.getAbsolutePath());
		
			try (InputStream in = new FileInputStream(input)) {
				try (OutputStream out = new FileOutputStream(output)) {
					r.go(in, out);
				}
			}
			
		} catch(Exception e) {
			throw new BuildException(e);
		}
	}
	
}
