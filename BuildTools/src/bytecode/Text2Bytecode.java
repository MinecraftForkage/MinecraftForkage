package bytecode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class Text2Bytecode {
	
	public static void main(String[] args) throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try (ZipOutputStream out = new ZipOutputStream(System.out)) {
			new Text2Bytecode(in, out).run();
		}
	}
	
	private BufferedReader in;
	private ZipOutputStream out;
	private String line;
	public Text2Bytecode(BufferedReader in, ZipOutputStream out) {
		this.in = in;
		this.out = out;
	}
	
	private String getline() throws Exception {
		line = in.readLine();
		if(line != null)
			line = line.replace("\r", "");
		return line;
	}
	
	private ArrayList<Object> objstack = new ArrayList<>();
	private Map<String, Label> labels = new HashMap<>();
	
	public void run() throws Exception {
		boolean firstFile = true;
		while(getline() != null) {
			if(line.startsWith("FILE ")) {
				if(firstFile) firstFile = false;
				else {
					if(objstack.size() != 1)
						throw new AssertionError("leftover objects: "+objstack.toString());
					out.write(((ClassWriter)objstack.remove(0)).toByteArray());
					out.closeEntry();
				}
				out.putNextEntry(new ZipEntry(line.substring(5)));
				
				//System.err.println(line);
				
			} else if(line.startsWith("CALL ")) {
				String[] parts = line.split(" ");
				Object object;
				if(objstack.size() == 0 && parts[1].equals("PUSH")) {
					object = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					objstack.add(object);
				} else if(parts[1].equals("TOP"))
					object = objstack.get(objstack.size()-1);
				else if(parts[1].equals("POP")) {
					object = objstack.remove(objstack.size()-1);
					//System.err.println("pop "+object);
					object = objstack.get(objstack.size()-1);
				} else
					throw new AssertionError("can't get object: "+line);
				
				int nargs = Integer.parseInt(parts[4]);
				Object[] args = new Object[nargs];
				for(int k = 0; k < nargs; k++)
					args[k] = readArg();
				
				String methodName = parts[2];
				Method method = getMethod(object.getClass(), methodName);
				
				Object result = method.invoke(object, args);
				
				if(result instanceof MethodVisitor)
					labels.clear();
				
				//System.err.println("call "+object+" "+methodName);
				
				if(parts[3].equals("PUSH")) {
					objstack.add(result);
					//System.err.println("push "+result);
				} else if(!parts[3].equals("X"))
					throw new AssertionError("invalid return type: "+parts[3]);
				
			} else
				throw new Exception(line);
		}
		if(!firstFile) {
			if(objstack.size() != 1)
				throw new AssertionError("leftover objects: "+objstack.toString());
			out.write(((ClassWriter)objstack.remove(0)).toByteArray());
			out.closeEntry();
		}
	}

	private Method getMethod(Class<?> class1, String methodName) throws Exception {
		for(Method m : class1.getMethods()) {
			if(m.getName().equals(methodName)) {
				m.setAccessible(true);
				return m;
			}
		}
		//if(class1.getSuperclass() != null)
		//	return getMethod(class1.getSuperclass(), methodName);
		throw new AssertionError("no such method: "+class1.getName()+"."+methodName);
	}

	private Object readArg() throws Exception {
		getline();
		if(line.startsWith("I"))
			return Integer.parseInt(line.substring(1));
		if(line.startsWith("S"))
			return URLDecoder.decode(line.substring(line.indexOf(' ')+1), "UTF-8");
		if(line.equals("_null"))
			return null;
		if(line.equals("_true"))
			return Boolean.TRUE;
		if(line.equals("_false"))
			return Boolean.FALSE;
		if(line.startsWith("LNEW")) {
			Label l = new Label();
			labels.put(line.substring(4), l);
			return l;
		}
		if(line.startsWith("L")) {
			Label l = labels.get(line.substring(1));
			if(l == null) throw new AssertionError("label not defined "+line);
			return l;
		}
		if(line.startsWith("F"))
			return Float.intBitsToFloat(Integer.parseInt(line.substring(1)));
		if(line.startsWith("T"))
			return Type.getType(line.substring(1));
		if(line.startsWith("D"))
			return Double.longBitsToDouble(Long.parseLong(line.substring(1)));
		if(line.startsWith("J"))
			return Long.parseLong(line.substring(1));
		
		if(line.startsWith("A")) {
			int size = Integer.parseInt(line.substring(1, line.indexOf(' ')));
			String type = line.substring(line.indexOf(' ')+1);
			if(type.startsWith("[L")) {
				Object[] array = (Object[])Array.newInstance(Class.forName(type.substring(2, type.length()-1)), size);
				for(int k = 0; k < size; k++)
					array[k] = readArg();
				return array;
			} else if(type.equals("[I")) {
				int[] array = new int[size];
				for(int k = 0; k < size; k++)
					array[k] = (Integer)readArg();
				return array;
			} else
				throw new AssertionError("can't make array of type "+type);
		}
		throw new Exception(line);
	}
}
