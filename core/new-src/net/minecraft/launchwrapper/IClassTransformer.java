package net.minecraft.launchwrapper;

/**
 * Mods may register IClassTransformer instances to be able to modify classes
 * as they are loaded. When loading a class, first the class file is loaded
 * into a byte array, and then each registered transformer is invoked in turn.
 * 
 * A transformer may return a new byte array, or modify the original byte
 * array and return it, or return the original byte array unmodified. The
 * return value from each transformer is passed to the next transformer.
 * The return value from the last transformer is passed to the JVM.
 * 
 * This class must be binary-compatible with Mojang's IClassTransformer.
 * Apart from functional elements which must be the same, it has been
 * re-written from scratch.
 * 
 * @author immibis
 */
public interface IClassTransformer {
	/**
	 * Called
	 * 
	 * @param className The class name, with packages separated by dots
	 *                  (e.g. "net.minecraft.client.Minecraft").
	 * @param className2 Identical to className.
	 * 
	 * @param classBytes The class file data returned from the preceding
	 *                   transformer (or loaded from disk).
	 * 
	 * @return The class file data to pass to the next transformer (or the JVM)
	 */
	public byte[] transform(String className, String className2, byte[] classBytes);
}
