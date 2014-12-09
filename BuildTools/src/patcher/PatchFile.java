package patcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatchFile {

	// path -> list of alternative lists of hunks
	public Map<String, List<List<PatchHunk>>> hunks = new HashMap<>();

	public static PatchFile load(BufferedReader in) throws IOException {
		String line;
		
		// one list for each patch in the file (from +++ to next +++)
		List<List<String>> splitLines = new ArrayList<>();
		
		List<String> curPatch = null;
		while(true) {
			line = in.readLine();
			if(line == null)
				break;
			if(line.startsWith("diff "))
				continue;
			if(line.startsWith("---")) {
				curPatch = new LinkedList<String>();
				splitLines.add(curPatch);
			}
			if(curPatch == null)
				continue;
			curPatch.add(line);
		}
		
		PatchFile pf = new PatchFile();
		
		Pattern hunkHeaderPattern = Pattern.compile("@@ -([0-9]+)(,[0-9]+|) \\+([0-9]+)(,[0-9]+|) @@");
			
		for(List<String> patchLines : splitLines) {
			String path = patchLines.remove(0).substring(3).trim().replace("\\","/");
			if(!patchLines.get(0).startsWith("+++"))
				throw new IOException("corrupted patch file: --- not followed by +++ but by "+patchLines.get(0));
			patchLines.remove(0);
			
			if(path.contains("\t"))
				path = path.substring(0, path.indexOf('\t'));
			
			// XXX Minecraft-specific
			if(path.startsWith("../src-base/")) path = path.substring(12);
			if(path.startsWith("minecraft/")) path = path.substring(10);
			
			List<PatchHunk> patchHunks = new ArrayList<>();
			while(!patchLines.isEmpty()) {
				
				String header = patchLines.remove(0);
				Matcher headerMatcher = hunkHeaderPattern.matcher(header);
				PatchHunk h = new PatchHunk();
				h.path = path;
				if(headerMatcher.matches()) {
					h.oldStart = Integer.parseInt(headerMatcher.group(1));
					if(headerMatcher.group(2).equals(""))
						h.oldCount = 1;
					else
						h.oldCount = Integer.parseInt(headerMatcher.group(2).substring(1));
					h.newStart = Integer.parseInt(headerMatcher.group(3));
					if(headerMatcher.group(4).equals(""))
						h.newCount = 1;
					else
						h.newCount = Integer.parseInt(headerMatcher.group(4).substring(1));
				} else
					throw new IOException("corrupted patch file: unparseable hunk header: "+header+" / next line: ");
				
				while(h.oldLines.size() != h.oldCount || h.newLines.size() != h.newCount) {
					if(h.oldLines.size() > h.oldCount || h.newLines.size() > h.newCount)
						throw new IOException("corrupted patch file; line count mismatch, path is "+path);
					
					line = patchLines.remove(0);
					if(!line.startsWith("+")) h.oldLines.add(line.substring(1));
					if(!line.startsWith("-")) h.newLines.add(line.substring(1));
				}
				
				patchHunks.add(h);
			}
			
			//System.err.println(path);
			
			if(!pf.hunks.containsKey(path))
				pf.hunks.put(path, new ArrayList<List<PatchHunk>>());
			pf.hunks.get(path).add(patchHunks);
		}
		
		return pf;
	}
	
	public byte[] applyPatches(byte[] bytes, String path) {
		if(!hunks.containsKey(path))
			return bytes;
		
		List<String> lines = new ArrayList<>(Arrays.asList(new String(bytes, StandardCharsets.UTF_8).replace("\r\n","\n").split("\n")));
		
		List<List<PatchHunk>> alternatives = hunks.get(path);
		if(alternatives.size() == 1) {
			for(PatchHunk hunk : alternatives.get(0))
				try {
					hunk.apply(lines);
				} catch(RuntimeException e) {
					throw new RuntimeException("Failed patching hunk -"+hunk.oldStart+","+hunk.oldCount, e);
				}
			
		} else {
			
			boolean anyOK = false;
			RuntimeException lastException = null;
			ArrayList<String> origLines = new ArrayList<>(lines);
			alternatives: for(List<PatchHunk> alternative : alternatives) {
				lines.clear();
				lines.addAll(origLines);
				for(PatchHunk hunk : alternative)
					try {
						hunk.apply(lines);
					} catch(RuntimeException e) {
						lastException = new RuntimeException("Failed patching hunk -"+hunk.oldStart+","+hunk.oldCount, e);
						continue alternatives;
					}
				anyOK = true;
				break;
			}
			
			if(!anyOK)
				throw lastException;
			
		}
		
		StringBuilder rv = new StringBuilder();
		for(String l : lines) {
			rv.append(l);
			rv.append('\n');
		}
		return rv.toString().getBytes(StandardCharsets.UTF_8);
	}
}

