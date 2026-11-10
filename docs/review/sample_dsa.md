# Review sample: Data Structures and Algorithms for placements

Please mark each question OK / FIX / REMOVE and write corrections in the right-hand column of the sign-off sheet.
40 questions drawn with a fixed seed from 210. The tag after each wrong option is the misconception it is meant to reveal.

**1. [dsa.bst.q3] (difficulty 3)** Which tree is NOT a valid BST: root 10, left child 5 with right child 12?
   A) It is valid, all children are ordered  <- reveals: Checks only the direct children when validating a BST
   B) It cannot be decided  <- reveals: Checks only the direct children when validating a BST
   C) It is invalid, 12 is in the left subtree but larger than 10  <- CORRECT
   D) It is valid, 12 > 5  <- reveals: Checks only the direct children when validating a BST

**2. [dsa.sliding_window.q5] (difficulty 3)** For longest substring without repeating characters you track
   A) the characters in the current window with a set or map  <- CORRECT
   B) the sorted order of the string  <- reveals: Applies sliding window to non-contiguous subsequences
   C) the sum of the window  <- reveals: Recomputes the whole window sum from scratch each step
   D) only the left index  <- reveals: Recomputes the whole window sum from scratch each step

**3. [dsa.topo.q5] (difficulty 3)** A DAG with edges A->C and B->C has
   A) exactly one order  <- reveals: Believes the topological order is always unique
   B) only the order C, A, B  <- reveals: Believes topological sort works on graphs with cycles
   C) no valid order  <- reveals: Believes topological sort works on graphs with cycles
   D) more than one valid topological order  <- CORRECT

**4. [dsa.topo.q4] (difficulty 3)** Time complexity of topological sort is
   A) O(V + E)  <- CORRECT
   B) O(V^2) always  <- reveals: Believes topological sort works on graphs with cycles
   C) O(E^2)  <- reveals: Believes topological sort works on graphs with cycles
   D) O(V log V)  <- reveals: Believes topological sort works on graphs with cycles

**5. [dsa.recursion.q5] (difficulty 2)** Which base case is correct for factorial(n)?
   A) n == 1 returns 0  <- reveals: Forgets that recursion needs a base case and a step toward it
   B) n == 0 returns 0  <- reveals: Forgets that recursion needs a base case and a step toward it
   C) n == 0 returns 1  <- CORRECT
   D) no base case needed  <- reveals: Forgets that recursion needs a base case and a step toward it

**6. [dsa.complexity.q1] (difficulty 1)** What is the time complexity of a single for loop that runs from 1 to n doing constant work per step?
   A) O(log n)  <- reveals: Confuses best case with worst case, or ignores the recursion stack in space complexity
   B) O(n)  <- CORRECT
   C) O(1)  <- reveals: Thinks O(2n) or O(n+5) is a different, larger class than O(n)
   D) O(n^2)  <- reveals: Thinks nested loops are always O(n^2) even when the inner loop is bounded by a constant or halves

**7. [dsa.dp_1d.q4] (difficulty 3)** In coin change DP, unreachable amounts should be initialised to
   A) the amount itself  <- reveals: Starts coin change dp with zeros instead of infinity for unreachable amounts
   B) 0  <- reveals: Starts coin change dp with zeros instead of infinity for unreachable amounts
   C) infinity (a large number)  <- CORRECT
   D) -1 and then added to  <- reveals: Starts coin change dp with zeros instead of infinity for unreachable amounts

**8. [dsa.bit.q6] (difficulty 3)** For positive x, x & (x - 1) == 0 means
   A) x is odd  <- reveals: Thinks x & (x - 1) sets the lowest bit instead of clearing it
   B) x is a power of two  <- CORRECT
   C) x is a prime  <- reveals: Thinks x & (x - 1) sets the lowest bit instead of clearing it
   D) x is zero only  <- reveals: Thinks x & (x - 1) sets the lowest bit instead of clearing it

**9. [dsa.queue.q3] (difficulty 2)** A deque supports O(1) insert and remove at
   A) any position  <- reveals: Confuses a priority queue with a normal FIFO queue
   B) the middle only  <- reveals: Confuses a priority queue with a normal FIFO queue
   C) both ends  <- CORRECT
   D) the front only  <- reveals: Confuses a priority queue with a normal FIFO queue

**10. [dsa.greedy.q3] (difficulty 3)** Coins {1, 3, 4}, amount 6: greedy largest-first gives
   A) 3 coins (4, 1, 1) while optimal is 2 (3, 3)  <- CORRECT
   B) 1 coin  <- reveals: Believes greedy always gives the optimal answer
   C) the optimal 2 coins  <- reveals: Believes greedy always gives the optimal answer
   D) it fails with an error  <- reveals: Believes greedy always gives the optimal answer

**11. [dsa.dfs.q3] (difficulty 3)** Without a visited set DFS on a graph with a cycle will
   A) never terminate (infinite recursion)  <- CORRECT
   B) visit each node once  <- reveals: Forgets the visited set and loops forever on cycles
   C) find the shortest cycle  <- reveals: Forgets the visited set and loops forever on cycles
   D) finish faster  <- reveals: Forgets the visited set and loops forever on cycles

**12. [dsa.backtracking.q2] (difficulty 3)** The number of subsets of an n-element set is
   A) n^2  <- reveals: Believes backtracking is polynomial time because of pruning
   B) n!  <- reveals: Believes backtracking is polynomial time because of pruning
   C) 2^n  <- CORRECT
   D) 2n  <- reveals: Believes backtracking is polynomial time because of pruning

**13. [dsa.recursion.q6] (difficulty 2)** What does power(x, n) = x * power(x, n-1) with power(x,0)=1 compute for n=3?
   A) x+3  <- reveals: Forgets that recursion needs a base case and a step toward it
   B) x^3  <- CORRECT
   C) x^4  <- reveals: Forgets that recursion needs a base case and a step toward it
   D) 3x  <- reveals: Forgets that recursion needs a base case and a step toward it

**14. [dsa.trie.q5] (difficulty 3)** Why is a hash set poor for prefix queries?
   A) It cannot find all keys with a given prefix without scanning  <- CORRECT
   B) It stores characters, not words  <- reveals: Believes a hash set can answer prefix queries in O(1)
   C) It is slower than a trie for exact lookups in O(1)  <- reveals: Believes a hash set can answer prefix queries in O(1)
   D) It always has collisions  <- reveals: Believes a hash set can answer prefix queries in O(1)

**15. [dsa.trees.q1] (difficulty 2)** Inorder traversal visits nodes in the order
   A) root, left, right  <- reveals: Mixes up preorder, inorder and postorder
   B) right, root, left  <- reveals: Mixes up preorder, inorder and postorder
   C) left, right, root  <- reveals: Mixes up preorder, inorder and postorder
   D) left, root, right  <- CORRECT

**16. [dsa.linked_list.q5] (difficulty 2)** The slow pointer is at the middle when the fast pointer moving two steps reaches
   A) the node after the head  <- reveals: Believes you can access the kth node in O(1) like an array
   B) the end of the list  <- CORRECT
   C) the head  <- reveals: Believes you can access the kth node in O(1) like an array
   D) a random node  <- reveals: Believes you can access the kth node in O(1) like an array

**17. [dsa.math_number.q6] (difficulty 2)** Fast exponentiation computes x^n in
   A) O(n^2)  <- reveals: Believes you must test all divisors up to n to check primality
   B) O(1)  <- reveals: Believes you must test all divisors up to n to check primality
   C) O(n) multiplications  <- reveals: Believes you must test all divisors up to n to check primality
   D) O(log n) multiplications  <- CORRECT

**18. [dsa.dfs.q4] (difficulty 3)** In a directed graph a back edge, meaning a neighbour currently on the recursion path, indicates
   A) a topological order  <- reveals: Treats any previously visited neighbour in a directed graph as a cycle
   B) a cycle  <- CORRECT
   C) a connected component  <- reveals: Treats any previously visited neighbour in a directed graph as a cycle
   D) a shortest path  <- reveals: Treats any previously visited neighbour in a directed graph as a cycle

**19. [dsa.backtracking.q6] (difficulty 3)** If you forget to remove the last chosen element after recursion in subset generation, you get
   A) an automatic fix by the language  <- reveals: Forgets to undo the choice after the recursive call
   B) only duplicates removed  <- reveals: Forgets to undo the choice after the recursive call
   C) the correct answer faster  <- reveals: Forgets to undo the choice after the recursive call
   D) wrong, ever-growing paths in later branches  <- CORRECT

**20. [dsa.dp_intro.q1] (difficulty 3)** Memoized Fibonacci runs in
   A) O(log n)  <- reveals: Believes memoization does not change time complexity
   B) O(2^n)  <- reveals: Believes memoization does not change time complexity
   C) O(n)  <- CORRECT
   D) O(n^2)  <- reveals: Believes memoization does not change time complexity

**21. [dsa.sorting_basic.q6] (difficulty 2)** Why is insertion sort used for small subarrays in library sorts?
   A) It is O(n) in the worst case  <- reveals: Believes insertion sort is always O(n^2) even on sorted input
   B) It uses no comparisons  <- reveals: Confuses stability with being fast or in-place
   C) It is the only stable sort  <- reveals: Confuses stability with being fast or in-place
   D) It has low overhead and is fast on small or nearly sorted data  <- CORRECT

**22. [dsa.greedy.q2] (difficulty 3)** Greedy by value per weight is optimal for
   A) 0/1 knapsack  <- reveals: Applies greedy value-per-weight to 0/1 knapsack and expects optimal results
   B) longest common subsequence  <- reveals: Believes greedy always gives the optimal answer
   C) coin change with arbitrary coins  <- reveals: Believes greedy always gives the optimal answer
   D) fractional knapsack  <- CORRECT

**23. [dsa.complexity.q6] (difficulty 1)** Linear search for a target in an unsorted array. When is it O(1)?
   A) Always, because it is a simple loop  <- reveals: Confuses best case with worst case, or ignores the recursion stack in space complexity
   B) Best case, target is the first element  <- CORRECT
   C) Never, linear search is always O(n)  <- reveals: Confuses best case with worst case, or ignores the recursion stack in space complexity
   D) Worst case, target is the last element  <- reveals: Confuses best case with worst case, or ignores the recursion stack in space complexity

**24. [dsa.dfs.q5] (difficulty 3)** In a directed graph a neighbour that is already fully processed (done) means
   A) the end of the search  <- reveals: Treats any previously visited neighbour in a directed graph as a cycle
   B) an invalid graph  <- reveals: Treats any previously visited neighbour in a directed graph as a cycle
   C) not necessarily a cycle~m3  <- CORRECT
   D) always a cycle  <- reveals: Treats any previously visited neighbour in a directed graph as a cycle

**25. [dsa.bfs.q2] (difficulty 3)** BFS time complexity with an adjacency list is
   A) O(V^2)  <- reveals: Uses BFS for shortest paths on weighted graphs
   B) O(V log V)  <- reveals: Uses BFS for shortest paths on weighted graphs
   C) O(E^2)  <- reveals: Uses BFS for shortest paths on weighted graphs
   D) O(V + E)  <- CORRECT

**26. [dsa.bit.q4] (difficulty 3)** A single non-duplicated number in an array where every other number appears twice can be found by
   A) sum of all elements  <- reveals: Believes XOR of a number with itself is the number
   B) XOR of all elements  <- CORRECT
   C) AND of all elements  <- reveals: Believes XOR of a number with itself is the number
   D) sorting and summing  <- reveals: Believes XOR of a number with itself is the number

**27. [dsa.shortest.q5] (difficulty 4)** "Relaxing" edge (u, v) with weight w means
   A) if dist[u] + w < dist[v], update dist[v]  <- CORRECT
   B) always setting dist[v] = w  <- reveals: Believes Dijkstra with a binary heap is O(V^2) always or ignores the log factor
   C) removing the edge  <- reveals: Believes Dijkstra with a binary heap is O(V^2) always or ignores the log factor
   D) sorting the neighbours  <- reveals: Believes Dijkstra with a binary heap is O(V^2) always or ignores the log factor

**28. [dsa.backtracking.q4] (difficulty 3)** Adding the current path list directly to the results without copying causes
   A) later changes to the path alter the stored results  <- CORRECT
   B) the path becoming empty immediately  <- reveals: Stores a reference to the path list without copying it into results
   C) faster code with no side effects  <- reveals: Stores a reference to the path list without copying it into results
   D) a compile error  <- reveals: Stores a reference to the path list without copying it into results

**29. [dsa.sorting_basic.q2] (difficulty 2)** Best-case time of insertion sort on an already sorted array is
   A) O(n log n)  <- reveals: Believes insertion sort is always O(n^2) even on sorted input
   B) O(n)  <- CORRECT
   C) O(n^2)  <- reveals: Believes insertion sort is always O(n^2) even on sorted input
   D) O(log n)  <- reveals: Believes insertion sort is always O(n^2) even on sorted input

**30. [dsa.trees.q3] (difficulty 2)** Postorder traversal is useful for
   A) reading a BST in sorted order  <- reveals: Mixes up preorder, inorder and postorder
   B) copying the tree top-down only  <- reveals: Mixes up preorder, inorder and postorder
   C) level-by-level printing  <- reveals: Uses a stack for level-order traversal instead of a queue
   D) processing children before the parent, such as deleting a tree  <- CORRECT

**31. [dsa.monotonic_stack.q5] (difficulty 3)** Why store indices on the stack rather than values for daily temperatures?
   A) Because values may be negative  <- reveals: Believes the nested while loop makes the algorithm O(n^2)
   B) To save memory only  <- reveals: Believes the nested while loop makes the algorithm O(n^2)
   C) Stacks cannot store values  <- reveals: Believes the nested while loop makes the algorithm O(n^2)
   D) To compute the distance in days  <- CORRECT

**32. [dsa.two_pointers.q3] (difficulty 2)** The opposite-end two-pointer pair-sum logic is valid only if the array is
   A) all positive  <- reveals: Applies opposite-end pair-sum two pointers to an unsorted array without sorting
   B) made of distinct values only  <- reveals: Applies opposite-end pair-sum two pointers to an unsorted array without sorting
   C) sorted  <- CORRECT
   D) of even length  <- reveals: Applies opposite-end pair-sum two pointers to an unsorted array without sorting

**33. [dsa.tree_problems.q2] (difficulty 3)** Height of a node (in nodes) equals
   A) min(height(left), height(right))  <- reveals: Computes the diameter only through the root
   B) 1 + max(height(left), height(right))  <- CORRECT
   C) 1 + height(left) only  <- reveals: Computes the diameter only through the root
   D) height(left) + height(right)  <- reveals: Computes the diameter only through the root

**34. [dsa.complexity.q2] (difficulty 1)** An algorithm does 3n + 10 basic operations. Which Big-O describes it?
   A) O(n)  <- CORRECT
   B) O(3n)  <- reveals: Thinks O(2n) or O(n+5) is a different, larger class than O(n)
   C) O(n + 10)  <- reveals: Thinks O(2n) or O(n+5) is a different, larger class than O(n)
   D) O(n^2)  <- reveals: Thinks nested loops are always O(n^2) even when the inner loop is bounded by a constant or halves

**35. [dsa.strings.q2] (difficulty 1)** In Java, s.equals(t) tests
   A) whether the two strings have the same characters  <- CORRECT
   B) only the first character  <- reveals: Believes comparing strings with == always compares content
   C) only the length of the strings  <- reveals: Believes comparing strings with == always compares content
   D) whether both variables point to the same object  <- reveals: Believes comparing strings with == always compares content

**36. [dsa.sorting_adv.q4] (difficulty 3)** Quicksort average-case time is
   A) O(n^2)  <- reveals: Believes quicksort is always O(n log n)
   B) O(n)  <- reveals: Believes a comparison sort can run in O(n) in general
   C) O(2^n)  <- reveals: Believes quicksort is always O(n log n)
   D) O(n log n)  <- CORRECT

**37. [dsa.stack.q4] (difficulty 2)** For "((" after scanning all characters a correct bracket checker should
   A) report not balanced because the stack is not empty  <- CORRECT
   B) report balanced because there is no mismatch  <- reveals: Forgets to check for an empty stack, or forgets the stack must be empty at the end for balanced brackets
   C) crash on pop  <- reveals: Forgets to check for an empty stack, or forgets the stack must be empty at the end for balanced brackets
   D) ignore it  <- reveals: Forgets to check for an empty stack, or forgets the stack must be empty at the end for balanced brackets

**38. [dsa.binary_search.q2] (difficulty 2)** Which precondition is required for binary search on an array?
   A) The array has distinct values  <- reveals: Applies binary search to unsorted data
   B) The array is sorted  <- CORRECT
   C) The array has even length  <- reveals: Applies binary search to unsorted data
   D) The array is stored in a hash table  <- reveals: Applies binary search to unsorted data

**39. [dsa.graph_repr.q4] (difficulty 2)** Checking whether an edge (u, v) exists is O(1) with
   A) an adjacency matrix  <- CORRECT
   B) an edge list always  <- reveals: Thinks adjacency lists check edge existence in O(1)
   C) a linked list of vertices  <- reveals: Thinks adjacency lists check edge existence in O(1)
   D) an adjacency list always  <- reveals: Thinks adjacency lists check edge existence in O(1)

**40. [dsa.recursion.q1] (difficulty 2)** What are the two essential parts of a correct recursive function?
   A) A loop and a counter  <- reveals: Forgets that recursion needs a base case and a step toward it
   B) Two return statements  <- reveals: Forgets that recursion needs a base case and a step toward it
   C) A global variable and a stack  <- reveals: Forgets that recursion needs a base case and a step toward it
   D) A base case and a recursive step toward it  <- CORRECT

## Skill reference notes to review (same seed, 8 skills)

### dsa.mst - Minimum spanning trees
Prerequisites: dsa.union_find, dsa.shortest; difficulty 4; 4.0 h

A spanning tree of a connected undirected graph with V vertices uses exactly V minus one edges, connects every vertex and has no cycle; a minimum spanning tree has the smallest possible total weight. Kruskal's algorithm sorts all edges by weight and adds each edge if it connects two different components, checked with Union-Find, in O(E log E). Prim's algorithm grows a tree from one vertex, repeatedly adding the cheapest edge leaving the tree using a min-heap, in O((V + E) log V). Both rely on the cut property: the lightest edge crossing any cut belongs to some MST. If edge weights are distinct the MST is unique. An MST minimises total weight, not the path between two given vertices, so it is not the same as a shortest-path tree. A disconnected graph has a minimum spanning forest instead.

### dsa.arrays - Arrays and indexing
Prerequisites: dsa.complexity; difficulty 1; 3.0 h

An array stores elements in contiguous memory, which gives O(1) random access by index and excellent cache behaviour. Inserting or deleting in the middle costs O(n) because later elements must shift; appending to a dynamic array is amortised O(1) because capacity doubles occasionally. Indices run from 0 to n-1, and off-by-one mistakes at the boundaries are the most common bug, so check the first and last element and the empty array. Learn to traverse, reverse in place, rotate, find max and min, and remove duplicates from a sorted array. In-place algorithms use O(1) extra space by swapping or overwriting. Know the difference between array length and the last valid index. Many interview problems reduce to a clean single pass over an array, so practise tracking a running value like a maximum or a count.

### dsa.trie - Tries
Prerequisites: dsa.trees, dsa.hashing, dsa.strings; difficulty 3; 3.0 h

A trie, or prefix tree, stores strings character by character along root-to-node paths, with a flag on nodes where a word ends. Insert, search and prefix lookup each take O(L) time where L is the length of the word, independent of how many words are stored. Each node holds children in an array of 26 or in a hash map, trading memory for speed. Tries excel at autocomplete, prefix counting, dictionary word search and longest common prefix. A hash set can test whole-word membership in average O(L) too, but it cannot answer prefix queries without scanning. The main cost is memory, because many nodes can be created for sparse data. Be careful to distinguish a prefix from a complete word using the end-of-word flag, so searching for app does not succeed merely because apple was inserted.

### dsa.bst - Binary search trees
Prerequisites: dsa.trees, dsa.binary_search; difficulty 3; 4.0 h

In a binary search tree, every node's left subtree holds values smaller than the node and its right subtree holds larger values, applied recursively to the whole subtree, not just to the immediate children. This makes search, insert and delete O(h) where h is the height: O(log n) when balanced but O(n) for a degenerate tree built from sorted input. An inorder traversal of a valid BST gives the values in sorted order, which is the easiest way to validate one. Validate with min and max bounds passed down the recursion, because checking only each node against its direct children misses violations deeper down. Deleting a node with two children replaces it with its inorder successor, the smallest node in the right subtree. Self-balancing trees such as AVL and red-black trees keep the height at O(log n) with rotations.

### dsa.sorting_adv - Merge sort and quicksort
Prerequisites: dsa.sorting_basic, dsa.recursion; difficulty 3; 5.0 h

Merge sort splits the array in half, sorts each half recursively and merges the two sorted halves in linear time, giving O(n log n) in every case with O(n) extra space and a stable result. Quicksort picks a pivot, partitions the array so smaller values are left and larger values are right, and recurses on both sides; it is O(n log n) on average with O(log n) stack space but O(n^2) in the worst case, for example an already sorted array with the first element as pivot. Random or median-of-three pivots make the worst case unlikely. Quicksort is usually faster in practice because of cache behaviour, while merge sort suits linked lists and external sorting. Know that comparison sorting needs Omega(n log n) comparisons, and that counting sort breaks this for small integer ranges.

### dsa.graph_repr - Graph representations
Prerequisites: dsa.hashing, dsa.arrays; difficulty 2; 3.0 h

A graph is a set of vertices and edges, which may be directed or undirected and weighted or unweighted. The adjacency list stores for each vertex a list of its neighbours and takes O(V + E) space, which suits sparse graphs and makes iterating neighbours efficient. The adjacency matrix is a V by V grid taking O(V^2) space, but it answers whether an edge exists in O(1), which suits dense graphs. An undirected edge must be stored in both directions. Degree counts edges at a vertex; in a directed graph there are in-degree and out-degree. A graph with V vertices can have at most V(V-1)/2 undirected edges. Many interview inputs are implicit graphs, such as grids where neighbours are up, down, left and right, or words connected by one-letter changes. Decide the representation before writing traversal code.

### dsa.dp_1d - One-dimensional DP
Prerequisites: dsa.dp_intro; difficulty 3; 5.0 h

One-dimensional dynamic programming uses a single array where dp[i] depends on earlier entries. Classic problems include climbing stairs, where dp[i] equals dp[i-1] plus dp[i-2]; house robber, where dp[i] is the larger of skipping house i or taking it plus dp[i-2]; coin change for the minimum number of coins, where dp[a] equals one plus the minimum of dp[a - coin] over all coins; and longest increasing subsequence in O(n^2) or O(n log n) with binary search. State the meaning of dp[i] in one precise sentence, for example the best value using the first i items, then derive the recurrence. Initialise unreachable states with infinity and check for them before adding one. Often only the last few values are needed, reducing space to O(1). Compare with greedy to see when a greedy choice fails, such as coin change with unusual denominations.

### dsa.prefix_sums - Prefix sums
Prerequisites: dsa.arrays; difficulty 2; 3.0 h

A prefix sum array stores in prefix[i] the sum of the first i elements, so the sum of any range from l to r (inclusive) is prefix[r+1] - prefix[l] in O(1) after O(n) preprocessing. This turns repeated range-sum queries from O(n) each into O(1) each. Use an extra leading zero so empty ranges and ranges starting at index 0 need no special case. The idea extends to 2D grids with a rectangle formula, to counting subarrays with a given sum using a hash map of earlier prefix values, and to difference arrays that apply many range updates and recover the final array with one prefix pass. Beware of integer overflow with large sums and off-by-one errors in the indices. Prefix sums only work well when the array is static or updates are rare.
