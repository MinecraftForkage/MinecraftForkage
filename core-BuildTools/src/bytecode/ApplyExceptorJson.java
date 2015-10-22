package bytecode;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import immibis.bon.com.immibis.json.JsonReader;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ApplyExceptorJson extends BaseStreamingJarProcessor {
	public static void main(String[] args) {
		new ApplyExceptorJson().go(args);
	}
	
	private Map<String, Map<String, Object>> raw_json;
	
	@Override
	public void loadConfig(Reader file) throws Exception {
		raw_json = (Map<String, Map<String, Object>>)JsonReader.readJSON(file);
	}
	
	@Override
	public ClassVisitor createClassVisitor(ClassVisitor parent) throws Exception {
		return new ApplyJsonClassVisitor(parent, raw_json);
	}
	
	public static Map<String, Map<String, Object>> loadJson(File file) throws IOException {
		try(FileReader in = new FileReader(file)) {
			return (Map<String, Map<String, Object>>)JsonReader.readJSON(in);
		}
	}
	
	
	public static class ApplyJsonClassVisitor extends ClassVisitor {
		
		private Map<String, String> outer;
		private List<Map<String, String>> inner;
		private String className;
		
		private Map<String, Map<String, Object>> raw_json;
		
		public ApplyJsonClassVisitor(ClassVisitor parent, Map<String, Map<String, Object>> raw_json) {
			super(Opcodes.ASM5, parent);
			this.raw_json = raw_json;
		}
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			
			Map<String, Object> json_this_class = raw_json.get(name);
			if(json_this_class != null) {
				outer = (Map<String, String>)json_this_class.get("enclosingMethod");
				inner = (List<Map<String, String>>)json_this_class.get("innerClasses");
			}
			
			if(inner == null)
				inner = Collections.emptyList();
			
			this.className = name;
		}
		
		@Override
		public void visitOuterClass(String owner, String name, String desc) {
			super.visitOuterClass(owner, name, desc);
			
			if(outer != null) {
				if(!Objects.equals(owner, outer.get("owner")) || !Objects.equals(name, outer.get("name")) || !Objects.equals(desc, outer.get("desc"))) {
					System.err.println("=== In class file ===");
					System.err.println("Owner: "+owner);
					System.err.println("Name: "+name);
					System.err.println("Descriptor: "+desc);
					System.err.println("=== In JSON ===");
					System.err.println("Owner: "+outer.get("owner"));
					System.err.println("Name: "+outer.get("name"));
					System.err.println("Descriptor: "+outer.get("desc"));
					throw new RuntimeException("Class file conflicts with JSON data (class name: "+className+")");
				}
				outer = null;
			}
		}
		
		@Override
		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			super.visitInnerClass(name, outerName, innerName, access);
			
			Iterator<Map<String, String>> it = inner.iterator();
			while(it.hasNext()) {
				Map<String, String> entry = it.next();
				
				if(entry.get("inner_class").equals(name))
					it.remove();
			}
		}
		
		@Override
		public void visitEnd() {
			
			if(outer != null)
				super.visitOuterClass(outer.get("owner"), outer.get("name"), outer.get("desc"));
			
			for(Map<String, String> entry : inner) {
				int access = entry.containsKey("access") ? Integer.parseInt(entry.get("access"), 16) : 0;
				super.visitInnerClass(entry.get("inner_class"), entry.get("outer_class"), entry.get("inner_name"), access);
			}
			
			super.visitEnd();
		}
		
		
	}
}
