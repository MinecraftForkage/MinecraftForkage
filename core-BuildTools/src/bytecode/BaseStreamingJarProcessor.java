package bytecode;
import installer.Utils;

import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;


public abstract class BaseStreamingJarProcessor {
	public void go(String[] args) {
		if(args.length != (hasConfig() ? 1 : 0)) {
			if(hasConfig())
				System.err.println("Usage: java "+getClass().getSimpleName()+" config.txt < infile.jar > outfile.jar");
			else
				System.err.println("Usage: java "+getClass().getSimpleName()+" < infile.jar > outfile.jar");
			System.exit(1);
		}
		
		try {
			if(hasConfig())
				try (Reader in = new FileReader(args[0])){
					loadConfig(in);
				}
			
			go(System.in, System.out);
		} catch(Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
		
		
	}
	public final void go(InputStream in, OutputStream out) throws Exception {
			
		try (ZipInputStream zipIn = new ZipInputStream(in)) {
			try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
				ZipEntry ze;
				while((ze = zipIn.getNextEntry()) != null) {
					
					zipOut.putNextEntry(new ZipEntry(ze.getName()));
					
					if(!ze.getName().endsWith(".class")) {
						Utils.copyStream(zipIn, zipOut);
						
					} else {
						// class file
						ClassWriter cw = new ClassWriter(0);
						new ClassReader(zipIn).accept(createClassVisitor(cw), 0);
						
						zipOut.write(cw.toByteArray());
					}
					
					zipIn.closeEntry();
					zipOut.closeEntry();
				}
			}
		}
	}
	
	public boolean hasConfig() {return true;}
	public abstract void loadConfig(Reader file) throws Exception;
	public abstract ClassVisitor createClassVisitor(ClassVisitor parent) throws Exception;
}
