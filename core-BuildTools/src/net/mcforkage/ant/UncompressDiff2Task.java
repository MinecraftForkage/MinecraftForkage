package net.mcforkage.ant;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import net.mcforkage.ant.compression.BitInputStream;
import net.mcforkage.ant.diff2.UncompressDiff2;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class UncompressDiff2Task extends Task {
	private File infile, outfile;
	
	public void setInput(File f) {infile = f;}
	public void setOutput(File f) {outfile = f;}
	
	public static void main(String[] args) throws Exception {
		UncompressDiff2Task t = new UncompressDiff2Task();
		t.infile = new File("../../build/bytecode.patch2z");
		t.outfile = new File("../../build/bytecode.patch2.decomp");
		t.execute();
	}
	
	@Override
	public void execute() throws BuildException {
		if(infile == null) throw new BuildException("Input file not set");
		if(outfile == null) throw new BuildException("Output file not set");
		
		try (BitInputStream in = new BitInputStream(new BufferedInputStream(new FileInputStream(infile)))) {
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outfile), StandardCharsets.UTF_8))) {
				UncompressDiff2.uncompress(in, out);
			}
		} catch(IOException e) {
			throw new BuildException(e);
		}
	}
}
