/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     cpw - implementation
 */

package cpw.mods.fml.relauncher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.Level;

import net.minecraft.launchwrapper.LaunchClassLoader;

public class FMLInjectionData
{
    static File minecraftHome;
    final static String major = "7";
    final static String minor = "10";
    final static String rev = "85";
    final static String build = "1277";
    final static String mccversion = "1.7.10";
    static String mcpversion;
    static String deobfuscationDataHash;

    public static List<String> containers = new ArrayList<String>();

    static void build(File mcHome, LaunchClassLoader classLoader)
    {
        minecraftHome = mcHome;
        InputStream stream = classLoader.getResourceAsStream("fmlversion.properties");
        Properties properties = new Properties();

        if (stream != null)
        {
            try
            {
                properties.load(stream);
            }
            catch (IOException ex)
            {
                FMLRelaunchLog.log(Level.ERROR, ex, "Could not get FML version information - corrupted installation detected!");
            }
        }

        mcpversion = properties.getProperty("fmlbuild.mcpversion", "missing");
        deobfuscationDataHash = properties.getProperty("fmlbuild.deobfuscation.hash","deadbeef");
    }

    static String debfuscationDataName()
    {
        return "/deobfuscation_data-missing.lzma";
    }
    public static Object[] data()
    {
        return new Object[] { major, minor, rev, build, mccversion, mcpversion, minecraftHome, containers };
    }
}
