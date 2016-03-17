package cpw.mods.fml.common.asm.transformers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import net.minecraftforkage.PackerDataUtils;

import com.google.common.io.ByteSource;
import com.google.common.reflect.TypeToken;

import cpw.mods.fml.relauncher.FMLRelaunchLog;

public class ModAccessTransformer extends AccessTransformer {
    private static List<String> configPathList = new ArrayList<String>();
    
	public ModAccessTransformer() throws Exception
    {
        super(ModAccessTransformer.class);
        
        try {
	        List<String> configPathList = PackerDataUtils.read("mcforkage-FMLAT.json", new TypeToken<List<String>>(){}); 
	
	        for (final String configPath : configPathList)
	        {
	            int old_count = getModifiers().size();
	            processATFile(new ByteSource() {
					@Override
					public InputStream openStream() throws IOException {
						InputStream stream = ModAccessTransformer.class.getResourceAsStream("/META-INF/"+configPath);
						if(stream == null) {
							new IOException("Resource not found: /META-INF/"+configPath).printStackTrace();
							return new ByteArrayInputStream(new byte[0]);
						}
						return stream;
					}
				}.asCharSource(Charset.forName("UTF-8")));
	            int added = getModifiers().size() - old_count;
	            if (added > 0)
	            {
	                FMLRelaunchLog.fine("Loaded %d rules from mod AccessTransforme mod jar file %s\n", added, configPathList);
	            }
	        }
        } catch(Throwable t) {
        	// LaunchClassLoader (which instantiates this) swallows exceptions...
        	t.printStackTrace();
        	System.exit(1);
        }
    }
}
