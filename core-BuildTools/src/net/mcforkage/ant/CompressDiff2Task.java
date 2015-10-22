package net.mcforkage.ant;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.mcforkage.ant.compression.BitOutputStream;
import net.mcforkage.ant.compression.FrequencyTable;
import net.mcforkage.ant.compression.HuffmanNode;
import net.mcforkage.ant.compression.HuffmanTable;
import net.mcforkage.ant.compression.HuffmanTreeVisitor;
import net.mcforkage.ant.compression.HuffmanNode.Leaf;
import net.mcforkage.ant.compression.HuffmanNode.Node;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

// Hacked out in a few hours, so not well documented.
// This basically Huffman-codes the various components of the patch2 file.
//
// The indices Huffman table is itself written using Huffman-coded (!) differences between consecutive values.
// This is because the indices are large numbers, most of which only occur once, but which are close to each other.
// This saves some ridiculous amount of the output file size (about 400KiB out of 1400KiB).
//
// 108.6% file size compared to 7-Zip Ultra preset. 111.0% file size compared to 7-Zip highest settings.
// An obvious improvement would be to switch Huffman coding for arithmetic or range coding.
//
// TODO: Why we are doing this instead of just using LZMA?
public class CompressDiff2Task extends Task {
	private File infile, outfile;
	
	public void setInput(File f) {infile = f;}
	public void setOutput(File f) {outfile = f;}
	
	public static void main(String[] args) throws Exception {
		CompressDiff2Task t = new CompressDiff2Task();
		t.infile = new File("../../build/bytecode.patch2");
		t.outfile = new File("../../build/bytecode.patch2z");
		t.execute();
	}
	
	private static class InputLine {
		String literalString;
		int index, length;
		boolean isLiteral;
		
		public InputLine(String literal) {
			this.literalString = literal;
			this.isLiteral = true;
		}
		public InputLine(int index, int length) {
			this.index = index;
			this.length = length;
		}
	}
	
	@Override
	public void execute() throws BuildException {
		if(infile == null) throw new BuildException("Input file not set");
		if(outfile == null) throw new BuildException("Output file not set");
		
		FrequencyTable<String> literalFreq = new FrequencyTable<>();
		FrequencyTable<Integer> indexFreq = new FrequencyTable<>();
		FrequencyTable<Integer> lengthFreq = new FrequencyTable<>();
		FrequencyTable<Character> charFreq = new FrequencyTable<>();
		
		final int LITERAL_INDEX = -1;
		final int EOF_INDEX = -2;
		
		indexFreq.add(EOF_INDEX);
		
		List<InputLine> patchLines = new ArrayList<>();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(infile), StandardCharsets.UTF_8))) {
			String line;
			while((line = in.readLine()) != null) {
				if(line.startsWith("write ")) {

					String literal = line.substring(6);
					patchLines.add(new InputLine(literal));
					literalFreq.add(literal);
					indexFreq.add(LITERAL_INDEX);
					
					if(literalFreq.counts.get(literal) == 1) {
						for(int k = 0; k < literal.length(); k++)
							charFreq.add(literal.charAt(k));
					}
					charFreq.add('\uFFFE');
					
				} else if(line.startsWith("copy ")) {
					String[] parts = line.split(" ");
					int index = Integer.parseInt(parts[1]);
					int length = Integer.parseInt(parts[2]);
					indexFreq.add(index);
					lengthFreq.add(length);
					patchLines.add(new InputLine(index, length));
				}
			}
		} catch(IOException e) {
			throw new BuildException(e);
		}
		
		HuffmanTable<String> literalTable = HuffmanTable.build(literalFreq);
		HuffmanTable<Integer> indexTable = HuffmanTable.build(indexFreq);
		HuffmanTable<Integer> lengthTable = HuffmanTable.build(lengthFreq);
		HuffmanTable<Character> charTable = HuffmanTable.build(charFreq);
		
		try (BitOutputStream out = new BitOutputStream(new BufferedOutputStream(new FileOutputStream(outfile)))) {
			
			charTable.writeTable(out);
			writeStringHuffmanTable(literalTable.root, out, charTable);
			writeDiffedHuffmanTable(indexTable.root, out, 1);
			lengthTable.writeTable(out);
			
			for(InputLine line : patchLines) {
				if(line.isLiteral) {
					indexTable.write(LITERAL_INDEX, out);
					literalTable.write(line.literalString, out);
				} else {
					indexTable.write(line.index, out);
					lengthTable.write(line.length, out);
				}
			}
			indexTable.write(EOF_INDEX, out);
			
		} catch(IOException e) {
			throw new BuildException(e);
		}
	}
	
	private void writeStringHuffmanTable(HuffmanNode<String> t, BitOutputStream out, HuffmanTable<Character> charTable) throws IOException {
		if(t instanceof HuffmanNode.Node<?>) {
			out.write(true);
			writeStringHuffmanTable(((HuffmanNode.Node<String>)t).c0, out, charTable);
			writeStringHuffmanTable(((HuffmanNode.Node<String>)t).c1, out, charTable);
			return;
		}
		String val = ((HuffmanNode.Leaf<String>)t).value;
		
		out.write(false);

		for(int k = 0; k < val.length(); k++)
			charTable.write(val.charAt(k), out);
		charTable.write('\uFFFE', out);
	}
	
	private void writeDiffedHuffmanTable(HuffmanNode<Integer> t, final BitOutputStream out, int levels) throws IOException {
		
		if(levels == 0) {
			t.writeTree(out);
			return;
		}
		
		final FrequencyTable<Integer> diffFreq = new FrequencyTable<>();
		final int[] lastVal = {-2};
		t.accept(new HuffmanTreeVisitor<Integer>() {
			@Override
			public void visit(Leaf<Integer> n) {
				int diff = n.value - lastVal[0];
				diffFreq.add(diff);
				lastVal[0] = n.value;
			}
			@Override
			public void visit(Node<Integer> n) {
				n.c0.accept(this);
				n.c1.accept(this);
			}
		});
		
		final HuffmanTable<Integer> difftable = HuffmanTable.build(diffFreq);
		
		writeDiffedHuffmanTable(difftable.root, out, levels - 1);
		
		lastVal[0] = -2;
		t.accept(new HuffmanTreeVisitor<Integer>() {
			@Override
			public void visit(Leaf<Integer> n) {
				int diff = n.value - lastVal[0];
				try {
					out.write(false);
					difftable.write(diff, out);
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
				lastVal[0] = n.value;
			}
			@Override
			public void visit(Node<Integer> n) {
				try {
					out.write(true);
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
				n.c0.accept(this);
				n.c1.accept(this);
			}
		});
	}
}
