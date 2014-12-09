import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import net.minecraftforge.gradle.extrastuff.FmlCleanup;
import net.minecraftforge.gradle.extrastuff.GLConstantFixer;
import net.minecraftforge.gradle.extrastuff.McpCleanup;
import bytecode.BaseStreamingZipProcessor;

import com.github.abrarsyed.jastyle.ASFormatter;
import com.github.abrarsyed.jastyle.OptParser;


public class MCPCleanup extends BaseStreamingZipProcessor {
	public static void main(String[] args) {
		new MCPCleanup().go(args);
	}
	
	@Override
	protected boolean hasConfig() {
		return true;
	}
	
	ASFormatter formatter = new ASFormatter();
	
	@Override
	protected void loadConfig(File file) throws Exception {
		new OptParser(formatter).parseOptionFile(file);
	}
	
	@Override
	protected boolean shouldProcess(String name) {
		return name.endsWith(".java");
	}
	
	private static final Pattern BEFORE = Pattern.compile("(?m)((case|default).+(?:\\r\\n|\\r|\\n))(?:\\r\\n|\\r|\\n)");
    private static final Pattern AFTER  = Pattern.compile("(?m)(?:\\r\\n|\\r|\\n)((?:\\r\\n|\\r|\\n)[ \\t]+(case|default))");

    GLConstantFixer fixer = new GLConstantFixer();
    
    @Override
	protected byte[] process(byte[] in, String name) throws Exception {
		String text = new String(in, StandardCharsets.UTF_8);
		
		Reader reader;
        Writer writer;

        //System.err.println("processing comments");
        text = McpCleanup.stripComments(text);

        //System.err.println("fixing imports comments");
        text = McpCleanup.fixImports(text);

        //System.err.println("various other cleanup");
        text = McpCleanup.cleanup(text);

        //System.err.println("fixing OGL constants");
        text = fixer.fixOGL(text);

        //System.err.println("formatting source");
        reader = new StringReader(text);
        writer = new StringWriter();
        formatter.format(reader, writer);
        reader.close();
        writer.flush();
        writer.close();
        text = writer.toString();
        
        //System.err.println("applying FML transformations");
        text = BEFORE.matcher(text).replaceAll("$1");
        text = AFTER.matcher(text).replaceAll("$1");
        text = FmlCleanup.renameClass(text);

        return text.getBytes(StandardCharsets.UTF_8);
	}
}
