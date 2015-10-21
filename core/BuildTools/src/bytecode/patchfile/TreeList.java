package bytecode.patchfile;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class TreeList<T> extends AbstractList<T> {
	
	private List<T> left;
	private List<T> right;
	
	private int cachedSize = -1;
	
	private static <T> List<T> createBalancedTree(List<List<T>> chunks) {
		if(chunks.size() == 1)
			return chunks.get(0);
		
		int mid = chunks.size() / 2;
		return new TreeList<T>(createBalancedTree(chunks.subList(0, mid)), createBalancedTree(chunks.subList(mid, chunks.size())));
	}
	
	public TreeList(List<? extends T> from) {
		
		int fromSize = from.size();
		final int CHUNKSIZE = 128;
		int numChunks = (fromSize + CHUNKSIZE - 1) / CHUNKSIZE;
		List<List<T>> chunks = new ArrayList<>();
		
		for(int chunkstart = 0; chunkstart < fromSize; chunkstart += CHUNKSIZE) {
			int chunkend = Math.min(chunkstart + CHUNKSIZE, fromSize);
			
			chunks.add(new ArrayList<>(from.subList(chunkstart, chunkend)));
		}
		
		if(numChunks == 0) {
			left = new ArrayList<>();
			right = new ArrayList<>();
		} else if(numChunks == 1) {
			left = chunks.get(0);
			right = new ArrayList<>();
		} else {
			left = createBalancedTree(chunks.subList(0, chunks.size()/2));
			right = createBalancedTree(chunks.subList(chunks.size()/2, chunks.size()));
		}
		
		if(size() != fromSize)
			throw new AssertionError();
	}
	
	public TreeList(List<T> left, List<T> right) {
		this.left = left;
		this.right = right;
		this.cachedSize = left.size() + right.size();
	}

	@Override
	public int size() {
		if(cachedSize < 0)
			cachedSize = left.size() + right.size();
		return cachedSize;
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			Iterator<T> cur = left.iterator();
			Iterator<T> next = right.iterator();
			@Override
			public boolean hasNext() {
				if(cur.hasNext())
					return true;
				if(next == null)
					return false;
				cur = next;
				next = null;
				return cur.hasNext();
			}
			@Override
			public T next() {
				if(cur.hasNext())
					return cur.next();
				if(next == null)
					throw new NoSuchElementException();
				cur = next;
				next = null;
				return cur.next();
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <U> U[] toArray(U[] a) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(T e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return addAll(size(), c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		int mid = left.size();
		cachedSize = -1;
		if(index < mid)
			return left.addAll(index, c);
		else
			return right.addAll(index - mid, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		left.clear();
		right.clear();
		cachedSize = 0;
	}

	@Override
	public T get(int index) {
		int mid = left.size();
		if(index < mid)
			return left.get(index);
		else
			return right.get(index - mid);
	}

	@Override
	public T set(int index, T element) {
		int mid = left.size();
		if(index < mid)
			return left.set(index, element);
		else
			return right.set(index - mid, element);
	}

	@Override
	public void add(int index, T element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexOf(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int lastIndexOf(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<T> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		if(fromIndex == 0 && toIndex == size())
			return this;
		cachedSize = -1;
		int mid = left.size();
		if(fromIndex < mid && toIndex <= mid)
			return left.subList(fromIndex, toIndex);
		if(fromIndex >= mid && toIndex >= mid)
			return right.subList(fromIndex - mid, toIndex - mid);
		
		return new TreeList<T>(left.subList(fromIndex, mid), right.subList(0, toIndex - mid));
		//return super.subList(fromIndex, toIndex);
	}
}
