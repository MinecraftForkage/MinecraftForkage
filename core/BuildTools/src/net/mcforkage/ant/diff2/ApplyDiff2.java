package net.mcforkage.ant.diff2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ApplyDiff2 {
	public static void apply(List<String> inputLines, BufferedReader patch_in, PrintWriter out) throws IOException {
		String line;
		while((line = patch_in.readLine()) != null) {
			//System.out.println(line);
			
			if(line.startsWith("write "))
				out.println(line.substring(6));
			else if(line.startsWith("copy ")) {
				String[] parts = line.split(" ");
				int index = Integer.parseInt(parts[1]);
				int length = Integer.parseInt(parts[2]);
				for(int k = 0; k < length; k++)
					out.println(inputLines.get(index + k));
			}
		}
	}
	public static List<String> readFile(File f) throws IOException {
		ArrayList<String> lines = new ArrayList<>();
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
			
			String line;
			while((line = in.readLine()) != null) {
				if(line.endsWith("\r")) throw new IOException("wrong line endings");
				lines.add(line);
			}
			
		}
		
		lines.trimToSize();
		
		return lines;
	}
}
