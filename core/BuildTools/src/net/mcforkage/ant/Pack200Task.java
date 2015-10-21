package net.mcforkage.ant;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.jar.JarFile;
import java.util.jar.Pack200;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class Pack200Task extends Task {
private File input, output;
	
	public void setInput(File f) {input = f;}
	public void setOutput(File f) {output = f;}
	
	@Override
	public void execute() throws BuildException {
		if(input == null) throw new BuildException("input file not specified");
		if(output == null) throw new BuildException("output file not specified");
		
		try (JarFile in = new JarFile(input)) {
			try (OutputStream out = new BufferedOutputStream(new FileOutputStream(output))) {
				Pack200.newPacker().pack(in, out);
			}
		} catch(Exception e) {
			throw new BuildException(e);
		}
	}
}
