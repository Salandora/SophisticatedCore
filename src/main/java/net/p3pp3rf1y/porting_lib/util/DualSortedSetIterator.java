package net.p3pp3rf1y.porting_lib.util;

import com.google.common.collect.AbstractIterator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import org.jetbrains.annotations.Nullable;

public class DualSortedSetIterator<T> extends AbstractIterator<T> {
	private final Comparator<T> comparator;
	private final Iterator<T> first;
	private final Iterator<T> second;

	private T firstNext;
	private T secondNext;

	public DualSortedSetIterator(SortedSet<T> first, SortedSet<T> second) {
		//noinspection unchecked
		this.comparator = (Comparator<T>) first.comparator();
		this.first = first.iterator();
		this.second = second.iterator();
	}


	@Nullable
	@Override
	protected T computeNext() {
		if (firstNext == null && first.hasNext())
			firstNext = first.next();
		if (secondNext == null && second.hasNext())
			secondNext = second.next();

		if (firstNext == null) {
			if (secondNext == null) { // both null, both done
				return endOfData();
			} else { // second is available
				return consume(secondNext);
			}
		} else if (secondNext == null) { // both null, again
			return endOfData();
		} else { // neither null
			int compared = comparator.compare(firstNext, secondNext);
			// <0: firstNext < secondNext, or firstNext.getIndex() < secondNext.getIndex()
			// =0: go with first
			// >0: second is smaller
			T next = compared <= 0 ? firstNext : secondNext;
			return consume(next);
		}
	}

	private T consume(T element) {
		if (element == firstNext)
			firstNext = null;
		if (element == secondNext)
			secondNext = null;
		return element;
	}
}
