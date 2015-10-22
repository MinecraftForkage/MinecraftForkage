package bytecode;

import installer.ProgressDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class JarMerger {
	private static class Config {
		Collection<String> ignore = new ArrayList<>();

		public boolean isIgnored(String name) {
			for(String s : ignore)
				if(name.startsWith(s))
					return true;
			return false;
		}
	}
	
	public static void main(String[] args) {
		if(args.length != 4) {
			System.err.println("Usage: java JarMerger <clientpath> <serverpath> <outputpath> <configpath>");
			System.exit(1);
		}
		
		try {
			merge(new File(args[0]), new File(args[1]), new File(args[2]), new FileReader(new File(args[3])), null);
		} catch(Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
	
	// Closes `config`.
	public static void merge(File client, File server, File outFile, Reader config, ProgressDialog dlg) throws IOException {
		
		Config cfg = readConfig(config);
		
		try (ZipFile clientJF = new JarFile(client)) {
			try (ZipFile serverJF = new JarFile(server)) {
				try (ZipOutputStream outJF = new ZipOutputStream(new FileOutputStream(outFile))) {
					merge(clientJF, serverJF, outJF, cfg, dlg);
				}
			}
		}
    }

	// Closes `stream`.
	@SuppressWarnings("resource") // eclipse compiler bug causes spurious warning on `br` in try-with-resources statement
	private static Config readConfig(Reader stream) throws IOException {
		Config cfg = new Config();
		try (BufferedReader br = new BufferedReader(stream)) {
			String line;
			while((line = br.readLine()) != null) {
				if(line.contains("#"))
					line = line.split("#")[0];
				
				char cmd = line.charAt(0);
				String arg = line.substring(1).trim();
				
				if(cmd == '^')
					cfg.ignore.add(arg);
				else
					throw new IOException("Unknonwn mcp_merge.cfg command character: "+cmd);
			}
		}
		return cfg;
	}
	
	
	
	
	
	// Does not close `clientJF`, `serverJF` or `outJF`.
	public static void merge(ZipFile clientJF, ZipFile serverJF, ZipOutputStream outJF, Config cfg, ProgressDialog dlg) throws IOException {
		Set<String> seenResources = new TreeSet<>();
		Map<String, ZipEntry> clientClasses = new TreeMap<>();
		Map<String, ZipEntry> serverClasses = new TreeMap<>();
		Set<String> commonClasses = new TreeSet<>();
		
		gatherClassNamesAndCopyResources(clientJF, outJF, seenResources, clientClasses, cfg);
		gatherClassNamesAndCopyResources(serverJF, outJF, seenResources, serverClasses, cfg);
		
		commonClasses.addAll(clientClasses.keySet());
		commonClasses.retainAll(serverClasses.keySet());
		
		writeOneSidedClasses(clientJF, outJF, clientClasses, serverClasses.keySet(), "CLIENT");
		writeOneSidedClasses(serverJF, outJF, serverClasses, clientClasses.keySet(), "SERVER");
		
		writeMergedClasses(clientJF, serverJF, outJF, commonClasses, dlg);
	}
	
	private static void writeOneSidedClasses(ZipFile inJF, ZipOutputStream outJF, Map<String, ZipEntry> classes, Set<String> otherSideClasses, final String sideAnnotation) throws IOException {
		for(Map.Entry<String, ZipEntry> entry : classes.entrySet()) {
			if(otherSideClasses.contains(entry.getKey()))
				continue;
			
			byte[] data;
			try (InputStream entryIn = inJF.getInputStream(entry.getValue())) {
				ClassReader cr = new ClassReader(entryIn);
				ClassWriter cw = new ClassWriter(cr, 0);
				cr.accept(new ClassVisitor(Opcodes.ASM4, cw) {
					@Override
					public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
						super.visit(version, access, name, signature, superName, interfaces);
						
						AnnotationVisitor av = super.visitAnnotation("Lcpw/mods/fml/relauncher/SideOnly;", true);
						if(av != null) {
							av.visitEnum("value", "Lcpw/mods/fml/relauncher/Side;", sideAnnotation);
							av.visitEnd();
						}
					}
				}, 0);
				data = cw.toByteArray();
			}
			outJF.putNextEntry(new ZipEntry(entry.getValue().getName()));
			outJF.write(data);
			outJF.closeEntry();
		}
	}
	
	private static void writeMergedClasses(ZipFile clientJF, ZipFile serverJF, ZipOutputStream outJF, Set<String> commonClasses, ProgressDialog dlg) throws IOException {
		if(dlg != null) dlg.initProgressBar(0, commonClasses.size());
		for(String classname : commonClasses) {
			String filename = classname.replace('.', '/') + ".class";
			
			ClassNode clientCN = new ClassNode();
			ClassNode serverCN = new ClassNode();
			
			try (InputStream entryIn = clientJF.getInputStream(clientJF.getEntry(filename))) {
				new ClassReader(entryIn).accept(clientCN, 0);
			}
			
			try (InputStream entryIn = serverJF.getInputStream(serverJF.getEntry(filename))) {
				new ClassReader(entryIn).accept(serverCN, 0);
			}
			
			mergeClasses(clientCN, serverCN);
			
			ClassWriter cw = new ClassWriter(0);
			clientCN.accept(cw);
			
			outJF.putNextEntry(new ZipEntry(filename));
			outJF.write(cw.toByteArray());
			outJF.closeEntry();
			
			if(dlg != null) dlg.incrementProgress(1);
		}
	}

	private static void gatherClassNamesAndCopyResources(ZipFile inJF, ZipOutputStream outJF, Set<String> seenResources, Map<String, ZipEntry> thisSideClasses, Config cfg) throws IOException {
		
		byte[] buffer = new byte[32768];
		
		Enumeration<? extends ZipEntry> entries = inJF.entries();
		while(entries.hasMoreElements()) {
			ZipEntry ze = entries.nextElement();
			
			if(ze.getName().endsWith(".class")) {
				if(!cfg.isIgnored(ze.getName())) {
					String className = ze.getName().substring(0, ze.getName().length() - 6).replace('/', '.');
					thisSideClasses.put(className, ze);
				}
			
			} else if(seenResources.add(ze.getName()) && !ze.getName().equals("META-INF/MANIFEST.MF")) {
				if(!cfg.isIgnored(ze.getName())) {
					outJF.putNextEntry(new ZipEntry(ze.getName()));
					try (InputStream in = inJF.getInputStream(ze)) {
						while(true) {
							int read = in.read(buffer);
							if(read <= 0)
								break;
							outJF.write(buffer, 0, read);
						}
					}
					outJF.closeEntry();
				}
			}
		}
	}
	
	private static AnnotationNode getAnnotationNode(String side) {
		AnnotationNode an = new AnnotationNode("Lcpw/mods/fml/relauncher/SideOnly;");
		an.values = Arrays.<Object>asList("value", new String[] {"Lcpw/mods/fml/relauncher/Side;", side});
		return an;
	}
	private static void addAnnotationNode(FieldNode fn, String side) {
		if(fn.visibleAnnotations == null)
			fn.visibleAnnotations = new ArrayList<>(1);
		fn.visibleAnnotations.add(getAnnotationNode(side));
	}
	private static void addAnnotationNode(MethodNode mn, String side) {
		if(mn.visibleAnnotations == null)
			mn.visibleAnnotations = new ArrayList<>(1);
		mn.visibleAnnotations.add(getAnnotationNode(side));
	}
	
	
	// Merge server-only fields and methods into client. Add SideOnly annotations to fields and methods.
	// On exit, `client` holds the result and `server` is trashed.
	private static void mergeClasses(ClassNode client, ClassNode server) {
		
		mergeFields(client, server);
		mergeMethods(client, server);
	}
	
	private static void mergeFields(ClassNode client, ClassNode server) {
		Map<String, FieldNode> clientFields = new TreeMap<>();
		Map<String, FieldNode> serverFields = new TreeMap<>();
		Set<String> fieldNames = new TreeSet<>();
		for(FieldNode fn : client.fields) {clientFields.put(fn.name, fn); fieldNames.add(fn.name);}
		for(FieldNode fn : server.fields) {serverFields.put(fn.name, fn); fieldNames.add(fn.name);}
		for(String fname : fieldNames) {
			FieldNode cl = clientFields.get(fname);
			FieldNode sv = serverFields.get(fname);
			if(cl != null && sv != null)
				continue; // nothing to do
			
			if(cl != null) {
				addAnnotationNode(cl, "CLIENT");
				
			} else if(sv != null) {
				addAnnotationNode(sv, "SERVER");
				client.fields.add(sv);
				
			} else
				throw new AssertionError("shouldn't get here");
		}
	}
	
	private static void mergeMethods(ClassNode client, ClassNode server) {
		Map<String, MethodNode> clientMethods = new TreeMap<>();
		Map<String, MethodNode> serverMethods = new TreeMap<>();
		Set<String> methods = new TreeSet<>();
		for(MethodNode fn : client.methods) {clientMethods.put(fn.name+fn.desc, fn); methods.add(fn.name+fn.desc);}
		for(MethodNode fn : server.methods) {serverMethods.put(fn.name+fn.desc, fn); methods.add(fn.name+fn.desc);}
		for(String fname : methods) {
			MethodNode cl = clientMethods.get(fname);
			MethodNode sv = serverMethods.get(fname);
			if(cl != null && sv != null)
				continue; // nothing to do
			
			if(cl != null) {
				addAnnotationNode(cl, "CLIENT");
				
			} else if(sv != null) {
				addAnnotationNode(sv, "SERVER");
				client.methods.add(sv);
				
			} else
				throw new AssertionError("shouldn't get here");
		}
	}

	
}
