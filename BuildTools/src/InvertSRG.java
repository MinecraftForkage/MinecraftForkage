
import java.util.Scanner;


public class InvertSRG {
	public static void main(String[] args) {
		try (Scanner s = new Scanner(System.in)) {
			while(s.hasNextLine()) {
				String[] parts = s.nextLine().split(" ");
				switch(parts[0]) {
				case "CL:":
				case "FD:":
					System.out.println(parts[0]+" "+parts[2]+" "+parts[1]);
					break;
				case "MD:":
					System.out.println(parts[0]+" "+parts[3]+" "+parts[4]+" "+parts[1]+" "+parts[2]);
					break;
				case "PK:":
					break;
				default:
					throw new RuntimeException("Invalid SRG line type: "+parts[0]);
				}
			}
		}
	}
}
