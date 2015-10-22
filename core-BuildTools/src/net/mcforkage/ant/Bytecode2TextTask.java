package net.mcforkage.ant;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import bytecode.Bytecode2Text;

public class Bytecode2TextTask extends Task {
	private File input, output;

	public void setInput(File f) {input = f;}
	public void setOutput(File f) {output = f;}
	
	@Override
	public void execute() throws BuildException {
		if(input == null) throw new BuildException("Input file not specified");
		if(output == null) throw new BuildException("Output file not specified");
		
		try (InputStream in = new FileInputStream(input)) {
			try (PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(output), 16384))) {
				Bytecode2Text.go(in, out, null);
			}
		} catch(Exception e) {
			throw new BuildException(e);
		}
	}
}
