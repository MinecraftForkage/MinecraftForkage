package net.minecraftforkage.instsetup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class Utils {
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
}
