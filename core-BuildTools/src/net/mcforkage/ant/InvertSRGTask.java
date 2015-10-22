package net.mcforkage.ant;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class InvertSRGTask extends Task {
	private File input, output;
	
	public void setInput(File f) {input = f;}
	public void setOutput(File f) {output = f;}
	
	@Override
	public void execute() throws BuildException {
		if(input == null) throw new BuildException("input file not specified");
		if(output == null) throw new BuildException("output file not specified");
		
		try (Scanner s = new Scanner(input)) {
			try (PrintStream out = new PrintStream(output)) {
				while(s.hasNextLine()) {
					String[] parts = s.nextLine().split(" ");
					switch(parts[0]) {
					case "CL:":
					case "FD:":
						out.println(parts[0]+" "+parts[2]+" "+parts[1]);
						break;
					case "MD:":
						out.println(parts[0]+" "+parts[3]+" "+parts[4]+" "+parts[1]+" "+parts[2]);
						break;
					case "PK:":
						break;
					default:
						throw new RuntimeException("Invalid SRG line type: "+parts[0]);
					}
				}
			}
		} catch(IOException e) {
			throw new BuildException(e);
		}
	}
}
