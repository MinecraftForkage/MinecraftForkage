package net.mcforkage.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import decompsource.CSV2SRG;

public class CSV2SRGTask extends Task {
	private File methods, fields, srg, output;
	
	public void setMethods(File f) {methods = f;}
	public void setFields(File f) {fields = f;}
	public void setSrg(File f) {srg = f;}
	public void setOutput(File f) {output = f;}
	
	@Override
	public void execute() throws BuildException {
		if(methods == null) throw new BuildException("methods.csv not specified");
		if(fields == null) throw new BuildException("fields.csv not specified");
		if(srg == null) throw new BuildException("reference srg not specified");
		if(output == null) throw new BuildException("output srg not specified");
		
		try {
			try (PrintStream outStream = new PrintStream(output)) {
				CSV2SRG.go(methods.getAbsolutePath(), fields.getAbsolutePath(), new FileInputStream(srg), outStream);
			}
		} catch(Exception e) {
			throw new BuildException(e);
		}
	}
}
