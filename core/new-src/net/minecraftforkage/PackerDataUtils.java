package net.minecraftforkage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class PackerDataUtils {
	private PackerDataUtils() {}
	
	public static <T> T read(String path, TypeToken<T> type) {
		try {
			InputStream stream = PackerDataUtils.class.getResourceAsStream("/"+path);
			if(stream != null) {
				InputStreamReader in = new InputStreamReader(stream, Charset.forName("UTF-8"));
				try {
					return new Gson().<T>fromJson(in, type.getType());
				} finally {
					in.close();
				}
			} else {
				throw new IOException("Path not found: "+path);
			}
		} catch(IOException e) {
			throw new RuntimeException("Error reading "+path, e);
		}
	}
}
