package net.mcforkage.ant.compression;

public interface HuffmanTreeVisitor<T> {
	public void visit(HuffmanNode.Node<T> n);
	public void visit(HuffmanNode.Leaf<T> n);
}
