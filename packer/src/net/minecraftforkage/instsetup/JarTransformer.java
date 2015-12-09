package net.minecraftforkage.instsetup;

import net.minecraftforkage.instsetup.depsort.DependencySortedObject;

/**
 * Allows mods to transform the baked JAR before it loads.
 * 
 * JarTransformer classes are located through {@link java.util.ServiceLoader}.
 * JarTransformer classes will not be located in mods that do not identify themselves
 * as containing instance setup components. See the documentation for this package for more details.
 */
public abstract class JarTransformer implements DependencySortedObject {
	
	@Override
	public String getDependencies() {
		return "";
	}
	
	@Override
	public abstract String getID();
	
	/**
	 * Main entry point for a JAR transformer.
	 */
	public abstract void transform(AbstractZipFile zipFile, PackerContext context) throws Exception;
	
	
	/**
	 * Returns the stage this transformer runs in.
	 * To use multiple stages, use multiple transformers.
	 * 
	 * Dependencies are processed separately within each stage.
	 * This means that transformers in different stages can have the same ID, and
	 * transformers in different stages can't depend on each other. 
	 */
	public Stage getStage() {return Stage.MAIN_STAGE;}
	
	public static final class Stage {
		private Stage() {}
		
		/**
		 * Transformers that only generate new classes can run here.
		 * 
		 * This runs before other stages, so that transformers in
		 * other stages get a chance to transform the generated classes.
		 */
		public static final Stage CLASS_GENERATION_STAGE = new Stage();
		
		/**
		 * Transformers that add mods should ideally run in this stage, so that other
		 * transformers can use mod information.
		 */
		public static final Stage MOD_IDENTIFICATION_STAGE = new Stage();
		
		/**
		 * Most transformers should run in MAIN_STAGE.
		 */
		public static final Stage MAIN_STAGE = new Stage();
		
		/**
		 * Transformers in CLASS_INFO_EXTRACTION_STAGE should build indexes
		 * based on class files, but not modify any class files.
		 */
		public static final Stage CLASS_INFO_EXTRACTION_STAGE = new Stage();
	}
}
