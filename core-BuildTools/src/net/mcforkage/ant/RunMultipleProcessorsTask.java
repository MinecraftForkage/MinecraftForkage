package net.mcforkage.ant;

import installer.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import bytecode.BaseStreamingJarProcessor;

public class RunMultipleProcessorsTask extends Task {
	private File input, output;
	private List<Processor> processors = new ArrayList<>();
	
	public void setInput(File f) {input = f;}
	public void setOutput(File f) {output = f;}
	
	public static class Processor {
		String procClass;
		File confFile;
		BaseStreamingJarProcessor instance;
		public void setClass(String s) {procClass = s;}
		public void setConfig(File f) {confFile = f;}
		
		void createInstance() throws BuildException {
			Class<? extends BaseStreamingJarProcessor> clazz;
			try {
				clazz = Class.forName(procClass).asSubclass(BaseStreamingJarProcessor.class);
			} catch(ClassNotFoundException | ClassCastException e) {
				throw new BuildException("Class not found, or not a streaming JAR processor: " + procClass);
			}
			
			try {
				instance = clazz.getConstructor().newInstance();
			} catch(Exception e) {
				throw new BuildException(e);
			}
			
			if(instance.hasConfig()) {
				if(confFile == null) throw new BuildException("Config not specified for "+procClass);
				try (FileReader fr = new FileReader(confFile)) {
					instance.loadConfig(fr);
				} catch(Exception e) {
					throw new BuildException("Failed to load "+confFile+": "+e, e);
				}
			} else {
				if(confFile != null) throw new BuildException(procClass+" does not use a config, but one was specified");
			}
		}
	}
	
	public void addProcessor(Processor p) {
		processors.add(p);
	}
	
	@Override
	public void execute() throws BuildException {
		if(input == null) throw new BuildException("Input not set");
		if(output == null) throw new BuildException("Output not set");
		
		for(Processor p : processors)
			p.createInstance();
		
		try {
			
			try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(input))) {
				try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(output))) {
					ZipEntry ze;
					while((ze = zipIn.getNextEntry()) != null) {
						
						zipOut.putNextEntry(new ZipEntry(ze.getName()));
						
						if(!ze.getName().endsWith(".class")) {
							Utils.copyStream(zipIn, zipOut);
							
						} else {
							byte[] bytes = Utils.readStream(zipIn);
							zipIn.closeEntry();
							
							for(Processor p : processors) {
								ClassWriter cw = new ClassWriter(0);
								new ClassReader(bytes).accept(p.instance.createClassVisitor(cw), 0);
								bytes = cw.toByteArray();
							}
							
							zipOut.write(bytes);
						}
						
						zipIn.closeEntry();
						zipOut.closeEntry();
					}
				}
			}
			
		} catch(Exception e) {
			throw new BuildException(e);
		}
	}
}
