package net.minecraftforge.gradle.extrastuff;


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import immibis.bon.com.immibis.json.JsonReader;

public class GLConstantFixer
{
    private static final String[] PACKAGES = {
            "GL11",
            "GL12",
            "GL13",
            "GL14",
            "GL15",
            "GL20",
            "GL21",
            "ARBMultitexture",
            "ARBOcclusionQuery",
            "ARBVertexBufferObject",
            "ARBShaderObjects"
    };
    private static final String join(String[] a, String delim) {
    	StringBuilder sb = new StringBuilder();
    	for(int k = 0; k < a.length; k++) {
    		if(k > 0) sb.append(delim);
    		sb.append(a[k]);
    	}
    	return sb.toString();
    }

    private final Object json;
    public static final Pattern CALL_REGEX = Pattern.compile("(" + join(PACKAGES,"|") + ")\\.([\\w]+)\\(.+\\)");
    public static final Pattern CONSTANT_REGEX = Pattern.compile("(?<![-.\\w])\\d+(?![.\\w])");
    private static final String ADD_AFTER = "org.lwjgl.opengl.GL11";
    private static final String CHECK = "org.lwjgl.opengl.";
    private static final String IMPORT_CHECK = "import " + CHECK;
    private static final String IMPORT_REPLACE = "import " + ADD_AFTER + ";";

    public GLConstantFixer()
    {
    	try {
	    	try (Reader r = new InputStreamReader(GLConstantFixer.class.getResourceAsStream("gl.json"), StandardCharsets.UTF_8)) {
	    		json = JsonReader.readJSON(r);
	    	}
    	} catch(IOException e) {
    		throw new RuntimeException(e);
    	}
    }

    public String fixOGL(String text)
    {
        // if it never uses openGL, ignore it.
        if (!text.contains(IMPORT_CHECK))
        {
            return text;
        }

        text = annotateConstants(text);

        for (String pack : PACKAGES)
        {
            if (text.contains(pack + "."))
            {
                text = updateImports(text, CHECK + pack);
            }
        }

        return text;
    }

    private String annotateConstants(String text)
    {
        Matcher rootMatch = CALL_REGEX.matcher(text);
        String pack, method, fullCall;
        StringBuffer out = new StringBuffer(text.length());
        StringBuffer innerOut;

        // search with regex.
        while (rootMatch.find())
        {
            // helper variables
            fullCall = rootMatch.group();
            pack = rootMatch.group(1);
            method = rootMatch.group(2);

            Matcher constantMatcher = CONSTANT_REGEX.matcher(fullCall);
            innerOut = new StringBuffer(fullCall.length());
            
            Map listNode;

            // search for hardcoded numbers
            while (constantMatcher.find())
            {
                // helper variables and return variable.
                String constant = constantMatcher.group();
                String answer = null;

                // iterrate over the JSON
                for (Object group : (List)json)
                {
                    // the list part object
                    listNode = (Map)((List)group).get(0);

                    // ensure that the package and method are defined
                    if (listNode.containsKey(pack) && jsonArrayContains((List)listNode.get(pack), method))
                    {
                        // now the map part object
                        listNode = (Map)((List)group).get(1);

                        // itterrate through the map.
                        for (Map.Entry<String, Map> entry : (Set<Map.Entry<String, Map>>)listNode.entrySet())
                        {
                            // find the actual constant for the number from the regex
                            if (entry.getValue().containsKey(constant))
                            {
                                // construct the final line
                                answer = entry.getKey() + "." + entry.getValue().get(constant);
                            }
                        }
                    }

                }

                // replace the final line.
                if (answer != null)
                {
                    constantMatcher.appendReplacement(innerOut, Matcher.quoteReplacement(answer));
                }
            }
            constantMatcher.appendTail(innerOut);

            // replace the final line.
            if (fullCall != null)
            {
                rootMatch.appendReplacement(out, Matcher.quoteReplacement(innerOut.toString()));
            }
        }
        rootMatch.appendTail(out);

        return out.toString();
    }

    private boolean jsonArrayContains(List nodes, String str)
    {
        boolean hasMethod = false;
        for (Object testMethod : nodes)
        {
            hasMethod = testMethod.equals(str);
            if (hasMethod)
            {
                return hasMethod;
            }
        }

        return false;
    }

    private String updateImports(String text, String imp)
    {
        if (!text.contains("import " + imp + ";"))
        {
            text = text.replace(IMPORT_REPLACE, IMPORT_REPLACE + "\nimport " + imp + ";");
        }

        return text;
    }

}
