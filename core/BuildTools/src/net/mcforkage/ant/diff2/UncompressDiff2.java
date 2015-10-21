package net.mcforkage.ant.diff2;

import java.io.IOException;
import java.io.PrintWriter;

import net.mcforkage.ant.compression.BitInputStream;
import net.mcforkage.ant.compression.HuffmanNode;
import net.mcforkage.ant.compression.HuffmanTable;

public class UncompressDiff2 {
	public static void uncompress(BitInputStream in, PrintWriter out) throws IOException {
		final int LITERAL_INDEX = -1;
		final int EOF_INDEX = -2;
		
		HuffmanTable<Character> charTable = HuffmanTable.readTable(in, Character.class);
		HuffmanTable<String> literalTable = new HuffmanTable<>(readStringHuffmanTable(in, charTable));
		HuffmanTable<Integer> indexTable = readDiffedHuffmanTable(in, 1);
		HuffmanTable<Integer> lengthTable = HuffmanTable.readTable(in, Integer.class);
		
		while(true) {
			int index = indexTable.read(in);
			if(index == EOF_INDEX)
				break;
			
			if(index == LITERAL_INDEX) {
				String s = literalTable.read(in);
				out.print("write ");
				out.println(s);
				
			} else {
				int length = lengthTable.read(in);
				
				out.print("copy ");
				out.print(index);
				out.print(" ");;
				out.println(length);
			}
		}
	}
	
	private static HuffmanNode<String> readStringHuffmanTable(BitInputStream in, HuffmanTable<Character> charTable) throws IOException {
		if(in.readBit())
			return new HuffmanNode.Node<String>(readStringHuffmanTable(in, charTable), readStringHuffmanTable(in, charTable));
		
		StringBuilder val = new StringBuilder();
		while(true) {
			char ch = charTable.read(in);
			if(ch == '\uFFFE')
				break;
			val.append(ch);
		}
		
		return new HuffmanNode.Leaf<String>(val.toString(), 0);
	}
	
	private static HuffmanNode<Integer> readDiffedHuffmanNode(BitInputStream in, HuffmanTable<Integer> diffTable, int[] lastValue) throws IOException {
		if(in.readBit())
			return new HuffmanNode.Node<Integer>(readDiffedHuffmanNode(in, diffTable, lastValue), readDiffedHuffmanNode(in, diffTable, lastValue));
		
		lastValue[0] += diffTable.read(in);
		return new HuffmanNode.Leaf<Integer>(lastValue[0], 0);
	}
	
	private static HuffmanTable<Integer> readDiffedHuffmanTable(final BitInputStream in, int levels) throws IOException {
		
		if(levels == 0)
			return HuffmanTable.readTable(in, Integer.class);

		HuffmanTable<Integer> diffTable = readDiffedHuffmanTable(in, levels - 1);
		
		return new HuffmanTable<>(readDiffedHuffmanNode(in, diffTable, new int[] {-2}));
	}
}
