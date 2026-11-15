import java.util.*;

public class Tests {
    private static int passed = 0, failed = 0;

    private static Set<String> toSet(List<int[]> matches) {
        Set<String> s = new TreeSet<>();
        for (int[] m : matches) s.add(m[0] + ":" + m[1]);
        return s;
    }

    private static List<int[]> brute(String text, String[] patterns) {
        List<int[]> res = new ArrayList<>();
        for (int p = 0; p < patterns.length; p++)
            for (int i = 0; i + patterns[p].length() <= text.length(); i++)
                if (text.startsWith(patterns[p], i)) res.add(new int[]{i, p});
        return res;
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else failed++;
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
    }

    private static void checkMatches(String name, String text, String[] patterns) {
        List<int[]> ac = new AhoCorasick(patterns).findAll(text);
        List<int[]> expected = brute(text, patterns);
        check(name, ac.size() == expected.size() && toSet(ac).equals(toSet(expected)));
    }

    public static void main(String[] args) {
        // Classic example: "ushers" contains she@1, he@2, hers@2
        String[] classic = {"he", "she", "his", "hers"};
        List<int[]> r = new AhoCorasick(classic).findAll("ushers");
        check("classic example finds 3 matches", r.size() == 3);
        check("classic example positions", toSet(r).equals(new TreeSet<>(Arrays.asList("1:1", "2:0", "2:3"))));

        checkMatches("pattern is suffix of another", "xxabcdxx".replace('x', 'z'), new String[]{"abcd", "bcd", "cd", "d"});
        checkMatches("overlapping matches", "aaaaaa", new String[]{"aa", "aaa"});
        checkMatches("duplicate patterns", "abcabc", new String[]{"abc", "abc", "bc"});
        checkMatches("no matches", "aaaa", new String[]{"b", "bc"});
        checkMatches("pattern longer than text", "ab", new String[]{"abcdef"});
        checkMatches("text with non-alphabet characters", "ab cab,abc", new String[]{"ab", "abc", "cab"});

        Random rnd = new Random(1);
        boolean allOk = true;
        for (int t = 0; t < 300; t++) {
            int alpha = 2 + rnd.nextInt(3);
            String[] pats = new String[1 + rnd.nextInt(15)];
            for (int i = 0; i < pats.length; i++) pats[i] = randomString(rnd, 1 + rnd.nextInt(5), alpha);
            String text = randomString(rnd, rnd.nextInt(200), alpha);
            List<int[]> ac = new AhoCorasick(pats).findAll(text);
            List<int[]> kmp = KMP.findAllSeparately(text, pats);
            List<int[]> bf = brute(text, pats);
            if (!(toSet(ac).equals(toSet(bf)) && ac.size() == bf.size() && toSet(kmp).equals(toSet(bf)) && kmp.size() == bf.size())) {
                allOk = false;
                break;
            }
        }
        check("300 random cases: Aho-Corasick == KMP == brute force", allOk);

        boolean threw = false;
        try { new AhoCorasick(new String[]{""}); } catch (IllegalArgumentException e) { threw = true; }
        check("empty pattern rejected", threw);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static String randomString(Random rnd, int len, int alpha) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append((char) ('a' + rnd.nextInt(alpha)));
        return sb.toString();
    }
}
