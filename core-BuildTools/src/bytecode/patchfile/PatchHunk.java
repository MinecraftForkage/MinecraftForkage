package bytecode.patchfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatchHunk {
	public String path;
	
	public int oldStart, oldCount, newStart, newCount;
	
	public List<String> oldLines = new ArrayList<>();
	public List<String> newLines = new ArrayList<>();
	
	public boolean canApply(List<String> lines, int start) {
		if(start + oldCount > lines.size() + 1)
			return false;
		if(start < 1)
			return false;
		
		for(int k = 0; k < oldCount; k++) {
			String actualLine = lines.get(start-1 + k);
			String requiredLine = oldLines.get(k);
			if(!actualLine.trim().equals(requiredLine.trim())) {
				/*if(start == newStart) {
					System.err.println();
					for(int i = 0; i <= 5; i++)
						System.err.println("CONTEXT: "+(i+1)+": "+lines.get(i));
					for(int i = -5; i <= 5; i++)
						if(start+k+i-1 < lines.size())
							System.err.println("CONTEXT: "+(start+i+k)+": "+lines.get(start+k+i-1));
					System.err.println("XXX: "+(start+k)+": "+actualLine);
					System.err.println("XXX: "+(start+k)+": "+oldLines.get(k));
				}*/
				//throw new RuntimeException("patch failed, old line "+(start+k)+" was '"+actualLine+"', need '"+oldLines.get(k)+"'");
				return false;
			}
		}
		return true;
	}
	
	public int findStart(List<String> lines) {
		if(canApply(lines, newStart))
			return newStart;
		
		for(int k = 0; k < lines.size(); k++) {
			if(canApply(lines, newStart+k))
				return newStart+k;
			if(canApply(lines, newStart-k))
				return newStart-k;
		}
		
		List<String> actual = lines.subList(newStart, newStart+oldCount);
		List<String> expected = oldLines;
		System.err.println();
		System.err.println("== EXPECTED LINES ==");
		for(String s : expected)
			System.err.println(s);
		System.err.println();
		System.err.println("== ACTUAL LINES ==");
		for(String s : actual)
			System.err.println(s);
		System.err.println();
		
		throw new RuntimeException("can't find place to apply patch");
	}
	
	public void apply(List<String> lines) {
		// check context
		int at = findStart(lines);
		if(at != newStart + (newCount == 0 ? 1 : 0))
			System.err.println("Applying hunk -"+oldStart+","+oldCount+" at line "+at+" instead of "+newStart+" in "+path);
		
		at--;
		
		//for(int k = 0; k < oldCount; k++)
		//	lines.remove(at);
		//for(int k = 0; k < newCount; k++)
		//	lines.add(at+k, newLines.get(k));
		
		if(oldCount != 0)
			lines.subList(at, at+oldCount).clear();
		if(newLines.size() != 0)
			lines.addAll(at, newLines);
	}

	public void applyStreaming(StreamingPatchContext ctx) throws IOException {
		// can't search around when streaming - must be an exact match
		ctx.passThroughUntil(oldStart + (oldCount == 0 ? 1 : 0), newStart + (newCount == 0 ? 1 : 0));
		
		for(int k = 0; k < oldCount; k++) {
			String actual = ctx.readLine();
			String expected = oldLines.get(k);
			if(!actual.equals(expected))
				throw new IOException("Invalid patch? Expected line '"+expected+"', found '"+actual+"' on line "+(oldStart + k));
		}
		
		for(String s : newLines)
			ctx.writeLine(s);
	}
}
