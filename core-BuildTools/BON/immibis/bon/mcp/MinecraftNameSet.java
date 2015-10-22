package immibis.bon.mcp;

import immibis.bon.NameSet;

/**
 * E.g. "1.5.1 obfuscated", "1.5.1 SRG", "1.5.1 MCP" are NameSets.
 */
public class MinecraftNameSet extends NameSet {
	public static enum Type {
		OBF,
		SRG,
		MCP
	}
	
	public static enum Side {
		UNIVERSAL,
		CLIENT,
		SERVER
	}

	
	public final Type type;
	public final String mcVersion;
	public final Side side;
	
	public MinecraftNameSet(Type type, Side side, String mcVersion) {
		this.type = type;
		this.side = side;
		this.mcVersion = mcVersion;
	}
	
	@Override
	public boolean equals(Object obj) {
		try {
			MinecraftNameSet ns = (MinecraftNameSet)obj;
			return ns.type == type && ns.side == side && ns.mcVersion.equals(mcVersion);
			
		} catch(ClassCastException e) {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return (side.ordinal() << 8) + type.ordinal() + mcVersion.hashCode();
	}
	
	@Override
	public String toString() {
		return mcVersion+" "+type+" "+side;
	}
}
