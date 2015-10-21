package net.mcforkage.ant.compression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class HuffmanNode<T> {
	public static class Leaf<T> extends HuffmanNode<T> {
		public T value;
		
		public Leaf(T value, int freq) {
			this.value = value;
			this.freq = freq;
		}
		
		@Override
		void initCodes(HuffmanNode<T> parent, boolean nextBit, HuffmanTable<T> table) {
			super.initCodes(parent, nextBit, table);
			table.valueToNode.put(value, this);
		}
		
		@Override
		public void writeTree(BitOutputStream out) throws IOException {
			out.write(false);
			if(value instanceof Integer) {
				new DataOutputStream(out).writeInt((Integer)value);
			} else if(value instanceof Character) {
				new DataOutputStream(out).writeChar((Character)value);
			} else
				throw new RuntimeException("Can't write "+value);
		}
		
		@Override
		public void accept(HuffmanTreeVisitor<T> v) {
			v.visit(this);
		}
		
		@Override
		public T getRepresentativeValue() {
			return value;
		}
		
		@Override
		public T read(BitInputStream in) throws IOException {
			return value;
		}
	}
	public static class Node<T> extends HuffmanNode<T> {
		public HuffmanNode<T> c0, c1;
		
		public Node(HuffmanNode<T> c0, HuffmanNode<T> c1) {
			this.c0 = c0;
			this.c1 = c1;
			this.freq = c0.freq + c1.freq;
		}
		
		@Override
		void initCodes(HuffmanNode<T> parent, boolean nextBit, HuffmanTable<T> table) {
			super.initCodes(parent, nextBit, table);
			c0.initCodes(this, false, table);
			c1.initCodes(this, true, table);
		}
		
		@Override
		public void writeTree(BitOutputStream out) throws IOException {
			out.write(true);
			c0.writeTree(out);
			c1.writeTree(out);
		}
		
		@Override
		public void accept(HuffmanTreeVisitor<T> v) {
			v.visit(this);
		}
		
		@Override
		public T getRepresentativeValue() {
			return c0.getRepresentativeValue();
		}
		
		@Override
		public T read(BitInputStream in) throws IOException {
			return (in.readBit() ? c1 : c0).read(in);
		}
	}
	public int freq;
	
	public boolean[] code;

	void initCodes(HuffmanNode<T> parent, boolean nextBit, HuffmanTable<T> table) {
		if(parent == null)
			code = new boolean[0];
		else {
			code = new boolean[parent.code.length + 1];
			System.arraycopy(parent.code, 0, code, 0, parent.code.length);
			code[parent.code.length] = nextBit;
		}
	}

	public abstract void writeTree(BitOutputStream out) throws IOException;
	public abstract void accept(HuffmanTreeVisitor<T> v);
	public abstract T getRepresentativeValue();
	
	public static <T> HuffmanNode<T> readTree(BitInputStream in, Class<T> valueType) throws IOException {
		if(in.readBit())
			return new Node<T>(readTree(in, valueType), readTree(in, valueType));
		else {
			
			Leaf<T> l = new Leaf<T>(null, 0);
			
			if(valueType == Integer.class) {
				l.value = valueType.cast(new DataInputStream(in).readInt());
				
			} else if(valueType == Character.class) {
				l.value = valueType.cast(new DataInputStream(in).readChar());
				
			} else
				throw new RuntimeException("Can't read "+valueType);
			
			return l;
		}
	}

	public abstract T read(BitInputStream in) throws IOException;
}
