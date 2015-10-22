/**
 * This package contains the Minecraft Forkage instance setup program.
 * You're probably wondering what this is.
 * 
 * In Minecraft Forkage, we want to move most of the work that occurs during startup
 * to a one-time "instance setup" phase, and cache the results.
 * 
 * Currently, this includes:
 * 
 * <ul>
 * <li> Searching for mods.
 * <li> Bytecode (ASM) transformations.
 * </ul>
 * 
 * as these are the least invasive changes.
 * 
 * Potentially, in the future, instance setup might also involve:
 * <ul>
 * <li> Config parsing.
 * <li> Item registration.
 * <li> Ore dictionary registration.
 * <li> Fluid registration (deciding which mods' fluids to use, and setting icons)
 * <li> Dynamic recipe generation (e.g. in GregTech).
 * <li> Texture stitching.
 * </ul>
 * and mods will of course be able to add their own steps.
 * 
 * 
 * 
 * 
 * 
 * If entire mods were brought onto the classpath for instance setup, it would be easy to run into class-loading
 * issues (see FML's ASM transformer problems).
 * For this reason, mods are permitted (but not required) to put their instance-setup-related classes into a separate
 * JAR file. To avoid inconveniencing users, this separate JAR can (optionally) then be packaged inside your main JAR.
 * 
 * 
 * 
 * 
 * 
 * <h1>Class location</h1>
 * JARs containing classes to be used in the instance setup process must be explicitly identified.
 * 
 * All JARs, ZIPs and directories in the mods folder are searched.
 * 
 * If any of them contain a "MCF-InstanceSetupClasspath: true" main manifest attribute, they are added to the
 * instance setup classpath.
 * 
 * If any of them contain files with "MCF-InstanceSetupClasspath: true" manifest attributes, those files are searched
 * recursively. This feature is not fully implemented - if you want to use it, report an issue to get it finished.
 * 
 * 
 * 
 * 
 * 
 * <h1>Initiation</h1>
 * Currently, the instance setup process runs on any startup, if the launcher is set to run the launcher stub
 * instead of running Minecraft directly.
 * 
 * As this is an experimental feature, the installer does not configure this. If you want to test instance setup,
 * edit the launcher version JSON file so the main class is
 * <tt>net.minecraftforkage.instsetup.launcher_stub.EntryPointFromLauncher</tt>.
 * 
 * 
 * 
 * 
 * 
 * 
 */
package net.minecraftforkage.instsetup;