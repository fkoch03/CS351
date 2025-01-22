package edu.uwm.cs351;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.function.Consumer;

//import edu.uwm.cs351.Lexicon.Node;

/**
 * Set of strings, sorted lexicographically.
 */
public class Lexicon extends AbstractSet<String> {
	
	private static class Node {
		String string;
		Node left, right;
		Node (String s) { string = s; }
		@Override
		public String toString() 
		{
			return super.toString() + "'" + string + "'";
		}
	}
	
	private Node root;
	private int numNodes;
	private int version;
	
	private static Consumer<String> reporter = (s) -> System.out.println("Invariant error: "+ s);
	
	/**
	 * Used to report an error found when checking the invariant.
	 * By providing a string, this will help debugging the class if the invariant should fail.
	 * @param error string to print to report the exact error found
	 * @return false always
	 */
	private boolean report(String error) {
		reporter.accept(error);
		return false;
	}

	private int reportNeg(String error) {
		report(error);
		return -1;
	}

	/**
	 * Count all the nodes in this subtree, 
	 * while checking that all the keys are all in the range (lo,hi),
	 * and that the keys are arranged in BST form.
	 * If a problem is found, -1 is returned and exactly one problem is reported.
	 * <p>
	 * @param n the root of the subtree to check
	 * @param lo if non-null then all strings in the subtree rooted
	 * 				at n must be [lexicographically] greater than this parameter
	 * @param hi if non-null then all strings in the subtree rooted
	 * 				at n must be [lexicographically] less than this parameter
	 * @return number of nodes in the subtree, or -1 is there is a problem.
	 */
	private int checkInRange(Node n, String lo, String hi)
	{
		//must account for checking an empty list or leaf's links
		if (n == null) return 0;
		if (n.string == null) return reportNeg("null word found");
		
		//first check node r
		if (lo != null && (n.string.equals(lo) || n.string.compareTo(lo) < 0))
			return reportNeg("Detected node outside of low bound: "+n.string);
		if (hi != null && (n.string.equals(hi) || n.string.compareTo(hi) > 0))
			return reportNeg("Detected node outside of high bound: "+n.string);
		
		//check subtrees
		int leftSubtree =  checkInRange(n.left, lo, n.string);
		if (leftSubtree < 0) return -1;
		
		int rightSubtree = checkInRange(n.right, n.string, hi);
		if (rightSubtree < 0) return -1;
				
		//otherwise return 1 + nodes in subtrees
		return 1 + leftSubtree + rightSubtree;
	}
	
	/**
	 * Check the invariant.  
	 * Returns false if any problem is found. 
	 * @return whether invariant is currently true.
	 * If false is returned then exactly one problem has been reported.
	 */
	private boolean wellFormed() {
		int n = checkInRange(root, null, null);
		if (n < 0) return false; // problem already reported
		if (n != numNodes) return report("numNodes is " + numNodes + " but should be " + n);
		return true;
	}
	
	/**
	 * Creates an empty lexicon.
	 */
	public Lexicon() {
		root = null;
		numNodes = 0;
		assert wellFormed() : "invariant false at end of constructor";
	}
	

	@Override // required
	public int size() {
		assert wellFormed() : "invariant false at start of size()";
		return numNodes;
	}
	
	/**
	 * Gets the [lexicographically] least string in the lexicon.
	 * @return the least string or null if empty
	 */
	public String getMin() {
		assert wellFormed() : "invariant false at start of getMin()";
		if (root == null) return null;
		Iterator<String> min = iterator();
		return min.next();
	}
	
	/**
	 * Gets the next [lexicographically] greater string than the given string.
	 * @param str the string of which to find the next greatest
	 * @return the next string greater than str, or null if no other
	 * @throws NullPointerException if str is null
	 */
	public String getNext(String str) {
		assert wellFormed() : "invariant false at start of getNext()";
		// TODO: Implement this method using the special iterator constructor.
		// HINT: If you add "\0" to the string and look for it with the iterator, 
		// you are most of the way there.
		if (str == null) throw new NullPointerException("String str is null");
		str += "\0";
		Iterator<String> getNext = iterator(str);
		if (!getNext.hasNext()) return null;
		return getNext.next();
	}
	
	/**
	 * Accept into the consumer all strings in this lexicon.
	 * @param consumer the consumer to accept the strings
	 * @throws NullPointerException if consumer is null
	 */
	public void consumeAll(Consumer<String> consumer) {
		consumeAllWithPrefix(consumer,"");
	}
	
	/**
	 * Accept into the consumer all strings that start with the given prefix.
	 * @param consumer the consumer to accept the strings
	 * @param prefix the prefix to find all strings starting with
	 * @throws NullPointerException if consumer or prefix is null
	 */
	public void consumeAllWithPrefix(Consumer<String> consumer, String prefix) {
		assert wellFormed() : "invariant false at start of consumeAllWithPrefix()";
		if (consumer == null) throw new NullPointerException("Can't accept into null consumer");
		if (prefix == null) throw new NullPointerException("Prefix can't be null");
		Iterator<String> iterator = iterator(prefix);
		String toAccept = prefix;
		while (iterator.hasNext() && toAccept.startsWith(prefix))
		{
		toAccept = iterator.next();
		if (toAccept.startsWith(prefix))
			{
			consumer.accept(toAccept);
			}
		}
	}
	
	@Override //efficiency
	public boolean contains(Object o)
		{
		assert wellFormed() : "invariant false at start of contains()";
		if (!(o instanceof String)) return false;
		String toFind = (String)o;
		return find(root, toFind) != null;
		}
	
	
	/**
	 * Add a new string to the lexicon. If it already exists, do nothing and return false.
	 * @param str the string to add (must not be null)
	 * @return true if str was added, false otherwise
	 * @throws NullPointerException if str is null
	 */
	@Override // implementation
	public boolean add(String str) {
		assert wellFormed() : "invariant false at start of add()";
		boolean result = false;
		if (str == null) throw new NullPointerException("Cannot add null.");
		Node n = root;
		Node lag = null;
		while (n != null) {
			if (n.string.equals(str)) break;
			lag = n;
			if (str.compareTo(n.string) > 0) n = n.right;
			else n = n.left;
		}
		if (n == null) {
			n = new Node(str);
			if (lag == null)
				root = n;
			else if (str.compareTo(lag.string) > 0)
				lag.right = n;
			else
				lag.left = n;
			++numNodes;
			result = true;
			++version;
		}
		assert wellFormed() : "invariant false at end of add()";
		return result;
	}
	
@Override //efficiency
public boolean remove(Object x)
	{
	assert wellFormed() : "invariant failed at start of remove";
	if (!(x instanceof String)) return false;
	String toRemove = (String)x;
	int oldSize = numNodes;
	Node remove = find(root, toRemove);
	if (remove != null) 
		{
		doRemove(remove, root);
		++version;
		--numNodes;
		return true;
		}
	assert wellFormed() : "invariant failed at end of remove";
	return oldSize != numNodes;
	}

private Node find(Node current, String find) 
	{
	if (current == null) return null;
	
	int c = find.compareTo(current.string);

	if (c == 0)
		{
		return current;
		}
	else if (c > 0)
		{
		return find(current.right, find);
		}
	else if (c < 0)
		{
		return find(current.left, find);
		}
	return null;
	}

private void doRemove(Node remove, Node startNode)
{
if (remove.left != null && remove.right != null)
	{ //left and right, find immediate *predecessor*
	
	Node immPre = remove.left;
	while (immPre.right != null)
		{
		immPre = immPre.right;
		}
	remove.string = immPre.string;
	doRemove(immPre, remove);
	return;
	}

else //promote child AND cut off
	{
	if (remove == root)
		{
		root = (remove.left == null) ? remove.right : remove.left;
		remove = null;
		}
	else
		{
		Node prev = startNode;
		boolean left = false;
		while (prev != null)
			{
			int c = remove.string.compareTo(prev.string);
			if (c > 0)
				{
				if (prev.right == remove) break;
				prev = prev.right;
				}
			else if (c <= 0)
				{
				if (prev.left == remove)
					{
					left = true;
					break;
					}
				prev = prev.left;
				}
			}
			if(left) 
				{
				prev.left = (remove.left == null) ? remove.right : remove.left;
				}
			else
				{
				prev.right = (remove.left == null) ? remove.right : remove.left;
				}
			remove = null;
			return;
		}
	}
}

	
	private boolean isNextGreaterAncestor(Node n, Node a) {
		Node p = a == null ? root : a.left;
		while (p != null) {
			if (n == p) return true;
			p = p.right;
		}
		return false;
	}
	
	private boolean isNext(Node p, Node n) {
		if (p.right == null) return isNextGreaterAncestor(p,n);
		else {
			p = p.right;
			while (p.left != null) p = p.left;
			return p == n;
		}
	}
	
	@Override // required
	public Iterator<String> iterator() {
		return new MyIterator();
	}
	
	/**
	 * Return an iterator that starts at the given element, or the next
	 * available element from the set.
	 * @param start starting element (or element before starting element,
	 * if the start isn't in the set), must not be null
	 * @return iterator starting "in the middle" (never null)
	 */
	public Iterator<String> iterator(String start) {
		return new MyIterator(start);
	}
	
	private class MyIterator implements Iterator<String> {
		private Stack<Node> pending = new Stack<>();
		private Node current = null; // when not null, we have a current element
		private int colVersion = version;

		
		private boolean wellFormed() {
			if (!Lexicon.this.wellFormed()) return false;
			if (version != colVersion) return true;
			Node prev = null;
			// stack iterator starts at BOTTOM
			for (Node n : pending) {
				if (!isNextGreaterAncestor(n,prev)) return report("pending wrong: " + n + " under " + prev);
				prev = n;
			}
			if (current != null) {
				if (!isNext(current,prev)) return report("current wrong: " + current + " before " + prev);
			}
			return true;
		}
		
		private void checkVersion() {
			if (colVersion != version) {
				throw new ConcurrentModificationException("stale iterator");
			}
		}
		
		/**
		 * Start the iterator at the first (lexicographically) node.
		 */
		public MyIterator() {
			this("");
			assert wellFormed() : "Iterator messed up after default constructor";
		}
		
		/**
		 * Start the iterator at this element, or at the first element after it
		 * (if any).  		 
		 * @param initial string to start at, must not be null
		 */
		public MyIterator(String initial) 
		{
			// TODO Set up an iterator starting with given (non-null) string.
			// NB: Do not attempt to use {@link #getNext} or any other method 
			// of the main class to help.  All the work needs to be done here 
			// so that the pending stack is set up correctly.
			if (root == null) return;
			traverseBST(initial, root);
			colVersion = version;
			assert wellFormed() : "Iterator messed up after special constructor";
		}
		
		private void traverseBST(String initial, Node n){
		if (n == null) return;
		if (n.string.compareTo(initial) >= 0)
			{
			pending.push(n);
			traverseBST(initial, n.left);
			}
		else if (n.right != null)
			{
			traverseBST(initial, n.right);
			}
		
		}

		@Override
		public boolean hasNext() 
		{
		assert wellFormed() : "wellFormed failed at start of hasNext()";
		checkVersion();
		return !pending.empty();
		}

		@Override
		public String next() 
		{
		assert wellFormed() : "wellFormed failed at start of next()";
		if (!hasNext()) throw new NoSuchElementException("no next value");
		Node toReturn = pending.pop();
		current = toReturn;
		traverseBST("", toReturn.right);
		return toReturn.string;
		}

		@Override
		public void remove()
		{
		checkVersion();
		if (current == null) throw new IllegalStateException("no current element");
		if (Lexicon.this.remove(current.string))
			{
			++colVersion;
			current = null;
			}
		}	
	}
}
