import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import bytecode.BaseStreamingZipProcessor;

public class RemapSources {
	
	private static Map<String, String> fieldNames = new HashMap<>();
	private static Map<String, String> fieldDocs = new HashMap<>();
	private static Map<String, String> methodNames = new HashMap<>();
	private static Map<String, String> methodDocs = new HashMap<>();
	private static Map<String, String> paramNames = new HashMap<>();
	
	private static boolean noJavadocs;
	private static boolean doesJavadocs = false; // currently unsettable; TODO what's the relationship between this and noJavadocs?
	
	public static void main(String[] args) {
		if(args.length != 4) {
			System.err.println("Usage: java RemapSources methods.csv fields.csv params.csv true/false < infile.jar > outfile.jar");
			System.exit(1);
		}
		
		try {
			
			try (BufferedReader reader = new BufferedReader(new FileReader(args[0]))) {
				String line;
				while((line = reader.readLine()) != null) {
					String[] parts = line.split(",",-1);
					methodNames.put(parts[0], parts[1]);
					methodDocs.put(parts[0], parts[3]);
				}
			}
			
			try (BufferedReader reader = new BufferedReader(new FileReader(args[1]))) {
				String line;
				while((line = reader.readLine()) != null) {
					String[] parts = line.split(",",-1);
					fieldNames.put(parts[0], parts[1]);
					fieldDocs.put(parts[0], parts[3]);
				}
			}
			
			try (BufferedReader reader = new BufferedReader(new FileReader(args[2]))) {
				String line;
				while((line = reader.readLine()) != null) {
					String[] parts = line.split(",",-1);
					paramNames.put(parts[0], parts[1]);
				}
			}
			
			noJavadocs = Boolean.parseBoolean(args[3]);
			
			try (ZipInputStream zipIn = new ZipInputStream(System.in)) {
				try (ZipOutputStream zipOut = new ZipOutputStream(System.out)) {
					ZipEntry ze;
					while((ze = zipIn.getNextEntry()) != null) {
						
						zipOut.putNextEntry(new ZipEntry(ze.getName()));
						
						if(!ze.getName().endsWith(".java")) {
							BaseStreamingZipProcessor.copyResource(zipIn, zipOut);
						
						} else {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							BaseStreamingZipProcessor.copyResource(zipIn, baos);

							byte[] bytes = baos.toByteArray();
							bytes = process(bytes, ze.getName());
							zipOut.write(bytes);
						}
						
						zipIn.closeEntry();
						zipOut.closeEntry();
					}
				}
			}
			
		} catch(Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
		
		System.exit(0);
	}
	
	private static final Pattern                   SRG_FINDER = Pattern.compile("(func_[0-9]+_[a-zA-Z_]+|field_[0-9]+_[a-zA-Z_]+|p_[\\w]+_\\d+_)([^\\w\\$])");
    private static final Pattern                   METHOD     = Pattern.compile("^((?: {4})+|\\t+)(?:[\\w$.\\[\\]]+ )+(func_[0-9]+_[a-zA-Z_]+)\\(");
    private static final Pattern                   FIELD      = Pattern.compile("^((?: {4})+|\\t+)(?:[\\w$.\\[\\]]+ )+(field_[0-9]+_[a-zA-Z_]+) *(?:=|;)");

    private static byte[] process(byte[] bytes, String filename) {
		Matcher matcher;
        ArrayList<String> newLines = new ArrayList<String>();
        for (String line : new String(bytes, StandardCharsets.UTF_8).split("\n"))
        {
        	if (noJavadocs) // noajavadocs? dont bothe with the rest of this crap...
            {
                newLines.add(replaceInLine(line));
                continue;
            }
            
        	matcher = METHOD.matcher(line);
            if (matcher.find())
            {
                String name = matcher.group(2);

                if (methodNames.containsKey(name))
                {
                    String javadoc = methodDocs.get(name);
                    if(javadoc != null && !javadoc.isEmpty())
                    {
                        if (doesJavadocs)
                            javadoc = buildJavadoc(matcher.group(1), javadoc, true);
                        else
                            javadoc = matcher.group(1) + "// JAVADOC METHOD $$ " + name;
                        insetAboveAnnotations(newLines, javadoc);
                    }
                }
            }
            else if (line.trim().startsWith("// JAVADOC "))
            {
                Matcher match = SRG_FINDER.matcher(line);
                if (match.find())
                {
                    String indent = line.substring(0, line.indexOf("// JAVADOC"));
                    String name = match.group();
                    if (name.startsWith("func_"))
                    {
                    	String mtdDoc = methodDocs.get(name);
                        if(mtdDoc != null && !mtdDoc.isEmpty())
                        {
                            line = buildJavadoc(indent, mtdDoc, true);
                        }
                    }
                    else if (name.startsWith("field_"))
                    {
                        String fldDoc = fieldDocs.get(name);
                        if(fldDoc != null && !fldDoc.isEmpty())
                        {
                            line = buildJavadoc(indent, fldDoc, true);
                        }
                    }

                    if (line.endsWith("\n"))
                    {
                        line = line.substring(0, line.length() - "\n".length());
                    }
                }
            }
            else
            {
                matcher = FIELD.matcher(line);
                if (matcher.find())
                {
                    String name = matcher.group(2);
                    if (fieldNames.containsKey(name))
                    {
                        String javadoc = fieldDocs.get(name);
                        if(javadoc != null && !javadoc.isEmpty())
                        {
                            if (doesJavadocs)
                                javadoc = buildJavadoc(matcher.group(1), javadoc, false);
                            else
                                javadoc = matcher.group(1) + "// JAVADOC FIELD $$ " + name;
                            insetAboveAnnotations(newLines, javadoc);
                        }
                    }
                }
            }
            newLines.add(replaceInLine(line));
        }

        StringBuilder sb = new StringBuilder();
        for(String line : newLines) {
        	if(sb.length() != 0) sb.append("\n");
        	sb.append(line);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
	}
    
    private static void insetAboveAnnotations(List<String> list, String line)
    {
        int back = 0;
        while (list.get(list.size() - 1 - back).trim().startsWith("@"))
        {
            back++;
        }
        list.add(list.size() - back, line);
    }
    
    private static String replaceInLine(String line)
    {
        // FAR all methods
        StringBuffer buf = new StringBuffer();
        Matcher matcher = SRG_FINDER.matcher(line);
        while (matcher.find())
        {
            String find = matcher.group(1);
            
            if (find.startsWith("p_"))
                find = paramNames.get(find);
            else if (find.startsWith("func_"))
                find = methodNames.get(find);
            else if (find.startsWith("field_"))
                find = fieldNames.get(find);
            
            if (find == null)
                find = matcher.group(1);
            
            matcher.appendReplacement(buf, find);
            buf.append(matcher.group(2));
        }
        matcher.appendTail(buf);
        return buf.toString();
    }
    
    private static String buildJavadoc(String indent, String javadoc, boolean isMethod)
    {
        StringBuilder builder = new StringBuilder();

        if (javadoc.length() >= 70 || isMethod)
        {
            List<String> list = wrapText(javadoc, 120 - (indent.length() + 3));

            builder.append(indent);
            builder.append("/**\n");

            for (String line : list)
            {
                builder.append(indent);
                builder.append(" * ");
                builder.append(line);
                builder.append('\n');
            }

            builder.append(indent);
            builder.append(" */");
            //builder.append(Constants.NEWLINE);

        }
        // one line
        else
        {
            builder.append(indent);
            builder.append("/** ");
            builder.append(javadoc);
            builder.append(" */");
            //builder.append(Constants.NEWLINE);
        }

        return builder.toString().replace(indent, indent);
    }
    
    private static List<String> wrapText(String text, int len)
    {
        // return empty array for null text
        if (text == null)
        {
            return new ArrayList<String>();
        }

        // return text if len is zero or less
        if (len <= 0)
        {
            return new ArrayList<String>(Arrays.asList(text));
        }

        // return text if less than length
        if (text.length() <= len)
        {
            return new ArrayList<String>(Arrays.asList(text));
        }

        List<String> lines = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        StringBuilder word = new StringBuilder();
        int tempNum;

        // each char in array
        for (char c : text.toCharArray())
        {
            // its a wordBreaking character.
            if (c == ' ' || c == ',' || c == '-')
            {
                // add the character to the word
                word.append(c);

                // its a space. set TempNum to 1, otherwise leave it as a wrappable char
                tempNum = Character.isWhitespace(c) ? 1 : 0;

                // subtract tempNum from the length of the word
                if ((line.length() + word.length() - tempNum) > len)
                {
                    lines.add(line.toString());
                    line.delete(0, line.length());
                }

                // new word, add it to the next line and clear the word
                line.append(word);
                word.delete(0, word.length());

            }
            // not a linebreak char
            else
            {
                // add it to the word and move on
                word.append(c);
            }
        }

        // handle any extra chars in current word
        if (word.length() > 0)
        {
            if ((line.length() + word.length()) > len)
            {
                lines.add(line.toString());
                line.delete(0, line.length());
            }
            line.append(word);
        }

        // handle extra line
        if (line.length() > 0)
        {
            lines.add(line.toString());
        }

        List<String> temp = new ArrayList<String>(lines.size());
        for (String s : lines)
        {
            temp.add(s.trim());
        }
        return temp;
    }
}
