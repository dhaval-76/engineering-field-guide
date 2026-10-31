/**
 * Wording clues → pattern, shown on "Approaching a problem" and at the top of the DSA cheat sheet.
 * Text cells are inline Markdown.
 */
export interface PatternClue {
  clue: string;
  pattern: string;
  topic: string;
  page: string;
}

export const patternClues: PatternClue[] = [
  {
    clue: '"have we seen", duplicates, anagrams, frequency, "pair with sum" (unsorted)',
    pattern: 'hash map or set',
    topic: 'dsa/arrays-and-hashing',
    page: 'Arrays and hashing',
  },
  {
    clue: 'subarray **sum** equals k, range sums, many sum queries',
    pattern: 'prefix sums (+ a hash map)',
    topic: 'dsa/arrays-and-hashing',
    page: 'Arrays and hashing',
  },
  {
    clue: '**sorted** array, pair or triple meeting a target, palindrome, in place',
    pattern: 'two pointers',
    topic: 'dsa/two-pointers',
    page: 'Two pointers',
  },
  {
    clue: '**contiguous** subarray or substring, longest/shortest with a condition',
    pattern: 'sliding window',
    topic: 'dsa/sliding-window',
    page: 'Sliding window',
  },
  {
    clue: 'sorted, "minimum value such that…", "maximise the minimum"',
    pattern: 'binary search (on the answer)',
    topic: 'dsa/binary-search',
    page: 'Binary search',
  },
  {
    clue: 'next greater/smaller, how many days until, spans, histograms',
    pattern: 'monotonic stack',
    topic: 'dsa/stacks-and-monotonic-stacks',
    page: 'Stacks',
  },
  {
    clue: 'brackets, nested structure, undo, evaluate an expression',
    pattern: 'stack',
    topic: 'dsa/stacks-and-monotonic-stacks',
    page: 'Stacks',
  },
  {
    clue: 'reverse, reorder, cycle, middle, merge lists',
    pattern: 'fast/slow pointers, dummy head',
    topic: 'dsa/linked-lists',
    page: 'Linked lists',
  },
  {
    clue: 'k largest/smallest/most frequent, merge k sorted, median of a stream',
    pattern: 'heap',
    topic: 'dsa/heaps-and-top-k',
    page: 'Heaps and top-k',
  },
  {
    clue: 'tree depth, path, ancestor, validate BST',
    pattern: 'DFS recursion returning a value',
    topic: 'dsa/trees-and-bst',
    page: 'Trees and BST',
  },
  {
    clue: 'level by level, minimum moves or steps, nearest',
    pattern: 'BFS',
    topic: 'dsa/graphs-bfs-dfs',
    page: 'Graphs: BFS and DFS',
  },
  {
    clue: 'prerequisites, build order, "is there a valid order"',
    pattern: 'topological sort',
    topic: 'dsa/topological-sort',
    page: 'Topological sort',
  },
  {
    clue: 'cheapest/fastest route with weights',
    pattern: 'Dijkstra or Bellman-Ford',
    topic: 'dsa/shortest-paths',
    page: 'Shortest paths',
  },
  {
    clue: 'connected groups, merge accounts, redundant edge, connections added over time',
    pattern: 'union-find',
    topic: 'dsa/union-find',
    page: 'Union-find',
  },
  {
    clue: 'meetings, bookings, overlapping ranges',
    pattern: 'sort by start (or end), then sweep',
    topic: 'dsa/intervals',
    page: 'Intervals',
  },
  {
    clue: '"all" combinations, permutations, subsets, arrangements',
    pattern: 'backtracking',
    topic: 'dsa/backtracking',
    page: 'Backtracking',
  },
  {
    clue: 'a locally best choice that never needs undoing',
    pattern: 'greedy (prove it with an exchange argument)',
    topic: 'dsa/greedy',
    page: 'Greedy',
  },
  {
    clue: 'number of ways, min/max cost, choices whose subproblems repeat',
    pattern: 'dynamic programming',
    topic: 'dsa/dynamic-programming',
    page: 'Dynamic programming',
  },
  {
    clue: 'XOR, "appears once", powers of two, small sets as masks',
    pattern: 'bit manipulation',
    topic: 'dsa/bit-manipulation',
    page: 'Bit manipulation',
  },
  {
    clue: '"design a structure with O(1) get/put/…", prefixes of words',
    pattern: 'combine structures (map + list, trie)',
    topic: 'dsa/design-data-structures',
    page: 'Designing data structures',
  },
];
