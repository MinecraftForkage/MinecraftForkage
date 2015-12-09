package net.minecraftforkage;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class PackerDataUtils {
	private PackerDataUtils() {}
	
	public static <T> T read(String path, TypeToken<T> type) {
		try {
			InputStreamReader in = new InputStreamReader(PackerDataUtils.class.getResourceAsStream("/"+path), Charset.forName("UTF-8"));
			try {
				return new Gson().<T>fromJson(in, type.getType());
			} finally {
				in.close();
			}
		} catch(IOException e) {
			throw new RuntimeException("Error reading "+path, e);
		}
	}
}
