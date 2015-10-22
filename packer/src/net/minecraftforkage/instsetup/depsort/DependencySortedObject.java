package net.minecraftforkage.instsetup.depsort;

public interface DependencySortedObject {
	/**
	 * The name of this object, so that other objects can reference it in their dependency list.
	 * Conventionally, this is your mod ID, but this is not a hard requirement.
	 * 
	 * These must be unique.
	 */
	public String getID();
	
	/**
	 * Dependency list, in the same format originally used by ModLoader - that is, a semicolon-separated list of the following:
	 * <ul>
	 * <li><tt>requires:OBJECTNAME</tt> - crash if OBJECTNAME is not present.
	 * <li><tt>after:OBJECTNAME</tt> - load after OBJECTNAME, if OBJECTNAME is present. (Otherwise, do nothing)
	 * <li><tt>before:OBJECTNAME</tt> - load after OBJECTNAME, if OBJECTNAME is present. (Otherwise, do nothing)
	 * <li><tt>after:*</tt> - load after everything that does not specify <tt>after:*</tt>. More asterisks can be added to load even later.
	 * 			Some Minecraft Forkage built-in setup components that run "last" use <tt>after:**</tt>; any objects with two or more asterisks
	 * 			should not expect to run before them.
	 * <li><tt>before:*</tt> - load before everything that does not specify <tt>before:*</tt>. More asterisks can be added to load even earlier.
	 * 			Some Minecraft Forkage built-in setup components that run "first" use <tt>before:**</tt>; any objects with two or more asterisks
	 * 			may experience an unpredictable environment (as the system is currently not designed for this).
	 * </ul>
	 */
	public String getDependencies();
}
