package bytecode;
import immibis.bon.ClassCollection;
import immibis.bon.Mapping;
import immibis.bon.NameSet;
import immibis.bon.ReferenceDataCollection;
import immibis.bon.Remapper;
import immibis.bon.SimpleNameSet;
import immibis.bon.io.JarLoader;
import immibis.bon.io.JarWriter;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collections;
import java.util.Scanner;


public class ApplySRG {
	public static void main(String[] args) {
		if(args.length != 3) {
			System.err.println("Usage: java ApplySRG srgfile infile outfile");
			System.exit(1);
		}
		
		try {
			
			apply(new FileReader(args[0]), new File(args[1]), new File(args[2]));
			
		} catch(Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void apply(Reader srg, File infile, File outfile) throws Exception {
		NameSet inputNS = new SimpleNameSet("IN");
		NameSet outputNS = new SimpleNameSet("OUT");
		
		Mapping m = new Mapping(inputNS, outputNS);
		
		try (Scanner s = new Scanner(srg)) {
			while(s.hasNextLine()) {
				String line = s.nextLine();
				String[] parts = line.split(" ");
				switch(parts[0]) {
				case "PK:": break;
				case "CL:":
					m.setClass(parts[1], parts[2]);
					break;
				case "FD:":
					m.setField(parts[1].substring(0, parts[1].lastIndexOf('/')), getLastPart(parts[1]), getLastPart(parts[2]));
					break;
				case "MD:":
					m.setMethod(parts[1].substring(0, parts[1].lastIndexOf('/')), getLastPart(parts[1]), parts[2], getLastPart(parts[3]));
					break;
				default: throw new Exception("Unknown SRG line start: "+line);
				}
			}
		}
		
		ClassCollection cc = JarLoader.loadClassesFromJar(inputNS, infile, null);
		cc = Remapper.remap(cc, m, Collections.<ReferenceDataCollection>emptyList(), null);
		JarWriter.write(outfile, cc, null);
	}
	
	private static String getLastPart(String string) {
		if(string.contains("/"))
			return string.substring(string.lastIndexOf('/')+1);
		return string;
	}
}
