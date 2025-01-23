package edu.uwm.cs351;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

import edu.uwm.cs351.util.AbstractEntry;
import edu.uwm.cs351.util.Primes;

/**
 * Multiset of strings, placed in a hash table
 */
public class WordMultiset extends AbstractMap<String, Integer> {
	private static class MyEntry extends AbstractEntry<String, Integer> {
		String string;
		int count;

		MyEntry(String s) {
			this(s, 1);
		}

		MyEntry(String s, int c) {
			string = s;
			count = c;
		}

		@Override // required
		public String getKey() {
			return string;
		}

		@Override // required
		public Integer getValue() {
			return count;
		}

		@Override // implementation
		public Integer setValue(Integer v) {
			if (v == null || v <= 0)
				throw new IllegalArgumentException("must be positive: " + v);
			int old = count;
			count = v;
			return old;
		}
	}

	private static final int INITIAL_CAPACITY = 7;

	private MyEntry[] data;
	private int numUsed;
	private int numEntries;
	private int version;

	private static MyEntry PLACE_HOLDER = new MyEntry(null);

	/**
	 * Hash the key to a table index, following double hashing, returning the first
	 * index that (1) includes an entry with the key, or (2) has null, or (3) has a
	 * placeholder (if phOK is true *and* the key cannot be found). This code
	 * assumes that double hashing will find a valid index. It may run forever
	 * otherwise.
	 * 
	 * @param key  string to look for, must not be null
	 * @param phOK whether we return a slot with a placeholder in preference to an
	 *             empty slot
	 * @return first index meeting the requirements using double hashing.
	 */
	private int hash(String key, boolean phOK) {
		int phIndex = -1;
		int h = key.hashCode();
		int hOne = h % data.length;
		if (hOne < 0)
			hOne += data.length;
		if (data[hOne] == PLACE_HOLDER)
			phIndex = hOne;
		else if (data[hOne] == null || data[hOne].getKey().equals(key))
			return hOne;

		int hTwo = h % (data.length - 2);
		if (hTwo < 0)
			hTwo += (data.length - 2);
		hTwo += 1;

		for (;;) {
			hOne += hTwo;
			if (hOne >= data.length)
				hOne = hOne % data.length;
			if (data[hOne] == PLACE_HOLDER) {
				if (phIndex == -1)
					phIndex = hOne;
			} else if (data[hOne] == null) {
				if (phOK && phIndex != -1) {
					return phIndex;
				} else
					return hOne;
			} else if (data[hOne].getKey().equals(key))
				return hOne;
		}
	}

	private static Consumer<String> reporter = (s) -> System.out.println("Invariant error: " + s);

	/**
	 * Used to report an error found when checking the invariant. By providing a
	 * string, this will help debugging the class if the invariant should fail.
	 * 
	 * @param error string to print to report the exact error found
	 * @return false always
	 */
	private static boolean report(String error) {
		reporter.accept(error);
		return false;
	}

	/**
	 * Check the invariant. Returns false if any problem is found.
	 * 
	 * @return whether invariant is currently true. If false is returned then
	 *         exactly one problem has been reported.
	 */
	private boolean wellFormed() {
		// 1. The data array must not be null.
		if (data == null)
			return report("data is null");
		// 2. The length of the array should be the larger of twin primes and at least
		// the initial capacity (seven).
		if (data.length < 7)
			return report("data.length is too small");
		if (!Primes.isPrime(data.length) || !Primes.isPrime(data.length - 2))
			return report("array size is not the larger of twin primes");
		// 3. The numUsed should be the number of non-null entries in the array.
		// 4. The numEntries should be the number of real (non-zombie) entries in the
		// table.
		// 5. None of the real entries have a null key or a non-positive count.
		// 6. Every real entry can be found; it’s at the index where hash would find it.
		int countUsed = 0;
		int countEntries = 0;
		for (int i = 0; i < data.length; ++i) {
			if (data[i] != null) {
				++countUsed;
				if (data[i] != PLACE_HOLDER) {
					++countEntries;
					if (data[i].getKey() == null)
						return report("entry key is null");
					if (data[i].getValue() == null || data[i].getValue() < 1)
						return report("invalid entry count");
					if (hash(data[i].getKey(), false) != i)
						return report("entry can't be found");
				}
			}
		}
		if (countUsed != numUsed)
			return report("incorrect numUsed");
		if (countEntries != numEntries)
			return report("incorrect numEntries");

		// 7. The number of used entries is never more than half the array length.
		if ((numUsed * 2) > data.length)
			return report("array is too full");
		return true;
	}

	/**
	 * Creates an empty multiset
	 */
	public WordMultiset() {
		data = new MyEntry[INITIAL_CAPACITY];
		numEntries = 0;
		numUsed = 0;
		version = 0;
		assert wellFormed() : "invariant false at end of constructor";
	}

	@Override
	public Integer put(String key, Integer value) {
		if (key == null)
			throw new NullPointerException("key can't be null");
		if (value == null || value < 1)
			throw new IllegalArgumentException("key can't be null");
		assert wellFormed() : "invariant false at start of put()";
		Integer oldValue = null;
		int index = hash(key, false);
		if (data[index] == null) {
			++numEntries;
			++numUsed;
			++version;
			data[index] = new MyEntry(key, value);
		} else if (data[index] == PLACE_HOLDER) {
			++numEntries;
			++version;
			data[index] = new MyEntry(key, value);
		} else {
			oldValue = data[index].getValue();
			data[index].count = value;
		}

		if ((numUsed * 2) > data.length)
			rehash();
		assert wellFormed() : "invariant false at end of put()";
		return oldValue;
	}

	@Override // required
	public int size() {
		assert wellFormed() : "invariant false at start of size()";
		return numEntries;
	}

	@Override
	public boolean isEmpty() {
		assert wellFormed() : "invariant false at start of isEmpty()";
		return size() == 0;
	}

	@Override // efficiency
	public Integer get(Object o) {
		assert wellFormed() : "invariant false at start of get()";
		String str = objToStr(o);
		if (str == null)
			return null;
		int i = hash(str, false);
		if (data[i] == null || !data[i].getKey().equals(str))
			return null;
		else
			return data[i].getValue();
	}

	/**
	 * Create a new data array that is at least four times the number of entries (at
	 * least INITIAL_CAPACITY) and place all the entries in the order that they
	 * appear in the original array. The new array will have no place holders.
	 */
	private void rehash() {
		int newSize = INITIAL_CAPACITY;
		while (newSize < (4 * numEntries) || newSize < 6) {
			newSize = Primes.nextTwinPrime(newSize);
		}
		MyEntry[] oldData = this.data;
		data = new MyEntry[newSize];
		int countEntries = 0;
		for (int i = 0; i < oldData.length; ++i) {
			if (oldData[i] != null && oldData[i] != PLACE_HOLDER) {
				data[this.hash(oldData[i].getKey(), false)] = oldData[i];
				++countEntries;
				if (countEntries == numEntries)
					break;
			}
		}
		numUsed = numEntries;
	}

	/**
	 * Add a new string to the multiset. If it already exists, increase the count
	 * for the string and return false. Otherwise, set the count to one and return
	 * true.
	 * 
	 * @param str the string to add (must not be null)
	 * @return true if str was added, false otherwise
	 * @throws NullPointerException if str is null
	 */
	public boolean add(String str) {
		assert wellFormed() : "invariant false at start of add";
		if (str == null)
			throw new NullPointerException("str is null");
		boolean result = false;
		Integer i = get(str);
		if (i == null) {
			put(str, 1);
			result = true;
		} else {
			put(str, i + 1);
		}
		assert wellFormed() : "invariant false at end of add";
		return result;
	}

	@Override // efficiency
	public Integer remove(Object key) {
		assert wellFormed() : "invariant false at start of remove";
		String str = objToStr(key);
		if (str == null)
			return null;
		int i = hash(str, false);
		if (data[i] == null || data[i] == PLACE_HOLDER)
			return null;
		else {
			Integer toReturn = get(str);
			data[i] = PLACE_HOLDER;
			++version;
			--numEntries;
			assert wellFormed() : "invariant false at end of remove";
			return toReturn;
		}
	}

	/**
	 * Remove one copy of a word from the multiset. If there are multiple copies,
	 * then we just adjust the count, and the map is unaffected (iterators don't go
	 * stale).
	 * 
	 * @param str string to remove one of, may be null (but ignored if so)
	 * @return true if the word was in the multiset.
	 */
	public boolean removeOne(String str) {
		assert wellFormed() : "invariant false at start of removeOne";
		boolean result = true;
		Integer i = get(str);
		if (i == null)
			return false;
		if (i == 1) {
			remove(str);
		} else {
			put(str, i - 1);
		}
		assert wellFormed() : "invariant false at end of removeOne";
		return result;
	}

	@Override
	public boolean containsKey(Object key) {
		assert wellFormed() : "invariant false at start of containsKey";
		String str = objToStr(key);
		if (str == null)
			return false;
		int i = hash(str, false);
		if (data[i] == null)
			return false;
		else
			return true;
	}

	private String objToStr(Object o) {
		if (o == null || !(o instanceof String))
			return null;
		return (String) o;
	}

	private final EntrySet entrySet = new EntrySet();

	@Override // required
	public Set<Map.Entry<String, Integer>> entrySet() {
		assert wellFormed() : "invariant broken in entrySet";
		return entrySet;
	}

	private class EntrySet extends AbstractSet<Map.Entry<String, Integer>> {
		@Override // required
		public int size() {
			assert wellFormed() : "invariant failed in size";
			return numEntries;
		}

		@Override // efficiency
		public boolean contains(Object x) {
			assert wellFormed() : "invariant broken in contains";
			if (!(x instanceof Map.Entry<?, ?>))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) x;
			if (!(e.getKey() instanceof String))
				return false;
			if (!(e.getValue() instanceof Integer))
				return false;
			return e.getValue().equals(get(e.getKey()));
		}

		@Override // efficiency
		public boolean remove(Object x) {
			if (!contains(x))
				return false;
			WordMultiset.this.remove(((Map.Entry<?, ?>) x).getKey());
			return true;
		}

		@Override // required
		public Iterator<Map.Entry<String, Integer>> iterator() {
			assert wellFormed() : "invariant broken in iterator";
			return new EntrySetIterator();
		}
	}

	private class EntrySetIterator implements Iterator<Map.Entry<String, Integer>> {

		private int index;
		int remaining;
		private boolean canRemove;
		private int colVersion;

		private boolean wellFormed() {
			if (!WordMultiset.this.wellFormed())
				return false;
			if (version != colVersion)
				return true;
			int r = 0;
			if (index == data.length) {
				if (canRemove)
					return report("cannot remove when no element");
			} else {
				if (data[index] == null)
					return report("index is on null");
				if (data[index] == PLACE_HOLDER)
					return report("index is on place holder");
				if (!canRemove)
					++r;
			}
			for (int i = index + 1; i < data.length; ++i) {
				if (data[i] == null)
					continue;
				if (data[i] != PLACE_HOLDER)
					++r;
			}
			if (r != remaining)
				return report("remaining claims " + remaining + ", but should be " + r);
			return true;
		}

		private int nextSpot(int start) {
			if (remaining == 0) {
				return data.length;
			} else {
				do {
					++start;
				} while (data[start] == null || data[start] == PLACE_HOLDER);
				return start;
			}

		}

		EntrySetIterator() {
			remaining = numEntries;
			index = nextSpot(-1);
			canRemove = false;
			colVersion = version;
			assert wellFormed() : "invariant broken in iterator constructor";
		}

		private void checkVersion() {
			if (version != colVersion)
				throw new ConcurrentModificationException("stale");
		}

		@Override // required
		public boolean hasNext() {
			assert wellFormed() : "invariant broken in hasNext";
			checkVersion();
			return (remaining > 0);
		}

		@Override // required
		public Entry<String, Integer> next() {
			assert wellFormed() : "invariant broken in next";
			checkVersion();
			if (!hasNext())
				throw new NoSuchElementException("no more");
			if (!canRemove) {
				canRemove = true;
			} else {
				index = nextSpot(index);
			}
			--remaining;
			assert wellFormed() : "invariant broken by next";
			return data[index];
		}

		@Override // implementation
		public void remove() {
			assert wellFormed() : "invariant broken in remove";
			checkVersion();
			if (!canRemove)
				throw new IllegalStateException("Can't remove");
			data[index] = PLACE_HOLDER;
			--numEntries;
			++version;
			canRemove = false;
			index = nextSpot(index);
			colVersion = version;
			assert wellFormed() : "invariant broken by remove";
		}
	}
}
