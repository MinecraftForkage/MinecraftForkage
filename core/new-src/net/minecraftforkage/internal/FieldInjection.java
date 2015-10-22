package net.minecraftforkage.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

@SuppressWarnings("serial")
public class FieldInjection {
	private static class Entry {
		String className;
		String fieldName;
		String type;
		Map<String, String> data = new HashMap<String, String>();
		
		Field locateField() throws ReflectiveOperationException {
			Field f = Class.forName(className).getDeclaredField(fieldName);
			f.setAccessible(true);
			return f;
		}
		
		void inject(Object value) throws ReflectiveOperationException {
			Field f = locateField();
			
			// TODO: according to FML code, Scala mods can't have static fields,
			// so we need to get an instance from somewhere?
			
			if(!Modifier.isStatic(f.getModifiers()))
				throw new RuntimeException(className+"."+fieldName+" is not static (for "+type+" injection)");
			if(value == null)
				throw new IllegalArgumentException("injecting null?");
			if(!f.getType().isAssignableFrom(value.getClass()))
				throw new RuntimeException(className+"."+fieldName+" has type "+f.getType()+", not compatible with actual value type "+value.getClass().getName());
			f.set(null, value);
		}
	}
	
	private static final List<Entry> entries;

	static {
		try {
			InputStreamReader in = new InputStreamReader(FieldInjection.class.getResourceAsStream("/mcforkage-fields-to-inject.json"), Charset.forName("UTF-8"));
			try {
				entries = Collections.unmodifiableList(new Gson().<List<Entry>>fromJson(in, new TypeToken<List<Entry>>(){}.getType()));
			} finally {
				in.close();
			}
		} catch(IOException e) {
			throw new RuntimeException("Error reading mcforkage-fields-to-inject.json", e);
		}
		
		for(Entry e : entries) {
			if(!e.type.equals("sided-proxy") && !e.type.equals("mod-instance"))
				throw new RuntimeException("Unknown field injection type: " + e.type+" on "+e.className+"."+e.fieldName+". Extra data: " + e.data);
		}
	}
	
	/**
	 * Injects all sided proxies. Called as soon as possible, and only once.
	 */
	public static void injectSidedProxies() {
		for(Entry e : entries) {
			if(e.type.equals("sided-proxy")) {
				
				String className;
				if(FMLLaunchHandler.side().isClient())
					className = e.data.get("clientSideClass");
				else
					className = e.data.get("serverSideClass");
				
				try {
					Object value = Class.forName(className).getConstructor().newInstance();
					e.inject(value);
				} catch(ReflectiveOperationException ex) {
					throw new RuntimeException("Failed to inject sided proxy, to "+e.className+"."+e.fieldName+", of type "+className, ex);
				}
			}
		}
	}

	/**
	 * Injects all mod instances and metadata objects. Called after mod objects are constructed (and only once).
	 */
	public static void injectModInstancesAndMetadata() {
		for(Entry e : entries) {
			if(e.type.equals("mod-instance")) {
				String mod = e.data.get("mod");
				if(Loader.isModLoaded(mod))
					try {
						e.inject(Loader.instance().getIndexedModList().get(mod).getMod());
					} catch(ReflectiveOperationException ex) {
						throw new RuntimeException("Failed to inject mod instance, to "+e.className+"."+e.fieldName+", of mod "+mod, ex);
					}
			}
			
			if(e.type.equals("mod-metadata")) {
				String mod = e.data.get("mod");
				if(Loader.isModLoaded(mod))
					try {
						e.inject(Loader.instance().getIndexedModList().get(mod).getMetadata());
					} catch(ReflectiveOperationException ex) {
						throw new RuntimeException("Failed to inject mod metadata, to "+e.className+"."+e.fieldName+", of mod "+mod, ex);
					}
			}
		}
	}
}
