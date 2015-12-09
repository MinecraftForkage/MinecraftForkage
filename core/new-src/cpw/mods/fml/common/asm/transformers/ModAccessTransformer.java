package cpw.mods.fml.common.asm.transformers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import net.minecraftforkage.PackerDataUtils;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.reflect.TypeToken;

import cpw.mods.fml.relauncher.FMLRelaunchLog;

public class ModAccessTransformer extends AccessTransformer {
    private static List<String> configPathList = new ArrayList<String>();
    
    @SuppressWarnings("unchecked")
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
						return ModAccessTransformer.class.getResourceAsStream("/META-INF/"+configPath);
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
