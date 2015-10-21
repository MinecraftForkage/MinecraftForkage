package net.mcforkage.ant.compression;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class HuffmanTable<T> {
	public HuffmanNode<T> root;
	public Map<T, HuffmanNode<T>> valueToNode = new HashMap<>();
	
	public HuffmanTable(HuffmanNode<T> root) {
		this.root = root;
		root.initCodes(null, false, this);
	}

	public static <T extends Comparable<? super T>> HuffmanTable<T> build(final FrequencyTable<T> freqs) {
		PriorityQueue<HuffmanNode<T>> pq = new PriorityQueue<HuffmanNode<T>>(freqs.counts.size(), new Comparator<HuffmanNode<T>>() {
			@Override
			public int compare(HuffmanNode<T> o1, HuffmanNode<T> o2) {
				int c = Integer.compare(o1.freq, o2.freq);
				if(c != 0)
					return c;
				
				return o1.getRepresentativeValue().compareTo(o2.getRepresentativeValue());
			}
		});
		
		for(Map.Entry<T, Integer> entry : freqs.counts.entrySet())
			pq.add(new HuffmanNode.Leaf<>(entry.getKey(), entry.getValue()));
		
		while(pq.size() > 1) {
			pq.add(new HuffmanNode.Node<>(pq.poll(), pq.poll()));
		}
		
		return new HuffmanTable<T>(pq.poll());
	}

	public void write(T value, BitOutputStream out) throws IOException {
		out.write(valueToNode.get(value).code);
	}

	public void writeTable(BitOutputStream out) throws IOException {
		root.writeTree(out);
	}

	public static <T> HuffmanTable<T> readTable(BitInputStream in, Class<T> valueType) throws IOException {
		return new HuffmanTable<T>(HuffmanNode.readTree(in, valueType));
	}

	public T read(BitInputStream in) throws IOException {
		return root.read(in);
	}
}
