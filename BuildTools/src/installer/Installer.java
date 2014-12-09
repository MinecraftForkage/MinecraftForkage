package installer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import immibis.bon.com.immibis.json.JsonReader;
import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import patcher.PatchFile;
import bytecode.AddOBFID;
import bytecode.ApplyAT;
import bytecode.ApplyExceptions;
import bytecode.ApplyExceptorJson;
import bytecode.ApplyParamNames;
import bytecode.ApplySRG;
import bytecode.BaseStreamingJarProcessor;
import bytecode.Bytecode2Text;
import bytecode.JarMerger;
import bytecode.SortZipEntries;
import bytecode.Text2Bytecode;
import bytecode.TrimBytecode;

public class Installer {
	public static void main(String[] args) {
		
	}

	public static void install(File clientJar, File serverJar, File tempDir, Map<String, byte[]> installData) throws Exception {
		File merged = new File(tempDir, "merged.jar");
		File srg = new File(tempDir, "srg.jar");
		File unsorted = new File(tempDir, "unsorted.jar");
		File sorted = new File(tempDir, "sorted.jar");
		
		
		JarMerger.merge(clientJar, serverJar, merged, new InputStreamReader(new ByteArrayInputStream(installData.get("mcp_merge.cfg"))));
		ApplySRG.apply(new InputStreamReader(new ByteArrayInputStream(installData.get("joined.srg"))), merged, srg);
		
		final Object exceptor_json = JsonReader.readJSON(new InputStreamReader(new ByteArrayInputStream(installData.get("exceptor.json"))));
		final List<ApplyAT.Pattern> fml_at = ApplyAT.loadActions(new InputStreamReader(new ByteArrayInputStream(installData.get("fml_at.cfg"))));
		final List<ApplyAT.Pattern> forge_at = ApplyAT.loadActions(new InputStreamReader(new ByteArrayInputStream(installData.get("forge_at.cfg"))));
		final ApplyExceptions exceptions = new ApplyExceptions();
		final ApplyParamNames params = new ApplyParamNames();
		final AddOBFID obfid = new AddOBFID();
		final TrimBytecode trim = new TrimBytecode();
		
		exceptions.loadConfig(new InputStreamReader(new ByteArrayInputStream(installData.get("joined.exc"))));
		params.loadConfig(new InputStreamReader(new ByteArrayInputStream(installData.get("joined.exc"))));
		obfid.loadConfig(new InputStreamReader(new ByteArrayInputStream(installData.get("joined.exc"))));
		
		new BaseStreamingJarProcessor() {
			@Override
			protected void loadConfig(Reader file) throws Exception {
			}
			
			@Override
			protected ClassVisitor createClassVisitor(ClassVisitor cv) throws Exception {
				cv = trim.createClassVisitor(cv);
				cv = obfid.createClassVisitor(cv);
				cv = params.createClassVisitor(cv);
				cv = exceptions.createClassVisitor(cv);
				cv = new ApplyAT.ApplyATClassVisitor(cv, forge_at);
				cv = new ApplyAT.ApplyATClassVisitor(cv, fml_at);
				cv = new ApplyExceptorJson.ApplyJsonClassVisitor(cv, (Map)exceptor_json);
				return cv;
			}
		}.go(new FileInputStream(srg), new FileOutputStream(unsorted));
		
		SortZipEntries.sort(unsorted, null, false, new FileOutputStream(sorted));
		
		ByteArrayOutputStream bcOrigBAOS = new ByteArrayOutputStream();
		Bytecode2Text.go(new FileInputStream(sorted), new PrintStream(bcOrigBAOS));
		byte[] bytecodeTextBytes = bcOrigBAOS.toByteArray();
		bcOrigBAOS = null;
		
		PatchFile bytecodePatch;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(installData.get("bytecode.patch"))))) {
			bytecodePatch = PatchFile.load(br);
		}
		
		bytecodeTextBytes = bytecodePatch.applyPatches(bytecodeTextBytes, "build/bytecode-orig.txt");
		try (FileOutputStream fos = new FileOutputStream(new File(tempDir, "bytecode-patched.txt"))) {
			fos.write(bytecodeTextBytes);
		}
		
		ByteArrayOutputStream patchedJarBAOS = new ByteArrayOutputStream();
		try (JarOutputStream patchedJarOut = new JarOutputStream(patchedJarBAOS)) {
			new Text2Bytecode(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytecodeTextBytes))), patchedJarOut).run();;
			Pack200.newUnpacker().unpack(new ByteArrayInputStream(installData.get("new-classes.pack")), patchedJarOut);
			
			try (ZipInputStream clientJarIn = new ZipInputStream(new FileInputStream(clientJar))) {
				copyResourcesOnly(clientJarIn, patchedJarOut);
			}
		}
		
		byte[] patchedJarBytes = patchedJarBAOS.toByteArray();
		patchedJarBAOS = null;
		
		// find superclass of every class (not interface)
		final Map<String, String> superclasses = new HashMap<String, String>();
		try (ZipInputStream patchedJarIn = new ZipInputStream(new ByteArrayInputStream(patchedJarBytes))) {
			ZipEntry ze;
			while((ze = patchedJarIn.getNextEntry()) != null) {
				if(ze.getName().endsWith(".class")) {
					new ClassReader(patchedJarIn).accept(new ClassVisitor(Opcodes.ASM5) {
						@Override
						public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
							if((access & Opcodes.ACC_INTERFACE) == 0) {
								System.err.println("superclass of "+name+" is "+superName);
								superclasses.put(name, superName);
							}
						}
					}, 0);
				}
				patchedJarIn.closeEntry();
			}
		}
		
		// compute frames and maxes for all methods; bytecode patching doesn't preserve them
		try (ZipInputStream patchedJarIn = new ZipInputStream(new ByteArrayInputStream(patchedJarBytes))) {
			try (ZipOutputStream completeJarOut = new ZipOutputStream(new FileOutputStream(new File(tempDir, "patched.jar")))) {
				ZipEntry ze;
				while((ze = patchedJarIn.getNextEntry()) != null) {
					completeJarOut.putNextEntry(new ZipEntry(ze.getName()));
					
					if(ze.getName().endsWith(".class")) {
						System.err.println("Generating frames for "+ze.getName());
						ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
							@Override
							protected String getCommonSuperClass(String type1, String type2) {
								if(isSuperclassOf(type1, type2, superclasses)) return type1;
								if(isSuperclassOf(type2, type1, superclasses)) return type2;
								do {
									//System.err.println("getCommonSuperClass("+type1+","+type2+")");
					                String next = superclasses.get(type1);
					                if(next == null) {
					                	System.err.println("Don't know superclass of "+type1);
										return "java/lang/Object";
					                }
					                type1 = next;
					            } while (type1 != null && !isSuperclassOf(type1, type2, superclasses));
								return type1;
							}

							private boolean isSuperclassOf(String _super, String _sub, Map<String, String> superclasses) {
								if(_super.equals(_sub)) return true;
								if(_sub.equals("java/lang/Object")) return false;
								String next = superclasses.get(_sub);
								//System.err.println("isSuperclassOf("+_super+","+_sub+")");
								if(next == null) {
									System.err.println("Don't know superclass of "+_sub);
									return false;
								}
								return isSuperclassOf(_super, next, superclasses);
							}
						};
						new ClassReader(patchedJarIn).accept(cw, 0);
						
						completeJarOut.write(cw.toByteArray());
						
					} else {
						Utils.copyStream(patchedJarIn, completeJarOut);
					}
					
					completeJarOut.closeEntry();
					patchedJarIn.closeEntry();
				}
			}
		}
	}

	private static void copyResourcesOnly(ZipInputStream clientJarIn, ZipOutputStream patchedJarOut) throws Exception {
		ZipEntry ze;
		while((ze = clientJarIn.getNextEntry()) != null) {
			if(!ze.getName().endsWith("/") && !ze.getName().endsWith(".class") && !ze.getName().startsWith("META-INF/")) {
				patchedJarOut.putNextEntry(new ZipEntry(ze.getName()));
				Utils.copyStream(clientJarIn, patchedJarOut);
				patchedJarOut.closeEntry();
			}
			clientJarIn.closeEntry();
		}
	}
}

class InstallerTestMain {
	public static void main(String[] args) throws Exception {
		Map<String, byte[]> installData;
		try (InputStream in = new LzmaInputStream(new BufferedInputStream(new FileInputStream(new File(args[0]))), new Decoder())) {
			installData = Utils.readZip(in);
		}
		File clientJar = new File(args[1]);
		File serverJar = new File(args[2]);
		File tempDir = new File(args[3]);
		
		Installer.install(clientJar, serverJar, tempDir, installData);
	}
}