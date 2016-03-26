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

package cpw.mods.fml.common.discovery;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.discovery.asm.ASMModParser;


public class ModCandidate
{
    private File classPathRoot;
    private File modContainer;
    private ContainerType sourceType;
    private boolean classpath;
    private List<String> baseModTypes = Lists.newArrayList();
    private boolean isMinecraft;
    private List<ASMModParser> baseModCandidateTypes = Lists.newArrayListWithCapacity(1);
    private List<ModContainer> mods;
    private ASMDataTable table;
    // XXX Compatibility with Extra Utilities
    private Set<String> classList = new HashSet<String>();

    public ModCandidate(File classPathRoot, File modContainer, ContainerType sourceType)
    {
        this(classPathRoot, modContainer, sourceType, false, false);
    }
    public ModCandidate(File classPathRoot, File modContainer, ContainerType sourceType, boolean isMinecraft, boolean classpath)
    {
        this.classPathRoot = classPathRoot;
        this.modContainer = modContainer;
        this.sourceType = sourceType;
        this.isMinecraft = isMinecraft;
        this.classpath = classpath;
    }

    public File getClassPathRoot()
    {
        return classPathRoot;
    }

    public File getModContainer()
    {
        return modContainer;
    }

    public ContainerType getSourceType()
    {
        return sourceType;
    }
    public List<ModContainer> explore(ASMDataTable table)
    {
        this.table = table;
        this.mods = sourceType.findMods(this, table);
        if (!baseModCandidateTypes.isEmpty())
        {
            FMLLog.info("Attempting to reparse the mod container %s", getModContainer().getName());
            this.mods = sourceType.findMods(this, table);
        }
        return this.mods;
    }

    public void addClassEntry(String name)
    {
        String className = name.substring(0, name.lastIndexOf('.')); // strip the .class
        // XXX Compatibility with Extra Utilities
        if(className.startsWith("cofh/api/energy/") || className.startsWith("com/rwtema/extrautils/network/packets/"))
            classList.add(className);
        className = className.replace('/','.');
        int pkgIdx = className.lastIndexOf('.');
        if (pkgIdx > -1)
        {
            String pkg = className.substring(0,pkgIdx);
            this.table.registerPackage(this,pkg);
        }
    }

    public boolean isClasspath()
    {
        return classpath;
    }
    public void rememberBaseModType(String className)
    {
        baseModTypes.add(className);
    }
    public List<String> getRememberedBaseMods()
    {
        return baseModTypes;
    }
    public boolean isMinecraftJar()
    {
        return isMinecraft;
    }
    public void rememberModCandidateType(ASMModParser modParser)
    {
        baseModCandidateTypes.add(modParser);
    }
    
    // XXX Compatibility with Extra Utilities
    public Set<String> getClassList()
    {
    	return Collections.unmodifiableSet(classList);
    }
    
    public List<ModContainer> getContainedMods()
    {
        return mods;
    }
    // FORKAGE DELETED: public List<String> getContainedPackages()
}
