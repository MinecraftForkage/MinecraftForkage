package installer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import immibis.bon.com.immibis.json.JsonReader;
import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;
import net.mcforkage.ant.ApplyDiff2Task;
import net.mcforkage.ant.MergeJarsTask;
import net.mcforkage.ant.UncompressDiff2Task;
import net.mcforkage.ant.compression.BitInputStream;
import net.mcforkage.ant.diff2.ApplyDiff2;
import net.mcforkage.ant.diff2.UncompressDiff2;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import bytecode.AddOBFID;
import bytecode.ApplyAT;
import bytecode.ApplyExceptions;
import bytecode.ApplyExceptorJson;
import bytecode.ApplyParamNames;
import bytecode.ApplySRG;
import bytecode.BaseStreamingJarProcessor;
import bytecode.Bytecode2Text;
import bytecode.JarMerger;
import bytecode.RemoveGenericMethods;
import bytecode.SortZipEntries;
import bytecode.Text2Bytecode;
import bytecode.TrimBytecode;
import bytecode.patchfile.PatchFile;

public class Installer {
	public static File install(File clientJar, File serverJar, File tempDir, final Map<String, byte[]> installData, final ProgressDialog dlg) throws Exception {
		File merged = new File(tempDir, "merged.jar");
		File srg = new File(tempDir, "srg.jar");
		File unsorted = new File(tempDir, "unsorted.jar");
		File sorted = new File(tempDir, "sorted.jar");
		
		File unpatchedBytecodeFile;
		
		int numClassFiles = 0;
		
		final int[] numClassFilesRef = {0};
		
		if(Boolean.getBoolean("minecraftforkage.installer.readUnpatchedBytecodeFromFile")) {
			unpatchedBytecodeFile = new File("../bytecode-orig.txt");
		}
		else
		{
			if(dlg != null) dlg.startIndeterminate("Merging JARs");
			JarMerger.merge(clientJar, serverJar, merged, new InputStreamReader(new ByteArrayInputStream(installData.get("mcp_merge.cfg"))), dlg);
			
			if(dlg != null) dlg.startIndeterminate("Applying deobfuscation mapping");
			ApplySRG.apply(new InputStreamReader(new ByteArrayInputStream(installData.get("joined.srg"))), merged, srg, dlg);
			
			final Object exceptor_json = JsonReader.readJSON(new InputStreamReader(new ByteArrayInputStream(installData.get("exceptor.json"))));
			final List<ApplyAT.Pattern> fml_at = ApplyAT.loadActions(new InputStreamReader(new ByteArrayInputStream(installData.get("fml_at.cfg"))));
			final List<ApplyAT.Pattern> forge_at = ApplyAT.loadActions(new InputStreamReader(new ByteArrayInputStream(installData.get("forge_at.cfg"))));
			final ApplyExceptions exceptions = new ApplyExceptions();
			final ApplyParamNames params = new ApplyParamNames();
			final AddOBFID obfid = new AddOBFID();
			final TrimBytecode trim = new TrimBytecode();
			final RemoveGenericMethods removeGenericBridges = new RemoveGenericMethods();
			
			exceptions.loadConfig(new InputStreamReader(new ByteArrayInputStream(installData.get("joined.exc"))));
			params.loadConfig(new InputStreamReader(new ByteArrayInputStream(installData.get("joined.exc"))));
			obfid.loadConfig(new InputStreamReader(new ByteArrayInputStream(installData.get("joined.exc"))));
			
			if(dlg != null) dlg.startIndeterminate("Processing bytecode");
			new BaseStreamingJarProcessor() {
				@Override
				public void loadConfig(Reader file) throws Exception {
				}
				
				@Override
				public ClassVisitor createClassVisitor(ClassVisitor cv) throws Exception {
					cv = trim.createClassVisitor(cv);
					cv = removeGenericBridges.createClassVisitor(cv);
					cv = obfid.createClassVisitor(cv);
					cv = params.createClassVisitor(cv);
					cv = exceptions.createClassVisitor(cv);
					cv = new ApplyAT.ApplyATClassVisitor(cv, forge_at);
					cv = new ApplyAT.ApplyATClassVisitor(cv, fml_at);
					cv = new ApplyExceptorJson.ApplyJsonClassVisitor(cv, (Map)exceptor_json);
					numClassFilesRef[0]++;
					return cv;
				}
			}.go(new FileInputStream(srg), new FileOutputStream(unsorted));
			
			numClassFiles = numClassFilesRef[0];
			
			if(dlg != null) dlg.startIndeterminate("Sorting class files");
			SortZipEntries.sort(unsorted, null, false, new FileOutputStream(sorted));
			
			unpatchedBytecodeFile = new File(tempDir, "bytecode-unpatched.txt");
			
			if(dlg != null) dlg.startIndeterminate("Converting bytecode to patchable format");
			try (PrintStream fout = new PrintStream(new BufferedOutputStream(new FileOutputStream(unpatchedBytecodeFile), 262144))) {
				if(dlg != null) dlg.initProgressBar(0, numClassFiles);
				Bytecode2Text.go(new FileInputStream(sorted), fout, dlg);
			}
		}
		
		
		
		File patchedBytecodeFile = new File(tempDir, "bytecode-patched.txt");
		
		if(dlg != null) dlg.startIndeterminate("Applying bytecode patch");
		{
			try (final PipedReader patch_in = new PipedReader()) {
				new Thread() {
					@Override
					public void run() {
						try (PrintWriter patch_out = new PrintWriter(new PipedWriter(patch_in))) {
							try (BitInputStream compressed_patch_in = new BitInputStream(new ByteArrayInputStream(installData.get("bytecode.patch2z")))) {
								UncompressDiff2.uncompress(compressed_patch_in, patch_out);
							}
						} catch(IOException e) {
							throw new RuntimeException(e);
						}
					}
				}.start();

				try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(patchedBytecodeFile), StandardCharsets.UTF_8))) {
					ApplyDiff2.apply(ApplyDiff2.readFile(unpatchedBytecodeFile), new BufferedReader(patch_in), out);
				}
			}
		}
		
		if(dlg != null) dlg.startIndeterminate("Converting back to JAR format");
		ByteArrayOutputStream patchedJarBAOS = new ByteArrayOutputStream();
		try (JarOutputStream patchedJarOut = new JarOutputStream(patchedJarBAOS)) {
			try (BufferedReader patchedBytecodeIn = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(patchedBytecodeFile), 262144), StandardCharsets.UTF_8))) {
				if(dlg != null) dlg.initProgressBar(0, numClassFiles);
				new Text2Bytecode(patchedBytecodeIn, patchedJarOut, dlg).run();
			}
			Pack200.newUnpacker().unpack(new ByteArrayInputStream(installData.get("new-classes.pack")), patchedJarOut);
			
			try (ZipInputStream clientJarIn = new ZipInputStream(new FileInputStream(clientJar))) {
				copyResourcesOnly(clientJarIn, patchedJarOut);
			}
		}
		
		byte[] patchedJarBytes = patchedJarBAOS.toByteArray();
		patchedJarBAOS = null;
		
		if(dlg != null) dlg.startIndeterminate("Extracting superclasses");
		if(dlg != null) dlg.initProgressBar(0, numClassFiles);
		
		// find superclass of every class (not interface)
		// Previous step added some class files, but we don't know how many, so re-count that too.
		
		numClassFilesRef[0] = 0;
		final Map<String, String> superclasses = new HashMap<String, String>();
		try (ZipInputStream patchedJarIn = new ZipInputStream(new ByteArrayInputStream(patchedJarBytes))) {
			ZipEntry ze;
			while((ze = patchedJarIn.getNextEntry()) != null) {
				if(ze.getName().endsWith(".class")) {
					numClassFilesRef[0]++;
					new ClassReader(patchedJarIn).accept(new ClassVisitor(Opcodes.ASM5) {
						@Override
						public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
							if(dlg != null) dlg.incrementProgress(1);
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
		numClassFiles = numClassFilesRef[0];
		
		File finalResultJar = new File(tempDir, "patched.jar");
		
		if(dlg != null) dlg.startIndeterminate("Pre-verifying JAR");
		if(dlg != null) dlg.initProgressBar(0, numClassFiles);
		
		// compute frames and maxes for all methods; bytecode patching doesn't preserve them
		try (ZipInputStream patchedJarIn = new ZipInputStream(new ByteArrayInputStream(patchedJarBytes))) {
			try (ZipOutputStream completeJarOut = new ZipOutputStream(new FileOutputStream(finalResultJar))) {
				ZipEntry ze;
				while((ze = patchedJarIn.getNextEntry()) != null) {
					completeJarOut.putNextEntry(new ZipEntry(ze.getName()));
					
					if(ze.getName().endsWith(".class")) {
						
						if(dlg != null) dlg.incrementProgress(1);
						
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
		
		return finalResultJar;
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