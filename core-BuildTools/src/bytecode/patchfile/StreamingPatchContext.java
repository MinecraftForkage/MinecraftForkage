package bytecode.patchfile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class StreamingPatchContext {
	private BufferedReader in;
	private PrintWriter out;
	
	public StreamingPatchContext(BufferedReader in, PrintWriter out) {
		this.in = in;
		this.out = out;
	}
	
	private int outputLineCount = 0; // number of lines written so far
	private int inputLineCount = 0; // number of lines read so far
	
	public int getNextInputLine() {
		return inputLineCount + 1;
	}
	
	public int getNextOutputLine() {
		return outputLineCount + 1;
	}
	
	public String readLine() throws IOException {
		String line = in.readLine();
		if(line != null)
			inputLineCount++;
		return line;
	}
	
	public String readLineAlways() throws IOException {
		String line = readLine();
		if(line == null)
			throw new IOException("Unexpected end of input stream");
		return line;
	}
	
	public void writeLine(String line) throws IOException {
		if(line.contains("\n"))
			throw new IOException("Line contains newline: "+line);
		out.println(line);
		outputLineCount++;
	}

	// Copies lines from input to output until just before the specified line numbers
	public void passThroughUntil(int inLine, int outLine) throws IOException {
		int inputLinesToSkip = inLine - getNextInputLine();
		int outputLinesToSkip = outLine - getNextOutputLine();
				
		if(inputLinesToSkip < 0)
			throw new IOException("Cannot seek backwards! (Current input line "+getNextInputLine()+", need to skip to "+inLine+")");
		if(outputLinesToSkip < 0)
			throw new IOException("Cannot seek backwards! (Current output line "+getNextOutputLine()+", need to skip to "+outLine+")");
		if(inputLinesToSkip != outputLinesToSkip)
			throw new IOException("Need to skip different number of input and output lines! (Current position "+getNextInputLine()+"/"+getNextOutputLine()+", target position "+inLine+"/"+outLine+", would skip "+inputLinesToSkip+"/"+outputLinesToSkip+")");
		
		for(int k = 0; k < inputLinesToSkip; k++)
			writeLine(readLine());
	}

	public void skipRestOfFile() throws IOException {
		String line;
		while((line = readLine()) != null)
			writeLine(line);
	}
	
	
}
