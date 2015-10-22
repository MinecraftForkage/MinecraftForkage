package net.mcforkage.ant;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lzma.sdk.lzma.Encoder;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class LzmaTask extends Task {
	private File input, output;
	
	public void setInput(File f) {input = f;}
	public void setOutput(File f) {output = f;}
	
	public static void main(String[] args) throws Exception {
		LzmaTask t = new LzmaTask();
		t.setInput(new File("../../build/install-data.zip"));
		t.setOutput(new File("../../build/install-data.zip.lzma"));
		t.execute();
	}
	
	@Override
	public void execute() throws BuildException {
		if(input == null) throw new BuildException("input file not specified");
		if(output == null) throw new BuildException("output file not specified");
		
		Encoder enc = new Encoder();
		
		enc.setDictionarySize(1 << 26);
		
		try (InputStream in = new BufferedInputStream(new FileInputStream(input))) {
			try (OutputStream out = new BufferedOutputStream(new FileOutputStream(output))) {
				enc.writeCoderProperties(out);
				for(int k = 0; k < 8; k++) out.write(255); // file size = -1
				enc.code(in, out, -1, -1, null);
			}
		} catch(IOException e) {
			throw new BuildException(e);
		}
	}
}
