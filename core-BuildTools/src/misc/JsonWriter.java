package misc;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class JsonWriter {
	
	public static String toString(Object obj) {
		StringWriter sw = new StringWriter();
		try {
			writeJSON(sw, obj);
			sw.close();
		} catch(IOException e) {
			throw new AssertionError("this shouldn't happen", e);
		}
		return sw.toString();
	}
	public static void writeJSON(Writer w, Object obj) throws IOException {
		if(obj instanceof Boolean) {
			w.write((Boolean)obj ? "true" : "false");
		} else if(obj instanceof Number) {
			double d = ((Number)obj).doubleValue();
			if(d == (double)(int)d)
				w.write(String.valueOf((int)d));
			else
				w.write(String.valueOf(d));
		} else if(obj == null) {
			w.write("null");
		} else if(obj instanceof String) {
			w.write('"');
			String s = (String)obj;
			for(int k = 0; k < s.length(); k++) {
				char c = s.charAt(k);
				if(c == '\\' || c == '"') {
					w.write('\\');
					w.write(c);
				} else if(c < 0x20) {
					w.write("\\u00");
					w.write(HEXDIGITS.charAt(c >> 4));
					w.write(HEXDIGITS.charAt(c & 15));
				} else
					w.write(c);
			}
			w.write('"');
			
		} else if(obj instanceof List<?>) {
			w.write('[');
			boolean first = true;
			for(Object element : (List<?>)obj) {
				if(first) first = false;
				else w.write(',');
				writeJSON(w, element);
			}
			w.write(']');
			
		} else if(obj instanceof Map<?, ?>) {
			w.write('{');
			
			boolean first = true;
			for(Map.Entry<?, ?> entry : ((Map<?,?>)obj).entrySet()) {
				if(!(entry.getKey() instanceof String))
					throw new IllegalArgumentException("Map keys must be strings. Found "+entry.getKey());
				
				if(first) first = false;
				else w.write(',');
				
				writeJSON(w, entry.getKey());
				w.write(':');
				writeJSON(w, entry.getValue());
			}
			
			w.write('}');
			
		} else {
			throw new IllegalArgumentException("Not a valid JSON value: "+obj+" of type "+obj.getClass().getName());
		}
	}
	
	private static final String HEXDIGITS = "0123456789ABCDEF";
}
