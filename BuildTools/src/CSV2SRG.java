import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class CSV2SRG {
	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			System.err.println("Usage: CSV2SRG methods.csv fields.csv < notch2srg.srg > srg2mcp.srg");
			System.exit(1);
		}
		
		Map<String, String> methodsCsv = loadCsv(args[0]);
		Map<String, String> fieldsCsv = loadCsv(args[1]);
		
		try (Scanner s = new Scanner(System.in)) {
			while(s.hasNextLine()) {
				String[] parts = s.nextLine().split(" ");
				switch(parts[0]) {
				case "CL:":
					System.out.println("CL: "+parts[2]+" "+parts[2]);
					break;
				case "FD:":
					String name = parts[2].substring(parts[2].lastIndexOf('/')+1);
					String owner = parts[2].substring(0, parts[2].lastIndexOf('/'));
					System.out.println("FD: "+owner+"/"+name+" "+owner+"/"+get(fieldsCsv, name));
					break;
				case "MD:":
					name = parts[3].substring(parts[3].lastIndexOf('/')+1);
					owner = parts[3].substring(0, parts[3].lastIndexOf('/'));
					String desc = parts[4];
					System.out.println("MD: "+owner+"/"+name+" "+desc+" "+owner+"/"+get(methodsCsv, name)+" "+desc);
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
