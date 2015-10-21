package net.mcforkage.ant;

import java.io.File;
import java.io.FileReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import bytecode.JarMerger;


public class MergeJarsTask extends Task {
	private File client, server, output, config;
	
	public void setClient(File f) {client = f;}
	public void setServer(File f) {server = f;}
	public void setOutput(File f) {output = f;}
	public void setConfig(File f) {config = f;}
	
	@Override
	public void execute() throws BuildException {
		if(client == null) throw new BuildException("Client JAR not specified");
		if(server == null) throw new BuildException("Server JAR not specified");
		if(output == null) throw new BuildException("Output JAR not specified");
		if(config == null) throw new BuildException("Config file not specified");
		
		try {
			JarMerger.merge(client, server, output, new FileReader(config), null);
		} catch(Exception e) {
			throw new BuildException(e);
		}
	}
}
