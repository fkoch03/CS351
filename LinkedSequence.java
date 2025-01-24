package edu.uwm.cs351;

import java.util.function.Consumer;

public class LinkedSequence<E> implements Cloneable {
	private static Consumer<String> reporter = (s) -> System.out.println("Invariant error: " + s);

	/**
	 * Used to report an error found when checking the invariant. By providing a
	 * string, this will help debugging the class if the invariant should fail.
	 * 
	 * @param error string to print to report the exact error found
	 * @return false always
	 */
	private boolean report(String error) {
		reporter.accept(error);
		return false;
	}

	private static class Node<X> {
		X data;
		Node<X> next;

		public Node() {
			data = null;
			next = null;
		}

		@SuppressWarnings("unchecked")
		public Node(Object d, Node<X> n) {
			data = (X) d;
			next = n;
		}
	}

	private Node<E> tail;
	private int size;
	private Node<E> precursor;

	private Node<E> getHead() {
		return getDummy().next;
	}

	private Node<E> getDummy() {
		return tail.next;
	}

	private Node<E> getCursor() {
		return precursor.next;
	}

	// added to make checking for this faster / easier to read
	private boolean isEmpty() {
		return getDummy().next == getDummy();
	}

	/**
	 * Check the invariant. Report any problem precisely once. Return false if any
	 * problem is found. Returning an informative {@link #report(String)} will make
	 * it easier to debug invariant problems.
	 * 
	 * @return whether invariant is currently true
	 */
	private boolean wellFormed() {
		// Invariant:

		// 1. tail node is not null, and the dummy (next after tail) should not be null
		// either.
		if (tail == null)
			return report("tail is null");
		if (tail.next == null)
			return report("tail.next is null");

		// 2. The dummy node's data should be itself.
		if (getDummy().data != getDummy())
			return report("dummy has bad data");

		// 3. list must be in the form of a cycle from tail back to tail
		boolean isCyclic = false;
		Node<E> hare = tail;
		Node<E> tortoise = tail;

		if (!isEmpty()) {
			try {
				do {
					hare = hare.next.next;
					tortoise = tortoise.next;
					if (hare == tail) {
						isCyclic = true;
						break;
					}
					if (hare == tortoise) {
						return report("incorrectly cyclic");
					}
				} while (tortoise != tail);
				if (!isCyclic)
					return report("list is not cyclic");
			} catch (NullPointerException e) {
				return report("Node within list points to null");
			}
		}

		// 4. size is number of nodes in list, other than the dummy
		int count = 0;
		if (!isEmpty()) {
			Node<E> i = getDummy();
			do {
				i = i.next;
				++count;
			} while (i != tail);
		}
		if (size != count)
			return report("size is incorrect");

		// 5. precursor points to a node in the list (possibly the dummy).
		if (precursor == null)
			return report("precursor is null");
		for (Node<E> i = getDummy(); i != tail; i = i.next) {
			if (precursor.next == i) {
				break;
			}
			if (i.next == tail) {
				if (precursor.next == tail) {
					break;
				} else {
					return report("precursor doesn't point to an element in the list");
				}
			}
		}
		// checking if Node is in the list
		Node<E> c = getDummy();
		do {
			if (precursor == c) {
				break;
			}
			if (c == tail) {
				return report("precursor isn't in the list");
			}
			c = c.next;
		} while (c != getDummy());

		// If no problems found, then return true:
		return true;
	}

	/**
	 * Create an empty sequence.
	 * 
	 * @param - none
	 * @postcondition This sequence is empty
	 **/
	@SuppressWarnings("unchecked")
	public LinkedSequence() {
		Node<E> dummy = new Node<E>();
		dummy.next = dummy;
		dummy.data = (E) dummy;
		// what is data supposed to be?
		this.tail = new Node<E>(null, dummy);
		this.precursor = new Node<E>(null, dummy);
		this.size = 0;
		assert wellFormed() : "invariant failed in constructor";
	}

	/**
	 * Determine the number of elements in this sequence.
	 * 
	 * @param - none
	 * @return the number of elements in this sequence
	 **/
	public int size() {
		assert wellFormed() : "invariant wrong at start of size()";
		// This method shouldn't modify any fields, hence no assertion at end
		return this.size;
	}

	/**
	 * Set the current element at the front of this sequence.
	 * 
	 * @param - none
	 * @postcondition The front element of this sequence is now the current element
	 *                (but if this sequence has no elements at all, then there is no
	 *                current element).
	 **/
	public void start() {
		assert wellFormed() : "invariant wrong at start of start()";
		if (size() > 0) {
			precursor = getDummy();
		}
		assert wellFormed() : "invariant wrong at end of start()";
	}

	/**
	 * Accessor method to determine whether this sequence has a specified current
	 * element that can be retrieved with the getCurrent method.
	 * 
	 * @param - none
	 * @return true (there is a current element) or false (there is no current
	 *         element at the moment)
	 **/
	public boolean isCurrent() {
		assert wellFormed() : "invariant wrong at start of getCurrent()";
		// This method shouldn't modify any fields, hence no assertion at end
		return (precursor.next != getDummy());
	}

	/**
	 * Accessor method to get the current element of this sequence.
	 * 
	 * @param - none
	 * @precondition isCurrent() returns true.
	 * @return the current element of this sequence
	 * @exception IllegalStateException Indicates that there is no current element,
	 *                                  so getCurrent may not be called.
	 **/
	public E getCurrent() {
		assert wellFormed() : "invariant wrong at start of getCurrent()";
		// This method shouldn't modify any fields, hence no assertion at end
		if (!isCurrent())
			throw new IllegalStateException("no current element");
		return getCursor().data;
	}

	/**
	 * Move forward, so that the current element is now the next element in this
	 * sequence.
	 * 
	 * @param - none
	 * @precondition isCurrent() returns true.
	 * @postcondition If the current element was already the end element of this
	 *                sequence (with nothing after it), then there is no longer any
	 *                current element. Otherwise, the new element is the element
	 *                immediately after the original current element.
	 * @exception IllegalStateException Indicates that there is no current element,
	 *                                  so advance may not be called.
	 **/
	public void advance() {
		assert wellFormed() : "invariant wrong at start of advance()";
		if (!isCurrent())
			throw new IllegalStateException("no current Node");
		precursor = precursor.next;
		assert wellFormed() : "invariant wrong at end of advance()";
	}

	/**
	 * Remove the current element from this sequence.
	 * 
	 * @param - none
	 * @precondition isCurrent() returns true.
	 * @postcondition The current element has been removed from this sequence, and
	 *                the following element (if there is one) is now the new current
	 *                element. If there was no following element, then there is now
	 *                no current element.
	 * @exception IllegalStateException Indicates that there is no current element,
	 *                                  so removeCurrent may not be called.
	 **/
	public void removeCurrent() {
		assert wellFormed() : "invariant wrong at start of removeCurrent()";
		if (!isCurrent())
			throw new IllegalStateException("no current Node");
		if (getCursor() == tail) {
			precursor.next = tail.next;
			tail = precursor;
		} else {
			precursor.next = getCursor().next;
		}
		--size;
		assert wellFormed() : "invariant wrong at end of removeCurrent()";
	}

	/**
	 * Add a new element to this sequence, before the current element (if any). If
	 * the new element would take this sequence beyond its current capacity, then
	 * the capacity is increased before adding the new element.
	 * 
	 * @param element the new element that is being added
	 * @postcondition A new copy of the element has been added to this sequence. If
	 *                there was a current element, then the new element is placed
	 *                before the current element. If there was no current element,
	 *                then the new element is placed at the end of the sequence. In
	 *                all cases, the new element becomes the new current element of
	 *                this sequence.
	 * @exception OutOfMemoryError Indicates insufficient memory for increasing the
	 *                             sequence.
	 **/
	public void insert(E element) {
		assert wellFormed() : "invariant failed at start of insert";
		Node<E> insert = new Node<E>(element, null);
		if (isCurrent()) {
			insert.next = getCursor();
			precursor.next = insert;
		} else if (!isCurrent()) {
			if (isEmpty()) {
				insert.next = getDummy().next;
				getDummy().next = insert;
				tail = insert;
				precursor = getDummy();
			} else {
				Node<E> temp = tail;

				insert.next = getDummy();
				temp.next = insert;
				tail = insert;
				precursor = temp;
			}

		}
		++size;
		assert wellFormed() : "invariant failed at end of insert";
	}

	/**
	 * Place the contents of another sequence (which may be the same one as this!)
	 * into this sequence before the current element (if any).
	 * 
	 * @param addend a sequence whose contents will be placed into this sequence
	 * @precondition The parameter, addend, is not null.
	 * @postcondition The elements from addend have been placed into this sequence.
	 *                The current element of this sequence (if any) is unchanged.
	 *                The addend is unchanged.
	 * @exception NullPointerException Indicates that addend is null.
	 * @exception OutOfMemoryError     Indicates insufficient memory to increase the
	 *                                 size of this sequence.
	 **/
	public void insertAll(LinkedSequence<E> addend) {
		assert wellFormed() : "invariant failed at start of insertAll";
		if (addend == null)
			throw new NullPointerException("addend is null");
		if (addend.size() == 0)
			return;
		LinkedSequence<E> copy = addend.clone();
		if (this.isCurrent()) {
			for (Node<E> n = copy.getHead(); n != copy.getDummy(); n = n.next) {
				this.insert(n.data);
				this.precursor = this.precursor.next;
			}
		} else {
			for (Node<E> n = copy.getHead(); n != copy.getDummy(); n = n.next) {
				this.insert(n.data);
				this.precursor = this.precursor.next;
			}
			this.precursor = this.tail;
		}
		assert wellFormed() : "invariant failed at end of insertAll";
	}

	/**
	 * Generate a copy of this sequence.
	 * 
	 * @param - none
	 * @return The return value is a copy of this sequence. Subsequent changes to
	 *         the copy will not affect the original, nor vice versa. Whatever was
	 *         current in the original object is now current in the clone.
	 * @exception OutOfMemoryError Indicates insufficient memory for creating the
	 *                             clone.
	 **/
	@SuppressWarnings("unchecked")
	public LinkedSequence<E> clone() {
		assert wellFormed() : "invariant wrong at start of clone()";

		LinkedSequence<E> result;

		try {
			result = (LinkedSequence<E>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("This class does not implement Cloneable");
		}

		if (isEmpty()) {
			return new LinkedSequence<E>();
		}
		result = new LinkedSequence<E>();
		Node<E> resultCurrent = result.getDummy();
		for (Node<E> n = this.getHead(); n != this.getDummy(); n = n.next) {
			Node<E> add = new Node<>(n.data, result.getDummy());
			resultCurrent.next = add;
			if (this.precursor.next == n)
				result.precursor = resultCurrent;
			result.tail = add;
			resultCurrent = result.tail;
			++result.size;
		}
		if (!this.isCurrent())
			result.precursor = result.tail;
		assert wellFormed() : "invariant wrong at end of clone()";
		assert result.wellFormed() : "invariant wrong for result of clone()";
		return result;
	}
}
