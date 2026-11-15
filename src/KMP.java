import java.util.ArrayList;
import java.util.List;

public class KMP {

    public static int[] prefixFunction(String pattern) {
        int[] pi = new int[pattern.length()];
        int k = 0;
        for (int i = 1; i < pattern.length(); i++) {
            while (k > 0 && pattern.charAt(i) != pattern.charAt(k)) k = pi[k - 1];
            if (pattern.charAt(i) == pattern.charAt(k)) k++;
            pi[i] = k;
        }
        return pi;
    }

    public static List<Integer> search(String text, String pattern, int[] pi) {
        List<Integer> res = new ArrayList<>();
        int k = 0;
        for (int i = 0; i < text.length(); i++) {
            while (k > 0 && text.charAt(i) != pattern.charAt(k)) k = pi[k - 1];
            if (text.charAt(i) == pattern.charAt(k)) k++;
            if (k == pattern.length()) {
                res.add(i - k + 1);
                k = pi[k - 1];
            }
        }
        return res;
    }

    public static List<int[]> findAllSeparately(String text, String[] patterns) {
        List<int[]> res = new ArrayList<>();
        for (int p = 0; p < patterns.length; p++) {
            int[] pi = prefixFunction(patterns[p]);
            for (int start : search(text, patterns[p], pi)) {
                res.add(new int[]{start, p});
            }
        }
        return res;
    }
}
