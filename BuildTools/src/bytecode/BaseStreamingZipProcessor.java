package bytecode;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public abstract class BaseStreamingZipProcessor {
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
				loadConfig(new File(args[0]));
			
			try (ZipInputStream zipIn = new ZipInputStream(System.in)) {
				try (ZipOutputStream zipOut = new ZipOutputStream(System.out)) {
					ZipEntry ze;
					while((ze = zipIn.getNextEntry()) != null) {
						
						zipOut.putNextEntry(new ZipEntry(ze.getName()));
						
						if(ze.getName().endsWith("/") || !shouldProcess(ze.getName())) {
							copyResource(zipIn, zipOut);
						
						} else {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							copyResource(zipIn, baos);

							byte[] bytes = baos.toByteArray();
							bytes = process(bytes, ze.getName());
							zipOut.write(bytes);
						}
						
						zipIn.closeEntry();
						zipOut.closeEntry();
					}
				}
			}
			
			done();
			
		} catch(Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
		
		System.exit(0);
	}
	
	protected void done() throws Exception {}
	
	protected abstract boolean hasConfig();
	protected abstract boolean shouldProcess(String name);
	protected abstract void loadConfig(File file) throws Exception;
	protected abstract byte[] process(byte[] in, String name) throws Exception;

	private static byte[] buffer = new byte[32768];
	public static void copyResource(InputStream zipIn, OutputStream zipOut) throws IOException {
		while(true) {
			int read = zipIn.read(buffer);
			if(read <= 0)
				break;
			zipOut.write(buffer, 0, read);
		}
	}
}

