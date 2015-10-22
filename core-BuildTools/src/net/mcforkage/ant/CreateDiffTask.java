package net.mcforkage.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

// I am more surprised than you that this algorithm works (and isn't horrendously inefficient).
public class CreateDiffTask extends Task {
	private File from, to, output;
	
	public void setFrom(File f) {from = f;}
	public void setTo(File f) {to = f;}
	public void setOutput(File f) {output = f;}
	
	public static void main(String[] args) {
		CreateDiffTask t = new CreateDiffTask();
		t.from = new File("../../build/bytecode-old.txt");
		t.to = new File("../../build/bytecode-new.txt");
		t.output = new File("../../build/bytecode.patch");
		t.execute();
	}
	
	private int countCommonFirstLines(List<String> a, List<String> b) {
		for(int k = 0; k < a.size() && k < b.size(); k++)
			if(!a.get(k).equals(b.get(k)))
				return k;
		return Math.min(a.size(), b.size());
	}
	

	private static class IntPair {
		int a, b;
		IntPair(int a, int b) {this.a = a; this.b = b;}
	}
	
	private static final int COARSE_CHECKLEN = 30;
	
	private IntPair isResyncPoint(List<String> a, List<String> b, int da, int db, int checklen) {
		if(da > a.size() - checklen || db > b.size() - checklen)
			return null;
		
		for(int k = 0; k < checklen; k++)
			if(!a.get(da+k).equals(b.get(db+k)))
				return null;
		
		return new IntPair(da, db);
	}
	
	private IntPair findNextResyncPoint(List<String> a, List<String> b, int checklen) {
		IntPair r;
		for(int da = 0; da < a.size(); da++) {
			r = isResyncPoint(a, b, da, da, checklen);
			if(r != null) return r;
			for(int db = 0; db < da; db++) {
				r = isResyncPoint(a, b, da, db, checklen);
				if(r != null) return r; 
				r = isResyncPoint(a, b, db, da, checklen);
				if(r != null) return r;
			}
		}
		return null;
	}
	
	@Override
	public void execute() throws BuildException {
		if(from == null) throw new BuildException("From file not set");
		if(to == null) throw new BuildException("To file not set");
		if(output == null) throw new BuildException("Output file not set");
		
		List<String> fromLines = readFile(from);
		List<String> toLines = readFile(to);
		
		
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8))) {
			
			out.println("--- oldfile");
			out.println("+++ newfile");
			
			int nextFromLine = 1;
			int nextToLine = 1;
			
			
			while(fromLines.size() > 0 && toLines.size() > 0) {
				int common = countCommonFirstLines(fromLines, toLines);
				
				if(common > 0) {
					nextFromLine += common;
					nextToLine += common;
					fromLines = fromLines.subList(common, fromLines.size());
					toLines = toLines.subList(common, toLines.size());
					continue;
				}
				
				IntPair rp = findNextResyncPoint(fromLines, toLines, COARSE_CHECKLEN);
				if(rp == null) {
					break;
				}
				
				List<String> deleted = fromLines.subList(0, rp.a);
				List<String> inserted = toLines.subList(0, rp.b);
				
				refineHunk(deleted, inserted, nextFromLine, nextToLine, COARSE_CHECKLEN, out);
				
				fromLines = fromLines.subList(rp.a, fromLines.size());
				toLines = toLines.subList(rp.b, toLines.size());
				nextFromLine += rp.a;
				nextToLine += rp.b;
			}
			
			if(!toLines.isEmpty() || !fromLines.isEmpty()) {
				refineHunk(fromLines, toLines, nextFromLine, nextToLine, COARSE_CHECKLEN, out);
			}
			
			
			
			
		} catch(IOException e) {
			throw new BuildException(e);
		}
	}
	
	/*private void write_hunk(change hunk, PrintWriter out, Map<Integer, String> equiv_to_line, String[] oldLines, String[] newLines) {
		out.println("@@ -"+hunk.line0+","+hunk.deleted+" +"+hunk.line1+","+hunk.inserted+" @@");
		for(int k = 0; k < hunk.deleted; k++)
			out.println("-" + oldLines[hunk.line0+k]);
		for(int k = 0; k < hunk.inserted; k++)
			out.println("+" + newLines[hunk.line1+k]);
	}*/
	
	private void printHunk(List<String> deleted, List<String> inserted, int nextFromLine, int nextToLine, PrintWriter out) {
		if(inserted.isEmpty() && deleted.isEmpty())
			return;
		out.println("@@ -"+(nextFromLine - (deleted.isEmpty() ? 1 : 0))+","+deleted.size()+" +"+(nextToLine - (inserted.isEmpty() ? 1 : 0))+","+inserted.size()+" @@");
		for(String s : deleted) out.println("-" + s);
		for(String s : inserted) out.println("+" + s);
	}
	
	private void refineHunk(List<String> fromLines, List<String> toLines, int nextFromLine, int nextToLine, int checklen, PrintWriter out) {
		if(fromLines.isEmpty() || toLines.isEmpty() || checklen == 1) {
			printHunk(fromLines, toLines, nextFromLine, nextToLine, out);
			return;
		}
		
		checklen--;
		
		while(fromLines.size() > 0 && toLines.size() > 0) {
			int common = countCommonFirstLines(fromLines, toLines);
			if(common > 0) {
				nextFromLine += common;
				nextToLine += common;
				fromLines = fromLines.subList(common, fromLines.size());
				toLines = toLines.subList(common, toLines.size());
				continue;
			}
			
			IntPair rp = findNextResyncPoint(fromLines, toLines, checklen);
			if(rp == null) {
				break;
			}
			
			List<String> deleted = fromLines.subList(0, rp.a);
			List<String> inserted = toLines.subList(0, rp.b);
			
			refineHunk(deleted, inserted, nextFromLine, nextToLine, checklen, out);
			
			nextFromLine += deleted.size();
			nextToLine += inserted.size();
			fromLines = fromLines.subList(rp.a, fromLines.size());
			toLines = toLines.subList(rp.b, toLines.size());
			
		}
		refineHunk(fromLines, toLines, nextFromLine, nextToLine, checklen, out);
	}
	
	private List<String> readFile(File f) throws BuildException {
		ArrayList<String> lines = new ArrayList<>();
		
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
			
			String line;
			while((line = in.readLine()) != null) {
				if(line.endsWith("\r")) throw new BuildException("x");
				lines.add(line);
			}
			
		} catch(IOException e) {
			throw new BuildException("Error reading "+f, e);
		}
		
		lines.trimToSize();
		
		return lines;
	}
}
