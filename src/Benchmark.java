import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Random;


    private static final int RUNS = 5;
    private static final int[] PATTERN_COUNTS = {10, 100, 1000};
    private static final int[] TEXT_LENGTHS = {1_000, 10_000, 100_000};

    static String randomString(Random rnd, int len) {
        char[] c = new char[len];
        for (int i = 0; i < len; i++) c[i] = (char) ('a' + rnd.nextInt(26));
        return new String(c);
    }

    public static void main(String[] args) throws Exception {
        Random rnd = new Random(42);
        long sink = 0;

        {
            String[] wp = new String[100];
            for (int i = 0; i < wp.length; i++) wp[i] = randomString(rnd, 3 + rnd.nextInt(6));
            String wt = randomString(rnd, 50_000);
            for (int i = 0; i < 20; i++) {
                sink += new AhoCorasick(wp).findAll(wt).size();
                sink += KMP.findAllSeparately(wt, wp).size();
            }
        }

        StringBuilder md = new StringBuilder();
        md.append("| Patterns | Text length | Matches | AC build (ms) | AC search (ms) | AC total (ms) | KMP total (ms) | KMP / AC |\n");
        md.append("|---:|---:|---:|---:|---:|---:|---:|---:|\n");

        try (PrintWriter csv = new PrintWriter("results.csv")) {
            csv.println("patterns,text_length,matches,ac_build_ms,ac_search_ms,ac_total_ms,kmp_total_ms,kmp_over_ac,trie_nodes");

            for (int pc : PATTERN_COUNTS) {
                for (int tl : TEXT_LENGTHS) {
                    String[] patterns = new String[pc];
                    for (int i = 0; i < pc; i++) patterns[i] = randomString(rnd, 3 + rnd.nextInt(6)); // length 3..8
                    String text = randomString(rnd, tl);

                    long buildSum = 0, searchSum = 0, kmpSum = 0;
                    int acMatches = 0, kmpMatches = 0, nodes = 0;

                    for (int run = 0; run < RUNS; run++) {
                        long t0 = System.nanoTime();
                        AhoCorasick ac = new AhoCorasick(patterns);
                        long t1 = System.nanoTime();
                        List<int[]> r1 = ac.findAll(text);
                        long t2 = System.nanoTime();
                        List<int[]> r2 = KMP.findAllSeparately(text, patterns);
                        long t3 = System.nanoTime();

                        buildSum += t1 - t0;
                        searchSum += t2 - t1;
                        kmpSum += t3 - t2;
                        acMatches = r1.size();
                        kmpMatches = r2.size();
                        nodes = ac.getNodeCount();
                        sink += acMatches + kmpMatches;
                    }
                    if (acMatches != kmpMatches) {
                        throw new IllegalStateException("Mismatch: AC=" + acMatches + " KMP=" + kmpMatches);
                    }

                    double build = buildSum / (double) RUNS / 1e6;
                    double search = searchSum / (double) RUNS / 1e6;
                    double total = build + search;
                    double kmp = kmpSum / (double) RUNS / 1e6;

                    csv.printf(Locale.US, "%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.2f,%d%n",
                            pc, tl, acMatches, build, search, total, kmp, kmp / total, nodes);
                    md.append(String.format(Locale.US, "| %d | %,d | %d | %.3f | %.3f | %.3f | %.3f | %.1fx |%n",
                            pc, tl, acMatches, build, search, total, kmp, kmp / total));
                }
            }
        }
        System.out.println(md);
        System.out.println("(checksum " + sink + ")");
    }
