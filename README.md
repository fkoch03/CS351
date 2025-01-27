These are a few sections of code I wrote for my CompSci 351 coursework. All excerpts feature fail-fast implementation, an invariant for the data structure, and were tested using JUnit.

Lexicon.java implements a binary search tree as the data structure for a set, including efficient implementations 
of an iterator and all necessary inherited methods.

WordMultiset.java utilizes double hashing with an array to efficiently implement a map ADT, including a rehash function, and iterator.

LinkedSequence is a cyclically linked list, utilizing a tail field and a precursor field. The invariant features a tortoise and hare algorithm to check for "good" cyclical behavior.
