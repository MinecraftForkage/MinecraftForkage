package bytecode;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


public class SortZipEntries {
	public static void main(String[] args) {
		if(args.length != 1 && args.length != 2) {
			System.err.println("Usage: java SortZipEntries infile.zip [[!]filterfile.zip] > outfile.zip");
			System.exit(1);
		}
		
		try {
			
			Set<String> filter = null;
			boolean filterOut = false;
			if(args.length >= 2) {
				if(args[1].startsWith("!")) {
					filterOut = true;
					args[1] = args[1].substring(1);
				}
				filter = new HashSet<>();
				try (ZipFile zf = new ZipFile(new File(args[1]))) {
					for(Enumeration<? extends ZipEntry> entries_enum = zf.entries(); entries_enum.hasMoreElements();)
						filter.add(entries_enum.nextElement().getName());
				}
			}
			
			sort(new File(args[0]), filter, filterOut, System.out);
			
		} catch(Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void sort(File inFile, Set<String> filter, boolean filterOut, OutputStream out) throws Exception {

		try (ZipFile zf = new ZipFile(inFile)) {
			List<ZipEntry> entries = new ArrayList<>();
			for(Enumeration<? extends ZipEntry> entries_enum = zf.entries(); entries_enum.hasMoreElements();)
				entries.add(entries_enum.nextElement());
			
			Collections.sort(entries, new Comparator<ZipEntry>() {
				@Override
				public int compare(ZipEntry o1, ZipEntry o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			
			try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
				for(ZipEntry ze : entries) {
					if(filter != null && filterOut == filter.contains(ze.getName()))
						continue;
					
					try (InputStream in = zf.getInputStream(ze)) {
						zipOut.putNextEntry(new ZipEntry(ze.getName()));
						BaseStreamingZipProcessor.copyResource(in, zipOut);
						zipOut.closeEntry();
					}
				}
			}
		}
	}
}
