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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class CreateDiff2Task extends Task {
	private File from, to, output;
	
	public void setFrom(File f) {from = f;}
	public void setTo(File f) {to = f;}
	public void setOutput(File f) {output = f;}
	
	public static void main(String[] args) {
		CreateDiff2Task t = new CreateDiff2Task();
		t.from = new File("../../build/bytecode-old.txt");
		t.to = new File("../../build/bytecode-new.txt");
		t.output = new File("../../build/bytecode.patch2");
		t.execute();
	}
	
	private static class SearchResult {
		public int index, length;
		public SearchResult(int index, int length) {
			this.index = index;
			this.length = length;
		}
	}
	
	private static class IntCorpusIndex {
		private int[] haystack;
		private Map<Integer, int[]> indices = new HashMap<Integer, int[]>();
		private Map<Long, int[]> dual_indices = new HashMap<Long, int[]>();
		
		public IntCorpusIndex(int[] haystack) {
			this.haystack = haystack;
			
			Map<Integer, List<Integer>> indices_temp = new HashMap<>();
			Map<Long, List<Integer>> dual_indices_temp = new HashMap<>();
			for(int index = 0; index < haystack.length; index++) {
				putToMultimap(indices_temp, haystack[index], index);
				if(index > 0)
					putToMultimap(dual_indices_temp, ((long)haystack[index] << 32) | (haystack[index-1] & 0xFFFFFFFFL), index-1);
			}
			
			for(Map.Entry<Integer, List<Integer>> entry : indices_temp.entrySet()) {
				indices.put(entry.getKey(), toIntArray(entry.getValue()));
			}
			
			for(Map.Entry<Long, List<Integer>> entry : dual_indices_temp.entrySet()) {
				dual_indices.put(entry.getKey(), toIntArray(entry.getValue()));
			}
		}
		
		private static <K, V> void putToMultimap(Map<K, List<V>> map, K key, V value) {
			if(!map.containsKey(key))
				map.put(key, new ArrayList<V>());
			map.get(key).add(value);
		}
		private static int[] toIntArray(List<Integer> l) {
			int[] ints = new int[l.size()];
			for(int k = 0; k < ints.length; k++)
				ints[k] = l.get(k);
			return ints;
		}
		
		public SearchResult search(int[] needle, int needleStart) {
			int[] indicesToTry;
			
			int best_index = 0;
			int best_length = 0;
			
			indicesToTry = indices.get(needle[needleStart]);
			if(indicesToTry == null)
				return null;
			
			best_index = indicesToTry[0];
			best_length = 1;
			
			indicesToTry = dual_indices.get(((long)needle[needleStart+1] << 32) | (needle[needleStart] & 0xFFFFFFFFL));
			
			if(indicesToTry != null) {
				for(int k : indicesToTry) {
					if(k + best_length >= haystack.length)
						continue;
					if(haystack[k+best_length-1] != needle[needleStart+best_length-1])
						continue;
					int i = 0;
					for(i = 0; i < needle.length - needleStart; i++) {
						if(haystack[k+i] != needle[needleStart+i])
							break;
					}
					if(i > best_length) {
						best_length = i;
						best_index = k;
					}
				}
			}
			return new SearchResult(best_index, best_length);
		}
	}
	
	
	private static boolean arrayContains(int[] a, int b) {
		for(int c : a) if(b == c) return true;
		return false;
	}
	
	@Override
	public void execute() throws BuildException {
		if(from == null) throw new BuildException("From file not set");
		if(to == null) throw new BuildException("To file not set");
		if(output == null) throw new BuildException("Output file not set");
		
		List<String> fromLines = readFile(from);
		List<String> toLines = readFile(to);
		
		Map<String, Integer> lineToIndex = new HashMap<String, Integer>();
		Map<Integer, String> indexToLine = new HashMap<Integer, String>();
		
		int[] fromLineIndices = linesToIndices(fromLines, lineToIndex, indexToLine);
		int[] toLineIndices = linesToIndices(toLines, lineToIndex, indexToLine);
		
		IntCorpusIndex fromLineIndicesIndex = new IntCorpusIndex(fromLineIndices);
		
		toLines = null;
		fromLines = null;
		
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output), StandardCharsets.UTF_8))) {
			
			int nextToLine = 0;
			int progressCounter = 0;
			
			while(nextToLine < toLineIndices.length) {
				SearchResult result = fromLineIndicesIndex.search(toLineIndices, nextToLine);

				if(result == null) {
					while(nextToLine < toLineIndices.length && !arrayContains(fromLineIndices, toLineIndices[nextToLine])) {
						out.println("write " + indexToLine.get(toLineIndices[nextToLine]));
						nextToLine++;
					}
				} else {
					out.println("copy " + result.index + " " + result.length);
					nextToLine += result.length;
				}
				
				if((++progressCounter) % 10000 == 0)
					System.out.println(nextToLine+" / "+toLineIndices.length);
			}
			
			
			
		} catch(IOException e) {
			throw new BuildException(e);
		}
	}
	
	private int[] linesToIndices(List<String> lines, Map<String, Integer> lineToIndex, Map<Integer, String> indexToLine) {
		int[] a = new int[lines.size()];
		for(int k = 0; k < a.length; k++) {
			Integer index = lineToIndex.get(lines.get(k));
			if(index == null) {
				index = indexToLine.size();
				indexToLine.put(index, lines.get(k));
				lineToIndex.put(lines.get(k), index);
			}
			a[k] = index;
		}
		return a;
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
