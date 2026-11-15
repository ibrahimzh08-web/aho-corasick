import java.util.ArrayList;
import java.util.List;


public class AhoCorasick {
    private static final int ALPHABET = 26;

    private final String[] patterns;
    private final int[][] next;
    private final int[] fail;
    private final int[] dictLink;
    private final int[] outHead;
    private final int[] outNext;
    private int nodeCount = 1;

    public AhoCorasick(String[] patterns) {
        this.patterns = patterns.clone();
        int maxNodes = 1;
        for (String p : patterns) {
            if (p == null || p.isEmpty()) {
                throw new IllegalArgumentException("Patterns must be non-empty");
            }
            maxNodes += p.length();
        }
        next = new int[maxNodes][ALPHABET];
        for (int[] row : next) java.util.Arrays.fill(row, -1);
        fail = new int[maxNodes];
        dictLink = new int[maxNodes];
        outHead = new int[maxNodes];
        java.util.Arrays.fill(outHead, -1);
        outNext = new int[patterns.length];

        buildTrie();
        buildFailureLinks();
    }


    private void buildTrie() {
        for (int id = 0; id < patterns.length; id++) {
            int node = 0;
            for (int i = 0; i < patterns[id].length(); i++) {
                int c = patterns[id].charAt(i) - 'a';
                if (c < 0 || c >= ALPHABET) {
                    throw new IllegalArgumentException("Only a-z allowed in patterns: " + patterns[id]);
                }
                if (next[node][c] == -1) {
                    next[node][c] = nodeCount++;
                }
                node = next[node][c];
            }
            outNext[id] = outHead[node];
            outHead[node] = id;
        }
    }


    private void buildFailureLinks() {
        int[] queue = new int[nodeCount];
        int head = 0, tail = 0;

        for (int c = 0; c < ALPHABET; c++) {
            int v = next[0][c];
            if (v == -1) {
                next[0][c] = 0;
            } else {
                fail[v] = 0;
                dictLink[v] = 0;
                queue[tail++] = v;
            }
        }

        while (head < tail) {
            int u = queue[head++];
            for (int c = 0; c < ALPHABET; c++) {
                int v = next[u][c];
                if (v == -1) {
                    next[u][c] = next[fail[u]][c];
                } else {
                    int f = next[fail[u]][c];
                    fail[v] = f;
                    dictLink[v] = (outHead[f] != -1) ? f : dictLink[f];
                    queue[tail++] = v;
                }
            }
        }
    }


    public List<int[]> findAll(String text) {
        List<int[]> result = new ArrayList<>();
        int state = 0;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i) - 'a';
            if (c < 0 || c >= ALPHABET) {
                state = 0;
                continue;
            }
            state = next[state][c];
            int node = (outHead[state] != -1) ? state : dictLink[state];
            while (node != 0) {
                for (int p = outHead[node]; p != -1; p = outNext[p]) {
                    result.add(new int[]{i - patterns[p].length() + 1, p});
                }
                node = dictLink[node];
            }
        }
        return result;
    }

    public int getNodeCount() {
        return nodeCount;
    }
}
