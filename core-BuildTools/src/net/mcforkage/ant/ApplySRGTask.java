package net.mcforkage.ant;

import java.io.File;
import java.io.FileReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import bytecode.ApplySRG;

public class ApplySRGTask extends Task {
	private File input, output, srg;
	
	public void setInput(File f) {input = f;}
	public void setOutput(File f) {output = f;}
	public void setSrg(File f) {srg = f;}
	
	@Override
	public void execute() throws BuildException {
		if(input == null) throw new BuildException("Input not specified");
		if(output == null) throw new BuildException("Output not specified");
		if(srg == null) throw new BuildException("SRG not specified");
		
		try {
			ApplySRG.apply(new FileReader(srg), input, output, null);
		} catch(Exception e) {
			throw new BuildException(e);
		}
	}
}
