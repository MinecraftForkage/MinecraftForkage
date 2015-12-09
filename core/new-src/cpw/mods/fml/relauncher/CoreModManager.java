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

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforkage.PackerDataUtils;

import org.apache.logging.log4j.Level;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;
import com.google.common.primitives.Ints;
import com.google.common.reflect.TypeToken;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.asm.transformers.ModAccessTransformer;
import cpw.mods.fml.common.launcher.FMLInjectionAndSortingTweaker;
import cpw.mods.fml.common.launcher.FMLTweaker;
import cpw.mods.fml.common.toposort.TopologicalSort;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.DependsOn;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.Name;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

public class CoreModManager {
    private static final Attributes.Name COREMODCONTAINSFMLMOD = new Attributes.Name("FMLCorePluginContainsFMLMod");
    private static final Attributes.Name MODTYPE = new Attributes.Name("ModType");
    private static final Attributes.Name MODSIDE = new Attributes.Name("ModSide");
    private static String[] rootPlugins = { "cpw.mods.fml.relauncher.FMLCorePlugin", "net.minecraftforge.classloading.FMLForgePlugin" };
    private static List<FMLPluginWrapper> loadPlugins;
    private static boolean deobfuscatedEnvironment;
    private static FMLTweaker tweaker;
    private static File mcDir;
    private static List<String> accessTransformers = Lists.newArrayList();

    private static class FMLPluginWrapper implements ITweaker {
        public final String name;
        public final IFMLLoadingPlugin coreModInstance;
        public final List<String> predepends;
        public final int sortIndex;
        
        // TODO: do any coremods rely on their location? we could possibly use the packed JAR location
        // (FMLTweaker.getJarLocation())
        private final File location = new File(FMLTweaker.getJarLocation());

        public FMLPluginWrapper(String name, IFMLLoadingPlugin coreModInstance, int sortIndex, String... predepends)
        {
            super();
            this.name = name;
            this.coreModInstance = coreModInstance;
            this.sortIndex = sortIndex;
            this.predepends = Lists.newArrayList(predepends);
        }

        @Override
        public String toString()
        {
            return String.format("%s {%s}", this.name, this.predepends);
        }

        @Override
        public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile)
        {
            // NO OP
        }
        
        private static String replaceTransformerForCompat(String originalClassName) {
        	try {
        		return (String)Class.forName("net.mcforkage.compat.MCFCompat").getMethod("replaceTransformer", String.class).invoke(null, originalClassName);
        	} catch(ClassNotFoundException e) {
        		return originalClassName;
        	} catch(Exception e) {
        		throw new RuntimeException(e);
        	}
        }

        @Override
        public void injectIntoClassLoader(LaunchClassLoader classLoader)
        {
            FMLRelaunchLog.fine("Injecting coremod %s {%s} class transformers", name, coreModInstance.getClass().getName());
            if (coreModInstance.getASMTransformerClass() != null) for (String transformer : coreModInstance.getASMTransformerClass())
            {
                FMLRelaunchLog.finer("Registering transformer %s", transformer);
                classLoader.registerTransformer(replaceTransformerForCompat(transformer));
            }
            FMLRelaunchLog.fine("Injection complete");

            FMLRelaunchLog.fine("Running coremod plugin for %s {%s}", name, coreModInstance.getClass().getName());
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("mcLocation", mcDir);
            data.put("coremodList", loadPlugins);
            data.put("runtimeDeobfuscationEnabled", true); // XXX BACKCOMPAT: some mods assume we're in MCP when this is false
            FMLRelaunchLog.fine("Running coremod plugin %s", name);
            data.put("coremodLocation", location);
            coreModInstance.injectData(data);
            String setupClass = coreModInstance.getSetupClass();
            if (setupClass != null)
            {
                try
                {
                    IFMLCallHook call = (IFMLCallHook) Class.forName(setupClass, true, classLoader).newInstance();
                    Map<String, Object> callData = new HashMap<String, Object>();
                    callData.put("runtimeDeobfuscationEnabled", true); // XXX BACKCOMPAT: some mods assume we're in MCP when this is false
                    callData.put("mcLocation", mcDir);
                    callData.put("classLoader", classLoader);
                    callData.put("coremodLocation", location);
                    callData.put("deobfuscationFileName", FMLInjectionData.debfuscationDataName());
                    call.injectData(callData);
                    call.call();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
            FMLRelaunchLog.fine("Coremod plugin class %s run successfully", coreModInstance.getClass().getSimpleName());

            String modContainer = coreModInstance.getModContainerClass();
            if (modContainer != null)
            {
                FMLInjectionData.containers.add(modContainer);
            }
        }

        @Override
        public String getLaunchTarget()
        {
            return "";
        }

        @Override
        public String[] getLaunchArguments()
        {
            return new String[0];
        }

    }

    public static void handleLaunch(File mcDir, LaunchClassLoader classLoader, FMLTweaker tweaker)
    {
        CoreModManager.mcDir = mcDir;
        CoreModManager.tweaker = tweaker;
        try
        {
            // Are we in a 'decompiled' environment?
            byte[] bs = classLoader.getClassBytes("net.minecraft.world.World");
            if (bs != null)
            {
                FMLRelaunchLog.info("Managed to load a deobfuscated Minecraft name- we are in a deobfuscated environment. Skipping runtime deobfuscation");
                deobfuscatedEnvironment = true;
            }
        }
        catch (IOException e1)
        {
        }

        if (!deobfuscatedEnvironment)
        {
            FMLRelaunchLog.fine("Enabling runtime deobfuscation");
        }

        tweaker.injectCascadingTweak("cpw.mods.fml.common.launcher.FMLInjectionAndSortingTweaker");

        loadPlugins = new ArrayList<FMLPluginWrapper>();
        for (String rootPluginName : rootPlugins)
        {
            loadCoreMod(classLoader, rootPluginName);
        }

        if (loadPlugins.isEmpty())
        {
            throw new RuntimeException("A fatal error has occured - no valid fml load plugin was found - this is a completely corrupt FML installation.");
        }

        FMLRelaunchLog.fine("All fundamental core mods are successfully located");
        // Now that we have the root plugins loaded - lets see what else might
        // be around
        String commandLineCoremods = System.getProperty("fml.coreMods.load", "");
        for (String coreModClassName : commandLineCoremods.split(","))
        {
            if (coreModClassName.isEmpty())
            {
                continue;
            }
            FMLRelaunchLog.info("Found a command line coremod : %s", coreModClassName);
            loadCoreMod(classLoader, coreModClassName);
        }
        discoverCoreMods(mcDir, classLoader);

    }

    private static void discoverCoreMods(File mcDir, LaunchClassLoader classLoader)
    {
        ModListHelper.parseModList(mcDir);
        FMLRelaunchLog.fine("Discovering coremods");
        
        // TODO: handle cascading tweakers
        if(PackerDataUtils.read("mcforkage-cascading-tweakers.json", new TypeToken<List<Map<String,String>>>() {}).size() > 0)
        	throw new RuntimeException("Can't handle cascading tweakers");
        //handleCascadingTweak(coreMod, jar, cascadedTweaker, classLoader, sortOrder);
        
        List<String> coremodClasses = PackerDataUtils.read("mcforkage-coremods.json", new TypeToken<List<String>>(){});
        
        for(String coremodClass : coremodClasses) {
        	loadCoreMod(classLoader, coremodClass);
        }
    }

    private static Method ADDURL;

    private static void handleCascadingTweak(File coreMod, JarFile jar, String cascadedTweaker, LaunchClassLoader classLoader, Integer sortingOrder)
    {
        try
        {
            // Have to manually stuff the tweaker into the parent classloader
            if (ADDURL == null)
            {
                ADDURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                ADDURL.setAccessible(true);
            }
            ADDURL.invoke(classLoader.getClass().getClassLoader(), coreMod.toURI().toURL());
            classLoader.addURL(coreMod.toURI().toURL());
            CoreModManager.tweaker.injectCascadingTweak(cascadedTweaker);
            tweakSorting.put(cascadedTweaker,sortingOrder);
        }
        catch (Exception e)
        {
            FMLRelaunchLog.log(Level.INFO, e, "There was a problem trying to load the mod dir tweaker %s", coreMod.getAbsolutePath());
        }
    }

    private static FMLPluginWrapper loadCoreMod(LaunchClassLoader classLoader, String coreModClass)
    {
        String coreModName = coreModClass.substring(coreModClass.lastIndexOf('.') + 1);
        try
        {
            FMLRelaunchLog.fine("Instantiating coremod class %s", coreModName);
            classLoader.addTransformerExclusion(coreModClass);
            Class<?> coreModClazz = Class.forName(coreModClass, true, classLoader);
            Name coreModNameAnn = coreModClazz.getAnnotation(IFMLLoadingPlugin.Name.class);
            if (coreModNameAnn != null && !Strings.isNullOrEmpty(coreModNameAnn.value()))
            {
                coreModName = coreModNameAnn.value();
                FMLRelaunchLog.finer("coremod named %s is loading", coreModName);
            }
            MCVersion requiredMCVersion = coreModClazz.getAnnotation(IFMLLoadingPlugin.MCVersion.class);
            if (!Arrays.asList(rootPlugins).contains(coreModClass) && (requiredMCVersion == null || Strings.isNullOrEmpty(requiredMCVersion.value())))
            {
                FMLRelaunchLog.log(Level.WARN, "The coremod %s does not have a MCVersion annotation, it may cause issues with this version of Minecraft",
                        coreModClass);
            }
            else if (requiredMCVersion != null && !FMLInjectionData.mccversion.equals(requiredMCVersion.value()))
            {
                FMLRelaunchLog.log(Level.ERROR, "The coremod %s is requesting minecraft version %s and minecraft is %s. It will be ignored.", coreModClass,
                        requiredMCVersion.value(), FMLInjectionData.mccversion);
                return null;
            }
            else if (requiredMCVersion != null)
            {
                FMLRelaunchLog.log(Level.DEBUG, "The coremod %s requested minecraft version %s and minecraft is %s. It will be loaded.", coreModClass,
                        requiredMCVersion.value(), FMLInjectionData.mccversion);
            }
            TransformerExclusions trExclusions = coreModClazz.getAnnotation(IFMLLoadingPlugin.TransformerExclusions.class);
            if (trExclusions != null)
            {
                for (String st : trExclusions.value())
                {
                    classLoader.addTransformerExclusion(st);
                }
            }
            DependsOn deplist = coreModClazz.getAnnotation(IFMLLoadingPlugin.DependsOn.class);
            String[] dependencies = new String[0];
            if (deplist != null)
            {
                dependencies = deplist.value();
            }
            SortingIndex index = coreModClazz.getAnnotation(IFMLLoadingPlugin.SortingIndex.class);
            int sortIndex = index != null ? index.value() : 0;

            IFMLLoadingPlugin plugin = (IFMLLoadingPlugin) coreModClazz.newInstance();
            String accessTransformerClass = plugin.getAccessTransformerClass();
            if (accessTransformerClass != null)
            {
                FMLRelaunchLog.log(Level.DEBUG, "Added access transformer class %s to enqueued access transformers", accessTransformerClass);
                accessTransformers.add(accessTransformerClass);
            }
            FMLPluginWrapper wrap = new FMLPluginWrapper(coreModName, plugin, sortIndex, dependencies);
            loadPlugins.add(wrap);
            FMLRelaunchLog.fine("Enqueued coremod %s", coreModName);
            return wrap;
        }
        catch (ClassNotFoundException cnfe)
        {
            if (!Lists.newArrayList(rootPlugins).contains(coreModClass))
                FMLRelaunchLog.log(Level.ERROR, cnfe, "Coremod %s: Unable to class load the plugin %s", coreModName, coreModClass);
            else
                FMLRelaunchLog.fine("Skipping root plugin %s", coreModClass);
        }
        catch (ClassCastException cce)
        {
            FMLRelaunchLog.log(Level.ERROR, cce, "Coremod %s: The plugin %s is not an implementor of IFMLLoadingPlugin", coreModName, coreModClass);
        }
        catch (InstantiationException ie)
        {
            FMLRelaunchLog.log(Level.ERROR, ie, "Coremod %s: The plugin class %s was not instantiable", coreModName, coreModClass);
        }
        catch (IllegalAccessException iae)
        {
            FMLRelaunchLog.log(Level.ERROR, iae, "Coremod %s: The plugin class %s was not accessible", coreModName, coreModClass);
        }
        return null;
    }

    @SuppressWarnings("unused")
    private static void sortCoreMods()
    {
        TopologicalSort.DirectedGraph<FMLPluginWrapper> sortGraph = new TopologicalSort.DirectedGraph<FMLPluginWrapper>();
        Map<String, FMLPluginWrapper> pluginMap = Maps.newHashMap();
        for (FMLPluginWrapper plug : loadPlugins)
        {
            sortGraph.addNode(plug);
            pluginMap.put(plug.name, plug);
        }

        for (FMLPluginWrapper plug : loadPlugins)
        {
            for (String dep : plug.predepends)
            {
                if (!pluginMap.containsKey(dep))
                {
                    FMLRelaunchLog.log(Level.ERROR, "Missing coremod dependency - the coremod %s depends on coremod %s which isn't present.", plug.name, dep);
                    throw new RuntimeException();
                }
                sortGraph.addEdge(plug, pluginMap.get(dep));
            }
        }
        try
        {
            loadPlugins = TopologicalSort.topologicalSort(sortGraph);
            FMLRelaunchLog.fine("Sorted coremod list %s", loadPlugins);
        }
        catch (Exception e)
        {
            FMLLog.log(Level.ERROR, e, "There was a problem performing the coremod sort");
            throw Throwables.propagate(e);
        }
    }

    public static void injectTransformers(LaunchClassLoader classLoader)
    {

        Launch.blackboard.put("fml.deobfuscatedEnvironment", deobfuscatedEnvironment);
        tweaker.injectCascadingTweak("cpw.mods.fml.common.launcher.FMLDeobfTweaker");
        tweakSorting.put("cpw.mods.fml.common.launcher.FMLDeobfTweaker", Integer.valueOf(1000));
    }

    public static void injectCoreModTweaks(FMLInjectionAndSortingTweaker fmlInjectionAndSortingTweaker)
    {
        @SuppressWarnings("unchecked")
        List<ITweaker> tweakers = (List<ITweaker>) Launch.blackboard.get("Tweaks");
        // Add the sorting tweaker first- it'll appear twice in the list
        tweakers.add(0, fmlInjectionAndSortingTweaker);
        for (FMLPluginWrapper wrapper : loadPlugins)
        {
            tweakers.add(wrapper);
        }
    }

    private static Map<String,Integer> tweakSorting = Maps.newHashMap();

    public static void sortTweakList()
    {
        @SuppressWarnings("unchecked")
        List<ITweaker> tweakers = (List<ITweaker>) Launch.blackboard.get("Tweaks");
        // Basically a copy of Collections.sort pre 8u20, optimized as we know we're an array list.
        // Thanks unhelpful fixer of http://bugs.java.com/view_bug.do?bug_id=8032636
        ITweaker[] toSort = tweakers.toArray(new ITweaker[tweakers.size()]);
        Arrays.sort(toSort, new Comparator<ITweaker>() {
            @Override
            public int compare(ITweaker o1, ITweaker o2)
            {
                Integer first = null;
                Integer second = null;
                if (o1 instanceof FMLInjectionAndSortingTweaker)
                {
                    first = Integer.MIN_VALUE;
                }
                if (o2 instanceof FMLInjectionAndSortingTweaker)
                {
                    second = Integer.MIN_VALUE;
                }

                if (o1 instanceof FMLPluginWrapper)
                {
                    first = ((FMLPluginWrapper) o1).sortIndex;
                }
                else if (first == null)
                {
                    first = tweakSorting.get(o1.getClass().getName());
                }
                if (o2 instanceof FMLPluginWrapper)
                {
                    second = ((FMLPluginWrapper) o2).sortIndex;
                }
                else if (second == null)
                {
                    second = tweakSorting.get(o2.getClass().getName());
                }
                if (first == null)
                {
                    first = 0;
                }
                if (second == null)
                {
                    second = 0;
                }

                return Ints.saturatedCast((long)first - (long)second);
            }
        });
        // Basically a copy of Collections.sort, optimized as we know we're an array list.
        // Thanks unhelpful fixer of http://bugs.java.com/view_bug.do?bug_id=8032636
        for (int j = 0; j < toSort.length; j++) {
            tweakers.set(j, toSort[j]);
        }
    }

    public static List<String> getAccessTransformers()
    {
        return accessTransformers;
    }
}
