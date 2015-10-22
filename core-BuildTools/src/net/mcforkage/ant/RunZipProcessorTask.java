package net.mcforkage.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import bytecode.BaseStreamingZipProcessor;

public class RunZipProcessorTask extends Task {
	private File input, output, config;
	private String processorClass;
	
	public void setInput(File f) {input = f;}
	public void setOutput(File f) {output = f;}
	public void setConfig(File f) {config = f;}
	public void setClass(String c) {processorClass = c;}
	
	@Override
	public void execute() throws BuildException {
		if(input == null) throw new BuildException("Input not set");
		if(output == null) throw new BuildException("Output not set");
		
		if(processorClass == null) throw new BuildException("Processor class not set");
		
		Class<? extends BaseStreamingZipProcessor> clazz;
		try {
			clazz = Class.forName(processorClass).asSubclass(BaseStreamingZipProcessor.class);
		} catch(ClassNotFoundException | ClassCastException e) {
			throw new BuildException("Class not found, or not a streaming ZIP processor: " + processorClass);
		}
		
		try {
			BaseStreamingZipProcessor zp = clazz.getConstructor().newInstance();
			
			if(zp.hasConfig()) {
				if(config == null) throw new BuildException("Config not specified for "+zp.getClass().getName());
				zp.loadConfig(config);
			} else {
				if(config != null) throw new BuildException(zp.getClass().getName()+" does not use a config");
			}
			
			try (InputStream in = new FileInputStream(input)) {
				try (OutputStream out = new FileOutputStream(output)) {
					zp.go(in, out);
				}
			}
			
		} catch(Exception e) {
			throw new BuildException(e);
		}
	}
}
