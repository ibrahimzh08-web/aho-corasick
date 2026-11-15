# Aho–Corasick vs KMP (multi-pattern string search)

Java implementation of the Aho–Corasick algorithm, with tests and an experiment that compares it to running KMP separately for each pattern.

## Project structure

```
src/AhoCorasick.java   trie + failure links + output links + search
src/KMP.java           KMP for one pattern, and a helper that runs it once per pattern
src/Tests.java         correctness tests (no external libraries)
src/Benchmark.java     experiments, writes results.csv
results.csv            raw results of the experiment
```

## How to run

```
mkdir out
javac -d out src/*.java
java -cp out Tests
java -cp out Benchmark
```

Requires Java 17 or newer. The alphabet is lowercase letters a–z; characters outside a–z in the text reset the search to the root.

## Implementation summary

1. **Trie construction.** Every pattern is inserted into a trie (`next[node][letter]`). The node where a pattern ends stores the pattern id. Duplicate patterns are supported (ids are chained in `outNext`).
2. **Failure links.** A BFS over the trie computes `fail[v]` for each node `v`: the node of the longest proper suffix of `v`'s string that is also a prefix of some pattern. For a child `v` of `u` reached by letter `c`, `fail[v] = go(fail[u], c)`. Missing edges are filled with `next[u][c] = next[fail[u]][c]`, so the search never has to loop on failure links.
3. **Output links.** `dictLink[v]` points to the nearest node on the failure chain of `v` that ends a pattern. When the search stands on a node, it follows `dictLink` to report every pattern that ends at the current position (for example, `she` also reports `he`), without checking nodes that end no pattern.
4. **Search.** One left-to-right pass over the text. For each character the automaton moves to the next state, then all patterns ending at that position are reported as `{startIndex, patternIndex}`.

## Experiment setup

- Patterns: 10, 100, 1,000 random strings of length 3–8 over a–z.
- Texts: 1,000, 10,000, 100,000 random characters over a–z.
- Fixed random seed (42), so the data is reproducible.
- Time measured with `System.nanoTime()`. Each case runs 5 times and the table shows the average.
- One warm-up phase before measuring, so JIT compilation does not distort the first cases.
- "AC total" = trie + failure links construction + search. "KMP total" = prefix function for each pattern + search for each pattern.
- In every case the number of matches found by Aho–Corasick equals the number found by KMP (the benchmark throws an error otherwise). `Tests.java` also compares both against brute force on 300 random cases.

## Results

| Patterns | Text length | Matches | AC build (ms) | AC search (ms) | AC total (ms) | KMP total (ms) | KMP / AC |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 1,000 | 0 | 0.077 | 0.004 | 0.082 | 0.048 | 0.6x |
| 10 | 10,000 | 2 | 0.029 | 0.042 | 0.071 | 0.445 | 6.3x |
| 10 | 100,000 | 13 | 0.040 | 0.415 | 0.455 | 4.359 | 9.6x |
| 100 | 1,000 | 0 | 0.237 | 0.005 | 0.242 | 0.459 | 1.9x |
| 100 | 10,000 | 5 | 0.306 | 0.091 | 0.397 | 4.714 | 11.9x |
| 100 | 100,000 | 113 | 0.251 | 0.474 | 0.725 | 47.827 | 66.0x |
| 1000 | 1,000 | 6 | 1.364 | 0.016 | 1.379 | 4.734 | 3.4x |
| 1000 | 10,000 | 101 | 0.943 | 0.123 | 1.067 | 42.386 | 39.7x |
| 1000 | 100,000 | 877 | 0.728 | 1.104 | 1.831 | 357.848 | 195.4x |
## Complexity analysis

Notation: `n` = text length, `k` = number of patterns, `M` = total length of all patterns, `σ` = alphabet size (26), `z` = number of reported matches.

**Aho–Corasick**

- Trie construction: `O(M)`.
- Failure and output links: `O(M·σ)` in this implementation, because the BFS also fills the full transition table. With only failure links (no full table) it is `O(M)` amortized.
- Search: `O(n + z)`. Each character causes exactly one transition (the goto table has no failure loops), and each reported match costs `O(1)` through output links.
- Total time: `O(M·σ + n + z)`.
- Space: `O(M·σ)` for the transition table (at most `M + 1` nodes, each with `σ` entries), plus `O(M)` for failure links, output links, and pattern ids. The experiment shows this clearly: 1,000 patterns gave about 4,000 trie nodes.

**KMP run separately for each pattern**

- Prefix function for pattern `i`: `O(m_i)`, searching: `O(n + z_i)`.
- Total time: `O(M + k·n + z)`. The text is scanned `k` times.
- Space: `O(max m_i)` extra at a time (one prefix table), which is much smaller than the automaton.

## Roles of the trie and failure links

The **trie** merges all patterns that share a prefix into one structure, so the text can be matched against all patterns at the same time instead of one by one. A node represents "the longest part of the text read so far that is a prefix of some pattern".

The **failure links** make the search never go backwards in the text. When the next letter does not continue the current prefix, the algorithm jumps to the longest suffix of that prefix that is still a prefix of some pattern, exactly like the prefix function in KMP, but for the whole trie. Because the text pointer only moves forward, the search is linear. **Output links** make sure patterns that are suffixes of other patterns (like `he` inside `she`) are not missed.

This makes Aho–Corasick suitable for many patterns: the cost of reading the text does not depend on the number of patterns. It is used for things like virus signature scanning, keyword filtering, and intrusion detection.

## Discussion of results

- **Search time grows with the text, not with the number of patterns.** For a 100,000-character text, Aho–Corasick search took 0.415 ms with 10 patterns, 0.474 ms with 100 patterns, and 1.104 ms with 1,000 patterns. It grows only about 2.7x while the number of patterns grows 100x. The small growth comes from a bigger automaton (worse cache use) and more matches to report.
- **KMP time grows roughly linearly with the number of patterns.** For 100,000 characters it went from 4.359 ms (10 patterns) to 47.827 ms (100) to 357.848 ms (1,000), because the text is scanned once per pattern. The speed-up of Aho–Corasick at this text size grew from 9.6x to 66.0x to 195.4x.
- **Only the smallest case favors KMP.** With 10 patterns and a 1,000-character text, KMP was faster (0.048 ms vs 0.082 ms), because building the automaton costs more than the search. With 100 patterns Aho–Corasick already wins even on 1,000 characters (1.9x). These timings are fractions of a millisecond, so JIT compilation and timer resolution affect them strongly.
- **Build cost grows with the total pattern length.** Build time was about 0.03–0.08 ms for 10 patterns, 0.24–0.31 ms for 100 patterns, and 0.73–1.36 ms for 1,000 patterns. Rows with the same number of patterns differ a bit, because each row uses different random patterns and the JVM optimizes the code while running.
- **The build is a one-time cost.** It makes sense when the same pattern set is used on long texts or on many texts.
- The patterns and texts are random over 26 letters, so matches are rare. With patterns that match often, the `z` term grows and output-link walking costs more, but the search is still `O(n + z)`.
## Advantages and limitations

| | Aho–Corasick | KMP once per pattern |
|---|---|---|
| Text passes | 1 | `k` |
| Search time | `O(n + z)`, independent of `k` | `O(k·n + z)` |
| Preprocessing | `O(M·σ)`, one shared automaton | `O(M)` total, small tables |
| Memory | `O(M·σ)`, can be large for big alphabets or many long patterns | `O(max m_i)` |
| Best for | many patterns, long texts, a fixed pattern set reused many times | one or very few patterns, short texts, tight memory |
| Complexity of code | higher (trie, BFS, output links) | simple |
| Adding a pattern later | needs a rebuild | trivial |

**Limitations of Aho–Corasick:** the transition table uses a lot of memory when the alphabet is large (Unicode would need hash maps or sorted edges instead of arrays), the automaton must be rebuilt when patterns change, and for one or two patterns it is more work than needed. **Limitations of KMP-per-pattern:** it rescans the whole text for each pattern, so its time grows linearly with the number of patterns.
