package installer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[32768];
		while(true) {
			int read = in.read(buffer);
			if(read <= 0)
				break;
			out.write(buffer, 0, read);
		}
	}
	
	public static byte[] readStream(InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copyStream(in, baos);
		return baos.toByteArray();
	}
	
	public static Map<String, byte[]> readZip(InputStream in) throws IOException {
		ZipInputStream zin = new ZipInputStream(in);
		Map<String, byte[]> rv = new HashMap<>();
		ZipEntry ze;
		while((ze = zin.getNextEntry()) != null) {
			if(!ze.getName().endsWith("/")) {
				rv.put(ze.getName(), readStream(zin));
			}
			zin.closeEntry();
		}
		return rv;
	}
}
