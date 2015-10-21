package decompsource;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class CSV2SRG {
	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			System.err.println("Usage: CSV2SRG methods.csv fields.csv < notch2srg.srg > srg2mcp.srg");
			System.exit(1);
		}
		
		go(args[0], args[1], System.in, System.out);
	}
	
	public static void go(String methodsCsvFile, String fieldsCsvFile, InputStream referenceSrg, PrintStream outputSrg) throws Exception {
		
		Map<String, String> methodsCsv = loadCsv(methodsCsvFile);
		Map<String, String> fieldsCsv = loadCsv(fieldsCsvFile);
		
		try (Scanner s = new Scanner(referenceSrg)) {
			while(s.hasNextLine()) {
				String[] parts = s.nextLine().split(" ");
				switch(parts[0]) {
				case "CL:":
					outputSrg.println("CL: "+parts[2]+" "+parts[2]);
					break;
				case "FD:":
					String name = parts[2].substring(parts[2].lastIndexOf('/')+1);
					String owner = parts[2].substring(0, parts[2].lastIndexOf('/'));
					outputSrg.println("FD: "+owner+"/"+name+" "+owner+"/"+get(fieldsCsv, name));
					break;
				case "MD:":
					name = parts[3].substring(parts[3].lastIndexOf('/')+1);
					owner = parts[3].substring(0, parts[3].lastIndexOf('/'));
					String desc = parts[4];
					outputSrg.println("MD: "+owner+"/"+name+" "+desc+" "+owner+"/"+get(methodsCsv, name)+" "+desc);
					break;
				case "PK:":
					break;
				default:
					throw new RuntimeException("Invalid SRG line type: "+parts[0]);
				}
			}
		}
	}

	private static String get(Map<String, String> csv, String name) {
		return csv.containsKey(name) ? csv.get(name) : name;
	}

	private static Map<String, String> loadCsv(String filename) throws Exception {
		Map<String, String> rv = new HashMap<>();
		try (Scanner s = new Scanner(new File(filename))) {
			while(s.hasNextLine()) {
				String[] parts = s.nextLine().split(",");
				rv.put(parts[0], parts[1]);
			}
		}
		return rv;
	}
}
