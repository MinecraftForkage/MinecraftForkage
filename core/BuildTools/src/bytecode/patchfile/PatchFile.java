package bytecode.patchfile;

import installer.ProgressDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
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
		
		// one object for each patch in the file (from +++ to next +++)
		class PatchedFileInfo {
			List<String> patchLines = new LinkedList<String>(); // XXX Why LinkedList? Why are we iterating over this with .remove(0) until empty?
			int firstLineInPatchFileIndex;
		}
		
		List<PatchedFileInfo> splitLines = new ArrayList<>();
		
		int currentLineInPatchFile = 0;
		PatchedFileInfo curPatch = null;
		while(true) {
			line = in.readLine(); currentLineInPatchFile++;
			if(line == null)
				break;
			if(line.startsWith("diff "))
				continue;
			if(line.startsWith("---")) {
				curPatch = new PatchedFileInfo();
				curPatch.firstLineInPatchFileIndex = currentLineInPatchFile;
				splitLines.add(curPatch);
			}
			if(curPatch == null)
				continue;
			curPatch.patchLines.add(line);
		}
		
		PatchFile pf = new PatchFile();
		
		Pattern hunkHeaderPattern = Pattern.compile("@@ -([0-9]+)(,[0-9]+|) \\+([0-9]+)(,[0-9]+|) @@");
			
		for(PatchedFileInfo pfi : splitLines) {
			List<String> patchLines = pfi.patchLines;
			String path = patchLines.remove(0).substring(3).trim().replace("\\","/");
			if(!patchLines.get(0).startsWith("+++"))
				throw new IOException("corrupted patch file: --- not followed by +++ but by "+patchLines.get(0));
			patchLines.remove(0);
			
			if(path.contains("\t"))
				path = path.substring(0, path.indexOf('\t'));
			
			// XXX Minecraft-specific
			if(path.startsWith("../src-base/")) path = path.substring(12);
			if(path.startsWith("minecraft/")) path = path.substring(10);
			
			currentLineInPatchFile = pfi.firstLineInPatchFileIndex + 1;
			List<PatchHunk> patchHunks = new ArrayList<>();
			while(!patchLines.isEmpty()) {
				
				String header = patchLines.remove(0); currentLineInPatchFile++;
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
					throw new IOException("corrupted patch file: unparseable hunk header: "+header+" on line " + currentLineInPatchFile);
				
				while(h.oldLines.size() != h.oldCount || h.newLines.size() != h.newCount) {
					if(h.oldLines.size() > h.oldCount || h.newLines.size() > h.newCount)
						throw new IOException("corrupted patch file; line count mismatch, path is "+path);
					
					line = patchLines.remove(0); currentLineInPatchFile++;
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
		return applyPatches(bytes, path, null);
	}
	
	// TODO: streaming is even faster than this in-memory list shuffling algorithm.
	// See if we can remove this algorithm. Note that streaming requires an exactly applicable
	// patch; it can't search for context.
	public byte[] applyPatches(byte[] bytes, String path, ProgressDialog dlg) {
		if(!hunks.containsKey(path))
			return bytes;
		
		
		// Split into lines, then trim \r (if we have Windows line endings).
		// This uses one less copy of the input than the previous .replace("\r\n", "\n").split("\n") method.
		List<String> lines = Arrays.asList(new String(bytes, StandardCharsets.UTF_8).split("\n"));
		for(int k = 0; k < lines.size(); k++) {
			String line = lines.get(k);
			if(line.endsWith("\r"))
				lines.set(k, line.substring(0, line.length() - 1));
		}
		
		// Measured time for the installer bytecode patching:
		// 0.99s using applyPatchesStreaming instead
		// 3.29s with this and TreeList
		// 39.1s with this and ArrayList
		// Over 5 minutes (after which I stopped waiting) with this and LinkedList (since it does not support efficient random access)
		lines = new TreeList<>(lines);
		
		List<List<PatchHunk>> alternatives = hunks.get(path);
		if(alternatives.size() == 1) {
			if(dlg != null) dlg.initProgressBar(0, alternatives.get(0).size());
			for(PatchHunk hunk : alternatives.get(0))
				try {
					if(dlg != null) dlg.incrementProgress(1);
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

	public void applyPatchesStreaming(BufferedReader in, PrintWriter out, String path, ProgressDialog dlg) throws IOException {
		List<PatchHunk> hunks;
		{
			List<List<PatchHunk>> alternatives = this.hunks.get(path);
			if(alternatives == null || alternatives.size() == 0) throw new RuntimeException("no hunks for "+path);
			if(alternatives.size() != 1) throw new RuntimeException("can't try alternative patches when streaming (for "+path+")");
			hunks = alternatives.get(0);
		}
		
		if(dlg != null)
			dlg.initProgressBar(0, hunks.size());
		
		StreamingPatchContext ctx = new StreamingPatchContext(in, out);
		
		for(PatchHunk hunk : hunks)
			try {
				if(dlg != null)
					dlg.incrementProgress(1);
				
				hunk.applyStreaming(ctx);
				
			} catch(RuntimeException e) {
				throw new RuntimeException("Failed patching hunk -"+hunk.oldStart+","+hunk.oldCount, e);
			}
		
		ctx.skipRestOfFile();
	}
}

