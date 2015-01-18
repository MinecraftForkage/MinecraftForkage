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

package cpw.mods.fml.common.asm;

import java.io.File;
import java.util.Map;

import net.minecraft.launchwrapper.LaunchClassLoader;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import cpw.mods.fml.relauncher.IFMLCallHook;

public class FMLSanityChecker implements IFMLCallHook
{
    public static File fmlLocation;

    @Override
    public Void call() throws Exception
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
        LaunchClassLoader cl = (LaunchClassLoader) data.get("classLoader");
        File mcDir = (File)data.get("mcLocation");
        fmlLocation = (File)data.get("coremodLocation");
        FMLDeobfuscatingRemapper.INSTANCE.setup(mcDir, cl, (String) data.get("deobfuscationFileName"));
    }

}
